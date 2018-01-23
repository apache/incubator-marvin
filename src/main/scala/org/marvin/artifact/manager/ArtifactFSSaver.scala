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
package org.marvin.artifact.manager

import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import akka.Done
import akka.actor.{Actor, ActorLogging}
import org.marvin.artifact.manager.ArtifactSaver.{SaveToLocal, SaveToRemote}
import org.marvin.model.EngineMetadata

class ArtifactFSSaver(metadata: EngineMetadata) extends Actor with ActorLogging {
  override def preStart() = {
    log.info(s"${this.getClass().getCanonicalName} actor initialized...")
  }

  def generatePaths(artifactName: String, protocol: String): Map[String, Path] = {
    Map(
      "localPath" -> Paths.get(s"${metadata.artifactsLocalPath}/${metadata.name}/$artifactName"),
      "remotePath" -> Paths.get((s"${metadata.artifactsRemotePath}/${metadata.name}/${metadata.version}/$artifactName/$protocol"))
    )
  }

  def copyFile(origin: Path, destination: Path): Unit = {
    if (!destination.getParent.toFile.exists()) destination.getParent.toFile.mkdirs()

    log.info(s"Copying files from ${origin} to ${destination}")

    Files.copy(origin, destination, StandardCopyOption.REPLACE_EXISTING)

    log.info(s"File ${destination} saved!")
  }

  override def receive: Receive = {
    case SaveToLocal(artifactName, protocol) =>
      log.info("Receive message and starting to working...")
      val uris = generatePaths(artifactName, protocol)

      copyFile(uris("remotePath"), uris("localPath"))

      sender ! Done

    case SaveToRemote(artifactName, protocol) =>
      log.info("Receive message and starting to working...")
      val uris = generatePaths(artifactName, protocol)

      copyFile(uris("localPath"), uris("remotePath"))

      sender ! Done

    case _ =>
      log.warning("Received a bad format message...")
  }
}
