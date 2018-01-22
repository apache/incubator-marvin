/*
 * Copyright [2017] [B2W Digital]
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
package org.marvin.executor.api

import java.io.FileNotFoundException

import akka.actor.{ActorRef, ActorSystem, Props}
import com.github.fge.jsonschema.core.exceptions.ProcessingException
import com.typesafe.config.{Config, ConfigFactory}
import org.marvin.executor.actions.{BatchAction, OnlineAction, PipelineAction}
import org.marvin.executor.manager.ExecutorManager
import org.marvin.executor.statemachine.{PredictorFSM, Reload}
import org.marvin.model.{EngineMetadata, MarvinEExecutorException}
import org.marvin.util.{ConfigurationContext, JsonUtil}

import scala.io.Source
import scala.reflect.ClassTag
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

  def setupConfig(): Config = {
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
    readJsonIfFileExists[EngineMetadata](vmParams("metadataFilePath").asInstanceOf[String], true)
  }

  def getEngineParameters(): String = {
    JsonUtil.toJson(readJsonIfFileExists[Map[String, String]](vmParams("paramsFilePath").asInstanceOf[String]))
  }

  def getVMParameters(): Map[String, Any] = {
    //Get all VM options
    val parameters = Map[String, Any](
      "metadataFilePath" -> s"${ConfigurationContext.getStringConfigOrDefault("engineHome", ".")}/engine.metadata",
      "paramsFilePath" -> s"${ConfigurationContext.getStringConfigOrDefault("engineHome", ".")}/engine.params",
      "ipAddress" -> ConfigurationContext.getStringConfigOrDefault("ipAddress", "localhost"),
      "port" -> ConfigurationContext.getIntConfigOrDefault("port", 8000),
      "protocol" -> ConfigurationContext.getStringConfigOrDefault("protocol", ""),
      "enableAdmin" -> ConfigurationContext.getBooleanConfigOrDefault("enableAdmin", false),
      "adminPort" -> ConfigurationContext.getIntConfigOrDefault("adminPort", 50100),
      "adminHost" -> ConfigurationContext.getStringConfigOrDefault("adminHost", "127.0.0.1")
    )

    parameters
  }

  def readJsonIfFileExists[T: ClassTag](filePath: String, validate: Boolean = false): T = {
    Try(JsonUtil.fromJson[T](Source.fromFile(filePath).mkString, validate)) match {
      case Success(json) => json
      case Failure(ex) => {
        ex match {
          case ex: FileNotFoundException => throw new MarvinEExecutorException(s"The file [$filePath] does not exists." +
            s" Check your engine configuration.", ex)
          case ex: ProcessingException => throw new MarvinEExecutorException(s"Invalid engine metadata file."  +
            s" Check your engine metadata file.", ex)
          case _ => throw ex
        }
      }
    }
  }

  def setupGenericAPI(): GenericAPI = {

    val metadata = getEngineMetadata()
    val params = getEngineParameters()
    val config = setupConfig()

    val system = ActorSystem(metadata.name, config)

    val actors = Map[String, ActorRef](
      "predictor" -> system.actorOf(Props(new PredictorFSM(metadata)), name = "predictorFSM"),
      "acquisitor" -> system.actorOf(Props(new BatchAction("acquisitor", metadata)), name = "acquisitorActor"),
      "tpreparator" -> system.actorOf(Props(new BatchAction("tpreparator", metadata)), name = "tpreparatorActor"),
      "trainer" -> system.actorOf(Props(new BatchAction("trainer", metadata)), name = "trainerActor"),
      "evaluator" -> system.actorOf(Props(new BatchAction("evaluator", metadata)), name = "evaluatorActor"),
      "pipeline" -> system.actorOf(Props(new PipelineAction(metadata)), name = "pipelineActor"),
      "feedback" -> system.actorOf(Props(new OnlineAction("feedback", metadata)), name = "feedbackActor")
    )

    val api = new GenericAPI(system, metadata, params, actors)

    //send model protocol to be reloaded by predictor service
    actors("predictor") ! Reload(vmParams("protocol").asInstanceOf[String])

    api
  }

  def setupAdministration() = {
    if (vmParams("enableAdmin").asInstanceOf[Boolean])
      executorManager = api.getSystem.actorOf(Props(new ExecutorManager(api.getMetadata, api.manageableActors)), name="executorManager")
  }

  def start() = {
    api.startServer(vmParams("ipAddress").asInstanceOf[String], vmParams("port").asInstanceOf[Int])
  }

}
