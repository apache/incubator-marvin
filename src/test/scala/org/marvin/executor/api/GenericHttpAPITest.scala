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
import actions.HealthCheckResponse.Status
import akka.actor.{ActorRef, ActorSystem, Terminated}
import akka.event.Logging
import akka.http.scaladsl.server.Route
import org.marvin.executor.actions.BatchAction.{BatchExecute, BatchHealthCheck, BatchReload}
import org.marvin.executor.actions.OnlineAction.{OnlineExecute, OnlineHealthCheck, OnlineReload}
import org.marvin.executor.actions.PipelineAction.PipelineExecute
import org.marvin.executor.statemachine.Reload
import org.marvin.model.MarvinEExecutorException
import org.marvin.testutil.MetadataMock
import org.marvin.util.ProtocolUtil

import scala.concurrent.Future

class GenericHttpAPITest extends WordSpec with ScalatestRouteTest with Matchers with Inside with MockFactory {

  val route = GenericHttpAPI.routes
  val testActors = setupGenericHttpAPIActors()

  "/predictor endpoint" should {

    "interpret the input message and respond with media type json" in {

      val message = "testQuery"
      val params = "testParams"
      val response = "fooReply"

      val result = Post("/predictor", HttpEntity(`application/json`, s"""{"params":"$params","message":"$message"}""")) ~> route ~> runRoute

      val expectedMessage = OnlineExecute(message, params)
      testActors("predictor").expectMsg(expectedMessage)
      testActors("predictor").reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "gracefully fail when message is not informed" in {
      Post("/predictor", HttpEntity(`application/json`, s"""{"params":"testParams"}""")) ~> route ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"requirement failed: The request payload must contain the attribute 'message'."}"""
      }
    }

    "use default params when no params is informed" in {

      val message = "testQuery"
      val response = "noParams"

      val result = Post("/predictor", HttpEntity(`application/json`, s"""{"message":"$message"}""")) ~> route ~> runRoute

      val expectedMessage = OnlineExecute(message, GenericHttpAPI.defaultParams)
      testActors("predictor").expectMsg(expectedMessage)
      testActors("predictor").reply(response)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val message = "testQuery"

      GenericHttpAPI.onlineActionTimeout = Timeout(50 millis)

      val result = Post("/predictor", HttpEntity(`application/json`, s"""{"message":"$message"}""")) ~> route ~> runRoute

      val expectedMessage = OnlineExecute(message, GenericHttpAPI.defaultParams)
      testActors("predictor").expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/acquisitor endpoint" should {

    "interpret params and call BatchActor" in {

      val protocol = "mockedProtocol"
      mockProtocolUtil(protocol)

      val params = "testParams"

      val result = Post("/acquisitor", HttpEntity(`application/json`, s"""{"params": "$params"}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(protocol, params)
      testActors("acquisitor").expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {

      val protocol = "mockedProtocol"
      mockProtocolUtil(protocol)

      val result = Post("/acquisitor", HttpEntity(`application/json`, s"""{}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(protocol, GenericHttpAPI.defaultParams)
      testActors("acquisitor").expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }
  }

  "/tpreparator endpoint" should {

    "interpret params and call BatchActor" in {

      val protocol = "mockedProtocol"
      mockProtocolUtil(protocol)

      val params = "testParams"

      val result = Post("/tpreparator", HttpEntity(`application/json`, s"""{"params": "$params"}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(protocol, params)
      testActors("tpreparator").expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {

      val protocol = "mockedProtocol"
      mockProtocolUtil(protocol)

      val result = Post("/tpreparator", HttpEntity(`application/json`, s"""{}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(protocol, GenericHttpAPI.defaultParams)
      testActors("tpreparator").expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }
  }

  "/trainer endpoint" should {

    "interpret params and call BatchActor" in {

      val protocol = "mockedProtocol"
      mockProtocolUtil(protocol)

      val params = "testParams"

      val result = Post("/trainer", HttpEntity(`application/json`, s"""{"params": "$params"}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(protocol, params)
      testActors("trainer").expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {

      val protocol = "mockedProtocol"
      mockProtocolUtil(protocol)

      val result = Post("/trainer", HttpEntity(`application/json`, s"""{}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(protocol, GenericHttpAPI.defaultParams)
      testActors("trainer").expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }
  }

  "/evaluator endpoint" should {

    "interpret params and call BatchActor" in {

      val protocol = "mockedProtocol"
      mockProtocolUtil(protocol)

      val params = "testParams"

      val result = Post("/evaluator", HttpEntity(`application/json`, s"""{"params": "$params"}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(protocol, params)
      testActors("evaluator").expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {

      val protocol = "mockedProtocol"
      mockProtocolUtil(protocol)

      val result = Post("/evaluator", HttpEntity(`application/json`, s"""{}""")) ~> route ~> runRoute

      val expectedMessage = BatchExecute(protocol, GenericHttpAPI.defaultParams)
      testActors("evaluator").expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }
  }

  "/pipeline endpoint" should {

    "interpret params and call PipelineActor" in {

      val protocol = "mockedProtocol"
      mockProtocolUtil(protocol)

      val params = "testParams"

      val result = Post("/pipeline", HttpEntity(`application/json`, s"""{"params": "$params"}""")) ~> route ~> runRoute

      val expectedMessage = PipelineExecute(protocol, params)
      testActors("pipeline").expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {

      val protocol = "mockedProtocol"
      mockProtocolUtil(protocol)

      val result = Post("/pipeline", HttpEntity(`application/json`, s"""{}""")) ~> route ~> runRoute

      val expectedMessage = PipelineExecute(protocol, GenericHttpAPI.defaultParams)
      testActors("pipeline").expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }
  }

  "/predictor/reload endpoint" should {

    "call OnlineReload" in {

      val protocol = "testProtocol"

      val result = Put(s"/predictor/reload?protocol=$protocol") ~> route ~> runRoute

      val expectedMessage = Reload(protocol)
      testActors("predictor").expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Work in progress...Thank you folk!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/predictor/reload") ~> Route.seal(route) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }

  "/tpreparator/reload endpoint" should {

    "call BatchReload" in {

      val protocol = "testProtocol"

      val result = Put(s"/tpreparator/reload?protocol=$protocol") ~> route ~> runRoute

      val expectedMessage = BatchReload(protocol)
      testActors("tpreparator").expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Work in progress...Thank you folk!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/tpreparator/reload") ~> Route.seal(route) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }

  "/trainer/reload endpoint" should {

    "call BatchReload" in {

      val protocol = "testProtocol"

      val result = Put(s"/trainer/reload?protocol=$protocol") ~> route ~> runRoute

      val expectedMessage = BatchReload(protocol)
      testActors("trainer").expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Work in progress...Thank you folk!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/trainer/reload") ~> Route.seal(route) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }

  "/evaluator/reload endpoint" should {

    "call BatchReload" in {

      val protocol = "testProtocol"

      val result = Put(s"/evaluator/reload?protocol=$protocol") ~> route ~> runRoute

      val expectedMessage = BatchReload(protocol)
      testActors("evaluator").expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Work in progress...Thank you folk!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/evaluator/reload") ~> Route.seal(route) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }

  "/predictor/health endpoint" should {

    "interpret and respond with media type json" in {

      val response = Status.OK

      val result = Get("/predictor/health") ~> route ~> runRoute

      val expectedMessage = OnlineHealthCheck
      testActors("predictor").expectMsg(expectedMessage)
      testActors("predictor").reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val message = "testQuery"

      GenericHttpAPI.healthCheckTimeout = Timeout(2 millis)

      val result = Get("/predictor/health") ~> route ~> runRoute

      val expectedMessage = OnlineHealthCheck
      testActors("predictor").expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/acquisitor/health endpoint" should {

    "interpret and respond with media type json" in {

      val response = Status.OK

      val result = Get("/acquisitor/health") ~> route ~> runRoute

      val expectedMessage = BatchHealthCheck
      testActors("acquisitor").expectMsg(expectedMessage)
      testActors("acquisitor").reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      GenericHttpAPI.healthCheckTimeout = Timeout(2 millis)

      val result = Get("/acquisitor/health") ~> route ~> runRoute

      val expectedMessage = BatchHealthCheck
      testActors("acquisitor").expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/tpreparator/health endpoint" should {

    "interpret and respond with media type json" in {

      val response = Status.OK

      val result = Get("/tpreparator/health") ~> route ~> runRoute

      val expectedMessage = BatchHealthCheck
      testActors("tpreparator").expectMsg(expectedMessage)
      testActors("tpreparator").reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      GenericHttpAPI.healthCheckTimeout = Timeout(2 millis)

      val result = Get("/tpreparator/health") ~> route ~> runRoute

      val expectedMessage = BatchHealthCheck
      testActors("tpreparator").expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/trainer/health endpoint" should {

    "interpret and respond with media type json" in {

      val response = Status.OK

      val result = Get("/trainer/health") ~> route ~> runRoute

      val expectedMessage = BatchHealthCheck
      testActors("trainer").expectMsg(expectedMessage)
      testActors("trainer").reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      GenericHttpAPI.healthCheckTimeout = Timeout(2 millis)

      val result = Get("/trainer/health") ~> route ~> runRoute

      val expectedMessage = BatchHealthCheck
      testActors("trainer").expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/evaluator/health endpoint" should {

    "interpret and respond with media type json" in {

      val response = Status.OK

      val result = Get("/evaluator/health") ~> route ~> runRoute

      val expectedMessage = BatchHealthCheck
      testActors("evaluator").expectMsg(expectedMessage)
      testActors("evaluator").reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      GenericHttpAPI.healthCheckTimeout = Timeout(2 millis)

      val result = Get("/evaluator/health") ~> route ~> runRoute

      val expectedMessage = BatchHealthCheck
      testActors("evaluator").expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

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

      val existentFile = getClass.getResource("/metadataToValidate.json").getPath()

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

  def setupGenericHttpAPIActors(): Map[String, TestProbe] = {
    val timeout = Timeout(3 seconds)
    GenericHttpAPI.system = system
    GenericHttpAPI.onlineActionTimeout = timeout
    GenericHttpAPI.healthCheckTimeout = timeout
    GenericHttpAPI.batchActionTimeout = timeout
    GenericHttpAPI.reloadTimeout = timeout
    GenericHttpAPI.pipelineTimeout = timeout

    GenericHttpAPI.metadata = MetadataMock.simpleMockedMetadata()
    GenericHttpAPI.log = Logging.getLogger(system, this)
    GenericHttpAPI.defaultParams = "testParams"

    val testActors = Map[String, TestProbe](
      "predictor" -> TestProbe(),
      "acquisitor" -> TestProbe(),
      "tpreparator" -> TestProbe(),
      "trainer" -> TestProbe(),
      "evaluator" -> TestProbe(),
      "pipeline" -> TestProbe()
    )

    GenericHttpAPI.actors = Map[String, ActorRef](
      "predictor" -> testActors("predictor").ref,
      "acquisitor" -> testActors("acquisitor").ref,
      "tpreparator" -> testActors("tpreparator").ref,
      "trainer" -> testActors("trainer").ref,
      "evaluator" -> testActors("evaluator").ref,
      "pipeline" -> testActors("pipeline").ref
    )

    testActors
  }

  def mockProtocolUtil(protocolValue:String) {
    val protocolUtil = mock[ProtocolUtil]
    (protocolUtil.generateProtocol _).expects(*).returning(protocolValue)
    GenericHttpAPI.protocolUtil = protocolUtil
  }
}

class GenericHttpAPIOpen(var protocolService: ProtocolUtil) extends GenericHttpAPI {
  override def setupSystem(engineFilePath: String, paramsFilePath: String, modelProtocol: String): ActorSystem = super.setupSystem(engineFilePath, paramsFilePath, modelProtocol)
  override def startServer(ipAddress: String, port: Int, system: ActorSystem): Unit = super.startServer(ipAddress, port, system)
  override def terminate(): Future[Terminated] = super.terminate()
}
