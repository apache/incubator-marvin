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
package org.marvin.manager

import akka.Done
import akka.actor.{Actor, ActorLogging}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, Path}
import org.marvin.manager.ArtifactSaver.{SaveToLocal, SaveToRemote}
import org.marvin.model.EngineMetadata

object ArtifactSaver {
  case class SaveToLocal(artifactName: String, protocol:String)
  case class SaveToRemote(artifactName: String, protocol:String)
}

class ArtifactSaver(metadata: EngineMetadata) extends Actor with ActorLogging {
  var conf: Configuration = _

  override def preStart() = {
    log.info(s"${this.getClass().getCanonicalName} actor initialized...")
    conf = new Configuration()
    conf.set("fs.defaultFS", metadata.hdfsHost)
  }

  def generatePaths(artifactName: String, protocol: String): Map[String, Path] = {
    Map(
      "localPath" -> new Path(s"${metadata.artifactsLocalPath}/${metadata.name}/$artifactName"),
      "remotePath" -> new Path(s"${metadata.artifactsRemotePath}/${metadata.name}/${metadata.version}/$artifactName/$protocol")
    )
  }

  override def receive: Receive = {
    case SaveToLocal(artifactName, protocol) =>
      log.info("Receive message and starting to working...")
      val fs = FileSystem.get(conf)
      val uris = generatePaths(artifactName, protocol)

      log.info(s"Copying files from ${uris("remotePath")} to ${uris("localPath")}")

      fs.copyToLocalFile(false, uris("remotePath"), uris("localPath"), false)
      fs.close()

      log.info(s"File ${uris("localPath")} saved!")

      sender ! Done

    case SaveToRemote(artifactName, protocol) =>
      log.info("Receive message and starting to working...")
      val fs = FileSystem.get(conf)
      val uris = generatePaths(artifactName, protocol)

      log.info(s"Copying files from ${uris("localPath")} to ${uris("remotePath")}")

      fs.copyFromLocalFile(uris("localPath"), uris("remotePath"))
      fs.close()

      log.info(s"File ${uris("localPath")} saved!")

      sender ! Done

    case _ =>
      log.warning("Received a bad format message...")
  }
}
