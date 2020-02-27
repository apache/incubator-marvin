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

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.apache.marvin.exception.MarvinEExecutorException
import org.apache.marvin.executor.api.GenericAPIFunctions
import org.apache.marvin.fixtures.MetadataMock
import org.scalamock.scalatest.MockFactory
import org.scalatest._


class EngineExecutorAppTest extends
  TestKit(ActorSystem("EngineExecutorAppTest", ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with MockFactory {

  "setupConfig method" should {

    "enabling admin" in {
      val app = new EngineExecutorApp()
      val host = "127.0.0.99"
      val port = 9991

      app.vmParams = Map[String, Any]("enableAdmin" -> true, "adminPort" -> port, "adminHost" -> host)

      val config = app.setupConfig()

      config.getString("akka.actor.provider") shouldEqual "remote"
      config.getString("akka.remote.artery.enabled") shouldEqual "on"
      config.getString("akka.remote.artery.canonical.hostname") shouldEqual host
      config.getInt("akka.remote.artery.canonical.port") shouldEqual port
    }

    "disabling admin" in {
      val app = new EngineExecutorApp()
      app.vmParams = Map[String, Any]("enableAdmin" -> false)

      val config = app.setupConfig()

      config.getString("akka.actor.provider") shouldEqual "local"
      config.getString("akka.remote.artery.enabled") shouldEqual "off"
    }

    "enabling and disabling admin in same session" in {
      val app = new EngineExecutorApp()
      val host = "127.0.0.99"
      val port = 9991

      app.vmParams = Map[String, Any]("enableAdmin" -> true, "adminPort" -> port, "adminHost" -> host)
      val config = app.setupConfig()

      config.getString("akka.actor.provider") shouldEqual "remote"
      config.getString("akka.remote.artery.enabled") shouldEqual "on"
      config.getString("akka.remote.artery.canonical.hostname") shouldEqual host
      config.getInt("akka.remote.artery.canonical.port") shouldEqual port

      app.vmParams = Map[String, Any]("enableAdmin" -> false)
      val config2 = app.setupConfig()

      config2.getString("akka.actor.provider") shouldEqual "local"
      config2.getString("akka.remote.artery.enabled") shouldEqual "off"
    }
  }

  "getEngineMetadata method" should {

    "with valid filePath" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]("engineHome" -> getClass.getResource("/engine_home").getPath())

      val metadata = app.getEngineMetadata()

      metadata.name shouldEqual "teste_engine"
    }

    "with invalid filePath" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]("engineHome" -> "invalid_path")

      val caught = intercept[MarvinEExecutorException] {
        app.getEngineMetadata()
      }

      caught.getMessage() shouldEqual "The file [invalid_path/engine.metadata] does not exists. Check your engine configuration."
    }

    "without a correct vmParameter key" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]()

      val caught = intercept[NoSuchElementException] {
        app.getEngineMetadata()
      }

      caught.getMessage() shouldEqual "key not found: engineHome"
    }
  }

  "getEngineParameters method" should {

    "with valid file" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]("engineHome" -> getClass.getResource("/engine_home").getPath())

      val params = app.getEngineParameters()

      params shouldEqual "{\"PARAM_1\":\"VALUE_OF_PARAM_1\"}"
    }

    "with invalid file" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]("engineHome" -> "invalid_path")

      val caught = intercept[MarvinEExecutorException] {
        app.getEngineParameters()
      }

      caught.getMessage() shouldEqual "The file [invalid_path/engine.params] does not exists. Check your engine configuration."
    }

    "without a correct vmParameter key" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]()

      val caught = intercept[NoSuchElementException] {
        app.getEngineParameters()
      }

      caught.getMessage() shouldEqual "key not found: engineHome"
    }
  }

  "getSchema method" should {

    "with valid file" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]("engineHome" -> getClass.getResource("/engine_home").getPath())

      val schema = app.getSchema("predictor", "message")

      schema shouldEqual """{"title":"PredictMessage","type":"object","properties":{"firstName":{"type":"string"},"lastName":{"type":"string"},"age":{"description":"Age in years","type":"integer","minimum":0}},"required":["firstName","lastName"]}"""    }

    "with invalid file" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]("engineHome" -> "invalid_path")

      val caught = intercept[MarvinEExecutorException] {
        app.getSchema("predictor", "message")
      }

      caught.getMessage() shouldEqual "The file [invalid_path/predictor-message.schema] does not exists. Check your engine configuration."
    }

    "without a correct vmParameter key" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]()

      val caught = intercept[NoSuchElementException] {
        app.getSchema("predictor", "message")
      }

      caught.getMessage() shouldEqual "key not found: engineHome"
    }
  }

  "getVMParameters method" should {

    "with valid filePath" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]("paramsFilePath" -> getClass.getResource("/engine_home/engine.params").getPath())

      val vmParams = app.getVMParameters()

      vmParams shouldEqual Map[String, Any](
        "engineHome" -> "a/fake/path",
        "enableValidation" -> false,
        "ipAddress" -> "1.1.1.1",
        "port" -> 9999,
        "protocol" -> "",
        "enableAdmin" -> false,
        "adminPort" -> 50100,
        "adminHost" -> "127.0.0.1"
      )
    }
  }

  "getDocsFilePath method" should {

    "with valid filePath" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]("engineHome" -> getClass.getResource("/engine_home").getPath())

      val filePath = app.getDocsFilePath()

      filePath shouldEqual s"${app.vmParams("engineHome").asInstanceOf[String].mkString}/docs.yaml"
    }

    "with invalid filePath" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]("engineHome" -> "invalid_path")

      val caught = intercept[MarvinEExecutorException] {
        app.getDocsFilePath()
      }

      caught.getMessage() shouldEqual "The file [invalid_path/docs.yaml] does not exists. Check your engine configuration."
    }
  }

  "start method" should {

    "works successfully" in {
      val app = new EngineExecutorApp()
      val mockApi = mock[GenericAPIFunctions]
      val host = "127.0.0.99"
      val port = 9991

      app.vmParams = Map[String, Any]("port" -> port, "ipAddress" -> host)
      app.api = mockApi

      (mockApi.startServer(_: String, _: Int)).expects(host, port).once()

      app.start()
    }
  }

  "readJsonIfFileExists method" should {

    "read file successfully" in {
      val app = new EngineExecutorApp()
      val obj = app.readJsonIfFileExists[Map[String, String]](getClass.getResource("/test.json").getPath())
      obj("test") shouldEqual "ok"
    }

    "throw exception trying to read inexistent file" in {
      val app = new EngineExecutorApp()

      val caught = intercept[MarvinEExecutorException] {
        app.readJsonIfFileExists[Map[String, String]]("invalid_path/test.json")
      }

      caught.getMessage() shouldEqual "The file [invalid_path/test.json] does not exists. Check your engine configuration."
    }

    "throw exception trying to read invalid file" in {
      val app = new EngineExecutorApp()
      val filePath = getClass.getResource("/test-invalid.json").getPath()

      val caught = intercept[MarvinEExecutorException] {
        app.readJsonIfFileExists[Map[String, String]](filePath)
      }

      caught.getMessage() shouldEqual s"The file [$filePath] is an invalid json file. Check your file syntax!"
    }
  }

  "setupAdministration method" should {
    "enabling security" in {
      val app = new EngineExecutorApp()
      val mockApi = mock[GenericAPIFunctions]
      val metadata = MetadataMock.simpleMockedMetadata()
      val mockSystem = ActorSystem("TestSystem")

      app.vmParams = Map[String, Any]("enableAdmin" -> true)
      app.api = mockApi

      (mockApi.getSystem _).expects().returns(mockSystem).once()

      app.setupAdministration()

      TestKit.shutdownActorSystem(mockSystem)
    }

    "not enabling security" in {
      val app = new EngineExecutorApp()
      val mockApi = mock[GenericAPIFunctions]

      app.vmParams = Map[String, Any]("enableAdmin" -> false)
      app.api = mockApi

      app.setupAdministration()
    }
  }

  "setupGenericAPI method" should {
    "starting api successfully" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any](
        "engineHome" -> getClass.getResource("/engine_home").getPath(),
        "enableAdmin" -> false,
        "protocol" -> "",
        "enableValidation" -> false
      )

      val api = app.setupGenericAPI()

      api.manageableActors.keys.size shouldBe 7
      api.manageableActors.keys.toList shouldBe List("tpreparator", "evaluator", "acquisitor", "pipeline", "feedback", "predictor", "trainer")

      var actor: ActorRef = api.manageableActors("tpreparator")
      actor.path.toString shouldEqual "akka://teste_engine/user/tpreparatorActor"

      actor = api.manageableActors("evaluator")
      actor.path.toString shouldEqual "akka://teste_engine/user/evaluatorActor"

      actor = api.manageableActors("acquisitor")
      actor.path.toString shouldEqual "akka://teste_engine/user/acquisitorActor"

      actor = api.manageableActors("pipeline")
      actor.path.toString shouldEqual "akka://teste_engine/user/pipelineActor"

      actor = api.manageableActors("feedback")
      actor.path.toString shouldEqual "akka://teste_engine/user/feedbackActor"

      actor = api.manageableActors("predictor")
      actor.path.toString shouldEqual "akka://teste_engine/user/predictorFSM"

      actor = api.manageableActors("trainer")
      actor.path.toString shouldEqual "akka://teste_engine/user/trainerActor"

      api.getSystem.name shouldEqual "teste_engine"
      api.getMetadata.name shouldEqual "teste_engine"
      api.getEngineParams shouldEqual "{\"PARAM_1\":\"VALUE_OF_PARAM_1\"}"

      TestKit.shutdownActorSystem(api.getSystem)
    }
  }
}