package org.apache.marvin.artifact.manager

import java.io.File

import akka.Done
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.GetObjectRequest
import com.typesafe.config.ConfigFactory
import org.apache.hadoop.fs.Path
import org.apache.marvin.artifact.manager.ArtifactSaver.{SaveToLocal, SaveToRemote}
import org.apache.marvin.fixtures.MetadataMock
import org.apache.marvin.model.EngineMetadata
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


class ArtifactS3SaverTest extends TestKit(
  ActorSystem("ArtifactS3SaverTest", ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with MockFactory {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "s3 saver" should {
    "receive SaveToLocal message" in {
      val metadata = MetadataMock.simpleMockedMetadata()
      val _s3Client = mock[AmazonS3]
      val actor = system.actorOf(Props(new ArtifactS3SaverMock(metadata, _s3Client, true)))

      val protocol = "protocol"
      val artifactName = "model"

      (_s3Client.getObject(_ : GetObjectRequest, _ : File)).expects(*, *).once()

      actor ! SaveToLocal(artifactName, protocol)

      expectMsg(Done)
    }

    "receive SaveToRemote message" in {
      val metadata = MetadataMock.simpleMockedMetadata()
      val _s3Client = mock[AmazonS3]
      val actor = system.actorOf(Props(new ArtifactS3SaverMock(metadata, _s3Client, true)))

      val protocol = "protocol"
      val artifactName = "model"

      (_s3Client.putObject(_ : String, _: String, _ : File)).expects(metadata.s3BucketName, *, *).once()

      actor ! SaveToRemote(artifactName, protocol)

      expectMsg(Done)
    }
  }

    "call preStart method wth success" in {
      val metadata = MetadataMock.simpleMockedMetadata()
      try{
        system.actorOf(Props(new ArtifactS3Saver(metadata)))
        assert(true)
      }catch {
        case _: Throwable =>
          assert(false)
      }
    }

  class ArtifactS3SaverMock(metadata: EngineMetadata, _s3Client: AmazonS3, _isRemote: Boolean) extends ArtifactS3Saver(metadata) {
    def _preStart(): Unit = super.preStart()
    override def preStart(): Unit = {
      s3Client = _s3Client
    }

    override def validatePath(path: Path, isRemote: Boolean): Boolean = {
      if (_isRemote) true
      else false
    }
  }
}
