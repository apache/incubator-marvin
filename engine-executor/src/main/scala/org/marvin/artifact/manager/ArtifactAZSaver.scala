package org.apache.marvin.artifact.manager

import org.apache.marvin.model.EngineMetadata
import java.io.{File, FileInputStream}

import org.apache.hadoop.fs.Path
import org.apache.marvin.artifact.manager.ArtifactSaver.{SaveToLocal, SaveToRemote}
import akka.Done
import akka.actor.{Actor, ActorLogging}
import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.StorageException
import com.microsoft.azure.storage.blob.CloudBlobClient

case class ArtifactAZSaver(metadata: EngineMetadata) extends Actor with ActorLogging {
  var azClient:CloudBlobClient = _

  override def preStart() = {
    log.info(s"${this.getClass().getCanonicalName} actor initialized...")


    //Create Azure Client with default credential informations(Environment Variable)
    val account = CloudStorageAccount.parse(metadata.azConnectionString)
    azClient = account.createCloudBlobClient

    log.info("Azure client initialized...")
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
      azClient.getContainerReference(metadata.azContainerName).exists()
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
        log.info(s"Copying files from ${metadata.azContainerName}: ${uris("remotePath")} to ${uris("localPath")}")

        // Container name must be lower case.
        val azBlobContainer = azClient.getContainerReference(metadata.azContainerName)

        //Get local artifact and save to S3 Bucket with name "uris("remotePath")"
        val blob = azBlobContainer.getBlockBlobReference(uris("remotePath").toString)

        //Get artifact named "uris("remotePath")" from AZURE Blob Container and save it to local
        blob.downloadToFile(localToSave.getAbsolutePath)


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
        log.info(s"Copying files from ${uris("localPath")} to ${metadata.azContainerName}: ${uris("remotePath")}")

        // Container name must be lower case.
        val azBlobContainer = azClient.getContainerReference(metadata.azContainerName)
        azBlobContainer.createIfNotExists()

        //Get local artifact and save to AZURE Blob Container with name "uris("remotePath")"
        val blob = azBlobContainer.getBlockBlobReference(uris("remotePath").toString)

        try {
          val sourceStream = new FileInputStream(fileToUpload)

          try blob.upload(sourceStream, fileToUpload.length)
          finally if (sourceStream != null) sourceStream.close()
        }
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

