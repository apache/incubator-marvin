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

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCode, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Inside, Matchers, WordSpec}
import akka.util.Timeout

import scala.concurrent.duration._
import ContentTypes._
import akka.actor.{ActorSystem, Terminated}
import org.marvin.executor.actions.BatchAction.BatchExecute
import org.marvin.executor.actions.OnlineAction.{OnlineExecute}
import org.marvin.model.MarvinEExecutorException
import org.marvin.util.ProtocolUtil

import scala.concurrent.Future

class GenericHttpAPITest extends WordSpec with ScalatestRouteTest with Matchers with Inside with MockFactory {

  val route = GenericHttpAPI.routes

  "/predictor endpoint" should {

    "interpret the input message and respond with media type json" in {

      val probe = setupProbe()
      GenericHttpAPI.predictorFSM = probe.ref

      val result = Post("/predictor", HttpEntity(`application/json`, s"""{"params":"testParams","message":"testQuery"}""")) ~> route ~> runRoute

      val expectedMessage = OnlineExecute(params="testParams", message="testQuery")
      probe.expectMsg(expectedMessage)
      probe.reply("fooReply")

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"fooReply"}"""
      }(result)
    }

    "gracefully fail when message is not informed" in {
      Post("/predictor", HttpEntity(`application/json`, s"""{"params":"testParams"}""")) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"requirement failed: The request payload must contain the attribute 'message'."}"""
      }
    }

    "use default params when no params is informed" in {
      val probe = setupProbe()
      GenericHttpAPI.predictorFSM = probe.ref
      GenericHttpAPI.defaultParams = "default for test"

      val result = Post("/predictor", HttpEntity(`application/json`, s"""{"message":"testQuery"}""")) ~> route ~> runRoute

      val expectedMessage = OnlineExecute(params="default for test", message="testQuery")
      probe.expectMsg(expectedMessage)
      probe.reply("noParams")

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"noParams"}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {
      val probe = setupProbe()
      GenericHttpAPI.predictorFSM = probe.ref
      GenericHttpAPI.onlineActionTimeout = Timeout(50 millis)

      val result = Post("/predictor", HttpEntity(`application/json`, s"""{"params":"testParams","message":"testQuery"}""")) ~> route ~> runRoute

      val expectedMessage = OnlineExecute(params="testParams", message="testQuery")
      probe.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }

  }

