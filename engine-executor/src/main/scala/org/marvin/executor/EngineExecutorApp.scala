/*
 * Copyright [2019] [Apache Software Foundation]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.apache.marvin.executor

import java.io.FileNotFoundException

import akka.actor.{ActorRef, ActorSystem, Props}
import com.fasterxml.jackson.core.JsonParseException
import com.github.fge.jsonschema.core.exceptions.ProcessingException
import com.typesafe.config.{Config, ConfigFactory}
import grizzled.slf4j.Logger
import org.apache.marvin.exception.MarvinEExecutorException
import org.apache.marvin.executor.actions.{BatchAction, OnlineAction, PipelineAction}
import org.apache.marvin.executor.api.{GenericAPI, GenericAPIFunctions}
import org.apache.marvin.executor.manager.ExecutorManager
import org.apache.marvin.executor.statemachine.{PredictorFSM, Reload}
import org.apache.marvin.model.EngineMetadata
import org.apache.marvin.util.{ConfigurationContext, JsonUtil}

import scala.io.Source
import scala.reflect.ClassTag
import scala.reflect.io.File
import scala.util.{Failure, Success, Try}

object EngineExecutorApp {
  def main(args: Array[String]): Unit = {

    val app = new EngineExecutorApp()

    // setup actors system before start server
    app.setupGenericAPI()

    // if requested enable remote administration
    app.setupAdministration()

    // start generic api server
    app.start()
  }
}

class EngineExecutorApp {

  var vmParams: Map[String, Any] = getVMParameters
  var executorManager: ActorRef = _
  var api: GenericAPIFunctions = _

  lazy val log = Logger[this.type]

  def setupConfig(): Config = {

    log.info("Configuring engine executor app...")

    if (vmParams("enableAdmin").asInstanceOf[Boolean]) {
      val configuration = """
        akka{
          actor {
            provider = remote
          }

          remote.artery {
            enabled = on
            canonical.hostname = "{hostname}"
            canonical.port = {port}
          }
        }
      """
        .replace("{hostname}", vmParams("adminHost").asInstanceOf[String])
        .replace("{port}", vmParams("adminPort").asInstanceOf[Int].toString)

      //set the new configuration
      ConfigFactory.parseString(configuration).withFallback(ConfigFactory.load())

    } else {
      //set the default configuration (from appication.conf file)
      ConfigFactory.load()
    }
  }

  def getEngineMetadata(): EngineMetadata = {
    log.info("Getting metadata file from engine...")

    val filePath = s"${vmParams("engineHome").asInstanceOf[String]}/engine.metadata"
    readJsonIfFileExists[EngineMetadata](filePath, true)
  }

  def getEngineParameters(): String = {
    log.info("Getting default parameters file from engine...")

    val filePath = s"${vmParams("engineHome").asInstanceOf[String]}/engine.params"
    JsonUtil.toJson(readJsonIfFileExists[Map[String, String]](filePath))
  }

  def getSchema(actionName: String, target: String): String = {
    log.info(s"Getting schema file for validate the ${actionName} ${target}...")

    val filePath = s"${vmParams("engineHome").asInstanceOf[String]}/${actionName}-${target}.schema"
    JsonUtil.toJson(readJsonIfFileExists[Map[String, String]](filePath))
  }

  def getDocsFilePath(): String = {
    log.info("Getting default api docs file path from engine...")

    val filePath = s"${vmParams("engineHome").asInstanceOf[String]}/docs.yaml"
    if (! File(filePath).exists) throw new MarvinEExecutorException(s"The file [$filePath] does not exists." + s" Check your engine configuration.")

    filePath
  }

  def getVMParameters(): Map[String, Any] = {

    log.info("Getting vm parameters...")

    //Get all VM options
    val parameters = Map[String, Any](
      "engineHome" -> s"${ConfigurationContext.getStringConfigOrDefault("engineHome", "")}",
      "ipAddress" -> ConfigurationContext.getStringConfigOrDefault("ipAddress", "localhost"),
      "port" -> ConfigurationContext.getIntConfigOrDefault("port", 8000),
      "protocol" -> ConfigurationContext.getStringConfigOrDefault("protocol", ""),
      "enableAdmin" -> ConfigurationContext.getBooleanConfigOrDefault("enableAdmin", false),
      "adminPort" -> ConfigurationContext.getIntConfigOrDefault("adminPort", 50100),
      "adminHost" -> ConfigurationContext.getStringConfigOrDefault("adminHost", "127.0.0.1"),
      "enableValidation" -> ConfigurationContext.getBooleanConfigOrDefault("enableValidation", false)
    )

    parameters
  }

  def readJsonIfFileExists[T: ClassTag](filePath: String, validate: Boolean = false): T = {

    log.info(s"Reading json file from [$filePath]...")

    Try(JsonUtil.fromJson[T](Source.fromFile(filePath).mkString, validate)) match {
      case Success(json) => json
      case Failure(ex) => {
        ex match {
          case ex: FileNotFoundException => throw new MarvinEExecutorException(s"The file [$filePath] does not exists." +
            s" Check your engine configuration.", ex)
          case ex: ProcessingException => throw new MarvinEExecutorException(s"The file [$filePath] is invalid."  +
            s" Check your file!", ex)
          case ex: JsonParseException => throw new MarvinEExecutorException(s"The file [$filePath] is an invalid json file."  +
            s" Check your file syntax!", ex)
          case _ => throw ex
        }
      }
    }
  }

  def setupGenericAPI(): GenericAPIFunctions = {

    log.info("Setting Generic API actor system...")

    val metadata = getEngineMetadata()
    val params = getEngineParameters()
    val config = setupConfig()
    val docsFilePath = getDocsFilePath()

    var schemas: Map[String, String] = null
    if (vmParams("enableValidation").asInstanceOf[Boolean]){
        schemas = Map[String, String](
          "predictor-message" -> getSchema("predictor", "message"),
          "feedback-message" -> getSchema("feedback", "message")
        )
    }

    val system = ActorSystem(metadata.name, config)

    log.info("Initializing all actors in API actor system ...")

    val actors = Map[String, ActorRef](
      "predictor" -> system.actorOf(Props(new PredictorFSM(metadata)), name = "predictorFSM"),
      "acquisitor" -> system.actorOf(Props(new BatchAction("acquisitor", metadata)), name = "acquisitorActor"),
      "tpreparator" -> system.actorOf(Props(new BatchAction("tpreparator", metadata)), name = "tpreparatorActor"),
      "trainer" -> system.actorOf(Props(new BatchAction("trainer", metadata)), name = "trainerActor"),
      "evaluator" -> system.actorOf(Props(new BatchAction("evaluator", metadata)), name = "evaluatorActor"),
      "pipeline" -> system.actorOf(Props(new PipelineAction(metadata)), name = "pipelineActor"),
      "feedback" -> system.actorOf(Props(new OnlineAction("feedback", metadata)), name = "feedbackActor")
    )

    api = new GenericAPI(system, metadata, params, actors, docsFilePath, schemas)

    //send model protocol to be reloaded by predictor service
    actors("predictor") ! Reload(vmParams("protocol").asInstanceOf[String])

    log.info("Generic API actor system setting done!")

    api
  }

  def setupAdministration() = {

    if (vmParams("enableAdmin").asInstanceOf[Boolean]){
      log.info("Enabling remote administration in engine executor actor system...")

      executorManager = api.getSystem.actorOf(Props(new ExecutorManager(api)), name="executorManager")
    }
  }

  def start() = {
    log.info("Starting Generic API ...")
    api.startServer(vmParams("ipAddress").asInstanceOf[String], vmParams("port").asInstanceOf[Int])
  }

}
