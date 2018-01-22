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

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.marvin.model.MarvinEExecutorException
import org.scalamock.scalatest.MockFactory
import org.scalatest._


class EngineExecutorAppTest extends
  TestKit(ActorSystem("BatchActionTest", ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]""")))
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

      app.vmParams = Map[String, Any]("metadataFilePath" -> getClass.getResource("/valid.metadata").getPath())

      val metadata = app.getEngineMetadata()

      metadata.name shouldEqual "teste_engine"
    }

    "with invalid filePath" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]("metadataFilePath" -> "invalid_path")

      val caught = intercept[MarvinEExecutorException] {
        app.getEngineMetadata()
      }

      caught.getMessage() shouldEqual "The file [invalid_path] does not exists. Check your engine configuration."
    }

    "without a correct vmParameter key" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]()

      val caught = intercept[NoSuchElementException] {
        app.getEngineMetadata()
      }

      caught.getMessage() shouldEqual "key not found: engineFilePath"
    }
  }

  "getEngineParameters method" should {

    "with valid filePath" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]("paramsFilePath" -> getClass.getResource("/valid.params").getPath())

      val params = app.getEngineParameters()

      params shouldEqual "{\"PARAM_1\":\"VALUE_OF_PARAM_1\"}"
    }

    "with invalid filePath" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]("paramsFilePath" -> "invalid_path")

      val caught = intercept[MarvinEExecutorException] {
        app.getEngineParameters()
      }

      caught.getMessage() shouldEqual "The file [invalid_path] does not exists. Check your engine configuration."
    }

    "without a correct vmParameter key" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]()

      val caught = intercept[NoSuchElementException] {
        app.getEngineParameters()
      }

      caught.getMessage() shouldEqual "key not found: paramsFilePath"
    }
  }

  "getVMParameters method" should {

    "with valid filePath" in {
      val app = new EngineExecutorApp()

      app.vmParams = Map[String, Any]("paramsFilePath" -> getClass.getResource("/valid.params").getPath())

      val vmParams = app.getVMParameters()

      vmParams shouldEqual Map[String, Any](
        "metadataFilePath" -> "a/fake/path/engine.metadata",
        "paramsFilePath" -> "a/fake/path/engine.params",
        "ipAddress" -> "1.1.1.1",
        "port" -> 9999,
        "protocol" -> "",
        "enableAdmin" -> false,
        "adminPort" -> 50100,
        "adminHost" -> "127.0.0.1"
      )
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

  "main method" should {

//    "enabling admin" in {
//      val app = new EngineExecutorApp()
//      val host = "127.0.0.99"
//      val port = 9991
//      app.vmParams = Map[String, Any]("enableAdmin" -> true, "adminPort" -> port, "adminHost" -> host)
//
//      EngineExecutorApp.main(null)
//
//      config.getString("akka.actor.provider") shouldEqual "remote"
//      config.getString("akka.remote.artery.enabled") shouldEqual "on"
//      config.getString("akka.remote.artery.canonical.hostname") shouldEqual host
//      config.getInt("akka.remote.artery.canonical.port") shouldEqual port
//    }
//
//    "disabling admin" in {
//      EngineExecutorApp.vmParams = Map[String, Any]("enableAdmin" -> false)
//
//      EngineExecutorApp.main(null)
//
//      config.getString("akka.actor.provider") shouldEqual "local"
//      config.getString("akka.remote.artery.enabled") shouldEqual "off"
//    }

  }
}