/*  "/predictor/reload endpoint" should {

    "call ArtifactLoaderActor" in {
      val probe = setupProbe()
      GenericHttpAPI.predictorActor = probe.ref

      val result = Put("/predictor/reload?protocol=1234") ~> route ~> runRoute

      val expectedMessage = OnlineReload(actionName = "predictor", protocol = "1234")
      probe.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Let's start!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/predictor/reload") ~> Route.seal(route) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }
*/
  "/acquisitor endpoint" should {

    "interpret params and call BatchActor" in {
      val probe = setupProbe()
      mockProtocolService()
      GenericHttpAPI.acquisitorActor = probe.ref

      val result = Post("/acquisitor", HttpEntity(`application/json`, s"""{"params": "testParams"}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(params = "testParams", protocol="mockedProtocol")
      probe.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"mockedProtocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {
      val probe = setupProbe()
      mockProtocolService()
      GenericHttpAPI.acquisitorActor = probe.ref
      GenericHttpAPI.defaultParams = "default for test"

      val result = Post("/acquisitor", HttpEntity(`application/json`, s"""{}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(params = "default for test", protocol="mockedProtocol")
      probe.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"mockedProtocol"}"""
      }(result)
    }
  }

  "/tpreparator endpoint" should {

    "interpret params and call BatchActor" in {
      val probe = setupProbe()
      mockProtocolService()
      GenericHttpAPI.tpreparatorActor = probe.ref

      val result = Post("/tpreparator", HttpEntity(`application/json`, s"""{"params": "testParams"}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(params = "testParams", protocol="mockedProtocol")
      probe.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"mockedProtocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {
      val probe = setupProbe()
      mockProtocolService()
      GenericHttpAPI.tpreparatorActor = probe.ref
      GenericHttpAPI.defaultParams = "default for test"

      val result = Post("/tpreparator", HttpEntity(`application/json`, s"""{}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(params = "default for test", protocol="mockedProtocol")
      probe.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"mockedProtocol"}"""
      }(result)
    }
  }
/*
  "/tpreparator/reload endpoint" should {

    "call ArtifactLoaderActor" in {
      val probe = setupProbe()
      GenericHttpAPI.tpreparatorActor = probe.ref

      val result = Put("/tpreparator/reload?protocol=1234") ~> route ~> runRoute

      val expectedMessage = BatchArtifactLoaderMessage(actionName = "tpreparator", protocol = "1234")
      probe.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Let's start!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/tpreparator/reload") ~> Route.seal(route) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }
*/
  "/trainer endpoint" should {

    "interpret params and call BatchActor" in {
      val probe = setupProbe()
      mockProtocolService()
      GenericHttpAPI.trainerActor = probe.ref

      val result = Post("/trainer", HttpEntity(`application/json`, s"""{"params": "testParams"}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(params = "testParams", protocol="mockedProtocol")
      probe.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"mockedProtocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {
      val probe = setupProbe()
      mockProtocolService()
      GenericHttpAPI.trainerActor = probe.ref
      GenericHttpAPI.defaultParams = "default for test"

      val result = Post("/trainer", HttpEntity(`application/json`, s"""{}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(params = "default for test", protocol="mockedProtocol")
      probe.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"mockedProtocol"}"""
      }(result)
    }
  }

  /*
  "/trainer/reload endpoint" should {

    "call ArtifactLoaderActor" in {
      val probe = setupProbe()
      GenericHttpAPI.trainerActor = probe.ref

      val result = Put("/trainer/reload?protocol=12345") ~> route ~> runRoute

      val expectedMessage = BatchArtifactLoaderMessage(actionName = "trainer", protocol = "12345")
      probe.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Let's start!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {
      Put("/trainer/reload") ~> Route.seal(route) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }
*/
  "/evaluator endpoint" should {

    "interpret params and call BatchActor" in {
      val probe = setupProbe()
      mockProtocolService()
      GenericHttpAPI.evaluatorActor = probe.ref

      val result = Post("/evaluator", HttpEntity(`application/json`, s"""{"params": "testParams"}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(params = "testParams", protocol = "mockedProtocol")
      probe.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"mockedProtocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {
      val probe = setupProbe()
      mockProtocolService()
      GenericHttpAPI.evaluatorActor = probe.ref
      GenericHttpAPI.defaultParams = "default for test"

      val result = Post("/evaluator", HttpEntity(`application/json`, s"""{}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(params = "default for test", protocol = "mockedProtocol")
      probe.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"mockedProtocol"}"""
      }(result)
    }
  }

  /*
  "/evaluator/reload endpoint" should {

    "call ArtifactLoaderActor" in {
      val probe = setupProbe()
      GenericHttpAPI.evaluatorActor = probe.ref

      val result = Put("/evaluator/reload?protocol=123456") ~> route ~> runRoute

      val expectedMessage = BatchArtifactLoaderMessage(actionName = "evaluator", protocol = "123456")
      probe.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Let's start!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {
      Put("/evaluator/reload") ~> Route.seal(route) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }
*/
  "main method" should {

    "load paths, ip and port from system configuration" in {

      val apiMock = mock[GenericHttpAPIOpen]
      GenericHttpAPI.api = apiMock

      (apiMock.setupSystem _).expects("a/fake/path/engine.metadata", "a/fake/path/engine.params", "").returning(null)
      (apiMock.startServer _).expects("1.1.1.1", 9999, null).returning(1)

      GenericHttpAPI.main(null)
    }
  }

  "setupSystem method" should {

    "throw a friendly exception when engine file does not exists" in {
      val httpApi = new GenericHttpAPIOpen(new ProtocolUtil())

      val existentFile = getClass.getResource("/test.json").getPath()

      val caught =
        intercept[MarvinEExecutorException] {
          httpApi.setupSystem("not_existent_engine_file", existentFile, "")
        }
      caught.getMessage() shouldEqual "The file [not_existent_engine_file] does not exists. Check your engine configuration."
    }

    "throw a friendly exception when params file does not exists" in {
      val httpApi = new GenericHttpAPIOpen(new ProtocolUtil())

      val existentFile = getClass.getResource("/test.json").getPath()

      val caught =
        intercept[MarvinEExecutorException] {
          httpApi.setupSystem(existentFile, "not_existent_params_file", "")
        }
      caught.getMessage() shouldEqual "The file [not_existent_params_file] does not exists. Check your engine configuration."
    }

    "do not throw exception and setup the system when params files are valid" in {
      val httpApi = new GenericHttpAPIOpen(new ProtocolUtil())

      val validMetadataFile = getClass.getResource("/valid.metadata").getPath()
      val validParamsFile = getClass.getResource("/valid.params").getPath()

      val system = httpApi.setupSystem(validMetadataFile, validParamsFile, "")
      system shouldNot be(null)
    }
  }

  def setupProbe() : TestProbe = {
    val probe = TestProbe()
    val timeout = Timeout(3 seconds)
    GenericHttpAPI.system = system
    GenericHttpAPI.onlineActionTimeout = timeout
    GenericHttpAPI.healthCheckTimeout = timeout

    probe
  }

  def mockProtocolService(): Unit = {
    val protocolUtil = mock[ProtocolUtil]
    (protocolUtil.generateProtocol _).expects(*).returning("mockedProtocol")
    GenericHttpAPI.api = new GenericHttpAPIImpl()
    GenericHttpAPI.protocolUtil = protocolUtil
  }
}

class GenericHttpAPIOpen(var protocolService: ProtocolUtil) extends GenericHttpAPI {
  override def setupSystem(engineFilePath: String, paramsFilePath: String, modelProtocol: String): ActorSystem = super.setupSystem(engineFilePath, paramsFilePath, modelProtocol)
  override def startServer(ipAddress: String, port: Int, system: ActorSystem): Unit = super.startServer(ipAddress, port, system)
  override def terminate(): Future[Terminated] = super.terminate()
}
