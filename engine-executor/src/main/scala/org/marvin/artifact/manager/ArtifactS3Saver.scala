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
package org.apache.marvin.artifact.manager

import java.io.File

import akka.Done
import akka.actor.{Actor, ActorLogging}
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import org.apache.hadoop.fs.Path
import org.apache.marvin.artifact.manager.ArtifactSaver.{SaveToLocal, SaveToRemote}
import org.apache.marvin.model.EngineMetadata

class ArtifactS3Saver(metadata: EngineMetadata) extends Actor with ActorLogging {
  var s3Client: AmazonS3 = _

  override def preStart() = {
    log.info(s"${this.getClass().getCanonicalName} actor initialized...")

    //Create S3 Client with default credential informations(Environment Variable)
    s3Client = AmazonS3ClientBuilder.standard.withRegion(System.getenv("AWS_DEFAULT_REGION")).build

    log.info("Amazon S3 client initialized...")
  }

  def generatePaths(artifactName: String, protocol: String): Map[String, Path] = {
    var artifactsRemotePath: String = null
    if(metadata.artifactsRemotePath.startsWith("/")){
      artifactsRemotePath = metadata.artifactsRemotePath.substring(1)
    }
    Map(
      "localPath" -> new Path(s"${metadata.artifactsLocalPath}/${metadata.name}/$artifactName"),
      "remotePath" -> new Path(s"${artifactsRemotePath}/${metadata.name}/${metadata.version}/$artifactName/$protocol")
    )
  }

  def validatePath(path: Path, isRemote: Boolean): Boolean = {
    if (isRemote) {
      s3Client.doesObjectExist(metadata.s3BucketName, path.toString)
    } else {
      new java.io.File(path.toString).exists
    }
  }

  override def receive: Receive = {
    case SaveToLocal(artifactName, protocol) =>
      log.info("Receive message and starting to working...")
      val uris = generatePaths(artifactName, protocol)
      val localToSave = new File(uris("localPath").toString)

      // Validate if the protocol is correct
      if (validatePath(uris("remotePath"), true)) {
        log.info(s"Copying files from ${metadata.s3BucketName}: ${uris("remotePath")} to ${uris("localPath")}")
        //Get artifact named "uris("remotePath")" from S3 Bucket and save it to local
        s3Client.getObject(new GetObjectRequest(metadata.s3BucketName, uris("remotePath").toString), localToSave)
        log.info(s"File ${uris("localPath")} saved!")
      }
      else {
        log.error(s"Invalid protocol: ${protocol}, save process canceled!")
      }

      sender ! Done

    case SaveToRemote(artifactName, protocol) =>
      log.info("Receive message and starting to working...")
      val uris = generatePaths(artifactName, protocol)
      val fileToUpload = new File(uris("localPath").toString)

      // Validate if the protocol is correct
      if (validatePath(uris("localPath"), false)) {
        log.info(s"Copying files from ${uris("localPath")} to ${metadata.s3BucketName}: ${uris("remotePath")}")
        //Get local artifact and save to S3 Bucket with name "uris("remotePath")"
        s3Client.putObject(metadata.s3BucketName, uris("remotePath").toString, fileToUpload)
        log.info(s"File ${uris("localPath")} saved!")
      }
      else {
        log.error(s"Invalid protocol: ${protocol}, save process canceled!")
      }

      sender ! Done

    case _ =>
      log.warning("Received a bad format message...")
  }
}
