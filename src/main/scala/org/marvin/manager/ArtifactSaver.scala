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

import akka.actor.{Actor, ActorLogging}
import org.apache.hadoop.conf.Configuration
import org.marvin.manager.ArtifactSaver.SaverMessage
import org.marvin.model.EngineMetadata
import org.marvin.util.HdfsUtil

object ArtifactSaver {
  case class SaverMessage(actionName:String)
}

class ArtifactSaver(engineMetadata: EngineMetadata) extends Actor with ActorLogging {
  var actionsMap:Map[String, List[String]] = _

  var hdfsConf = new Configuration()
  hdfsConf.set("fs.defaultFS", engineMetadata.hdfsHost)

  override def preStart() = {
    log.info(s"${this.getClass().getCanonicalName} actor initialized...")

    actionsMap = Map[String, List[String]]()

    for(action <- engineMetadata.actions){
      actionsMap += ((action.name) -> action.artifactsToPersist)
    }
  }

  def receive = {
    case SaverMessage(actionName) =>
      log.info("Receive message and starting to working...")

      val artifacts = actionsMap(actionName)

      val uris = HdfsUtil.generateUris(artifacts, engineMetadata.artifactsLocalPath, engineMetadata.artifactsRemotePath, engineMetadata.name, engineMetadata.version)
      for(uri <- uris){
        log.info(s"Sending file ${uri("localUri")} to ${uri("remoteUri")}")
        HdfsUtil.copyFromLocal(hdfsConf, uri("localUri"), uri("remoteUri"))
        log.info(s"File ${uri("remoteUri")} saved!")
      }

      sender ! "Done"
    case _ =>
      log.info("Received a bad format message...")
  }
}
