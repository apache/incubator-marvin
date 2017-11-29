package org.marvin.executor.manager

import org.marvin.manager.ArtifactSaver
import org.marvin.model.EngineMetadata
import org.scalatest.{Matchers, WordSpec}

class ArtifactSaverTest extends WordSpec with Matchers {

  val engineMetadata1: EngineMetadata = new EngineMetadata("name",
    "version", "engineType", null, "artifactsRemotePath", "HDFS", "marvin-artifact-bucket", null,
    3000, 3000, 3000, Option(3000), 3000, "testHost")

  val engineMetadata2: EngineMetadata = new EngineMetadata("name",
    "version", "engineType", null, "artifactsRemotePath", "S3", "marvin-artifact-bucket", null,
    3000, 3000, 3000, Option(3000), 3000, "testHost")

  val engineMetadata3: EngineMetadata = new EngineMetadata("name",
    "version", "engineType", null, "artifactsRemotePath", "fs", null, null,
    3000, 3000, 3000, Option(3000), 3000, "testHost")

  val props1 = ArtifactSaver.build(engineMetadata1)

  val props2 = ArtifactSaver.build(engineMetadata2)

  val props3 = ArtifactSaver.build(engineMetadata3)

  "A engineMetadata with artifactsSaverType as HDFS" should {
    "return Props with actorClass ArtifactHdfsSaver" in {
    assert(props1.actorClass().toString == "class org.marvin.manager.ArtifactHdfsSaver")
    }
  }

  "A engineMetadata with artifactsSaverType as S3" should {
    "return Props with actorClass ArtifactS3Saver" in {
      assert(props2.actorClass().toString == "class org.marvin.manager.ArtifactS3Saver")
    }
  }

  "A engineMetadata with artifactsSaverType as FS" should {
    "return Props with actorClass ArtifactFSSaver" in {
      assert(props3.actorClass().toString == "class org.marvin.manager.ArtifactFSSaver")
    }
  }
}
