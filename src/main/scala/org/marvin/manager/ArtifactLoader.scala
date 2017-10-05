/**
  * Copyright [2017] [B2W Digital]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
  */
package org.marvin.manager

import akka.Done
import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import akka.util.Timeout
import org.marvin.executor.actions.BatchAction.BatchReloadMessage
import org.marvin.executor.actions.OnlineAction.OnlineReloadMessage
import org.apache.hadoop.conf.Configuration
import org.marvin.manager.ArtifactLoader.{BatchArtifactLoaderMessage, OnlineArtifactLoaderMessage}
import org.marvin.model.EngineMetadata
import org.marvin.util.HdfsUtil

import scala.collection.mutable.Map
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object ArtifactLoader {
  case class OnlineArtifactLoaderMessage(actionName:String, protocol:String)
  case class BatchArtifactLoaderMessage(actionName:String, protocol:String)
}

class ArtifactLoader(engineMetadata: EngineMetadata) extends Actor with ActorLogging {
  var actionsMap: Map[String, List[String]] = _

  val onlineActor = context.system.actorSelection("/user/onlineActor")
  val batchActor = context.system.actorSelection("/user/batchActor")

  implicit val reloadTimeout = Timeout(engineMetadata.reloadTimeout seconds)

  var hdfsConf = new Configuration()
  hdfsConf.set("fs.defaultFS", engineMetadata.hdfsHost)

  override def preStart() = {
    log.info(s"${this.getClass().getCanonicalName} actor initialized...")
  }

  def copyArtifacts(artifacts: List[String], protocol:String) {
    val uris = HdfsUtil.generateUris(artifacts, engineMetadata.artifactsLocalPath, engineMetadata.artifactsRemotePath, engineMetadata.name, engineMetadata.version, protocol)

    for (uri <- uris) {
      log.info(s"Copy file from ${uri("remoteUri")} to ${uri("localUri")}}")
      HdfsUtil.copyToLocal(hdfsConf, uri("remoteUri"), uri("localUri"))
      log.info(s"File ${uri("remoteUri")} saved in ${uri("localUri")}!")
    }
  }

  def receive = {
    case OnlineArtifactLoaderMessage(actionName, protocol) =>
      log.info("Receive message and starting to working...")

      val artifacts = engineMetadata.actionsMap(actionName).artifactsToLoad

      copyArtifacts(artifacts, protocol)

      val joined_artifacts = artifacts.mkString(",")

      log.info(s"Reloading artifacts [${joined_artifacts}] in ${actionName}")

      val response: Future[String] = (onlineActor ? OnlineReloadMessage(actionName, joined_artifacts, protocol)).mapTo[String]

      log.info(s"Artifacts [${joined_artifacts}] in $actionName [$response]!!")

      sender ! Done

    case BatchArtifactLoaderMessage(actionName, protocol) =>
      log.info("Receive message and starting to working...")

      val artifacts = engineMetadata.actionsMap(actionName).artifactsToLoad

      copyArtifacts(artifacts, protocol)

      val joined_artifacts = artifacts.mkString(",")

      log.info(s"Reloading artifacts [${joined_artifacts}] in ${actionName}")

      val response: Future[String] = (batchActor ? BatchReloadMessage(actionName, joined_artifacts, protocol)).mapTo[String]

      log.info(s"Artifacts [${joined_artifacts}] in $actionName [$response]!!")

      sender ! Done

    case _ =>
      log.info("Received a bad format message...")
  }
}
