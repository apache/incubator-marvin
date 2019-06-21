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
package org.apache.marvin.executor.api

import actions.HealthCheckResponse.Status
import akka.actor.ActorRef
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import org.apache.marvin.exception.MarvinEExecutorException
import org.apache.marvin.executor.actions.BatchAction.{BatchExecute, BatchExecutionStatus, BatchHealthCheck, BatchReload}
import org.apache.marvin.executor.actions.OnlineAction.{OnlineExecute, OnlineHealthCheck}
import org.apache.marvin.executor.actions.PipelineAction.{PipelineExecute, PipelineExecutionStatus}
import org.apache.marvin.executor.statemachine.Reload
import org.apache.marvin.fixtures.MetadataMock
import org.apache.marvin.model.EngineMetadata
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Inside, Matchers, WordSpec}
import spray.json.{JsValue, _}

import scala.io.Source


class GenericAPITest extends WordSpec with ScalatestRouteTest with Matchers with Inside with MockFactory {

  "/predictor endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("predictor" -> actor.ref), null, null)

    "interpret the input message and respond with media type json" in {

      val message = "{\"msg\":\"testQuery\"}"
      val params = "{\"p1\":\"testParams\"}"
      val response = "fooReply"

      val result = Post("/predictor", HttpEntity(`application/json`, s"""{"params":$params,"message":$message}""")) ~> api.routes ~> runRoute

      val expectedMessage = OnlineExecute(message, params)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "gracefully fail when message is not informed" in {
      Post("/predictor", HttpEntity(`application/json`, s"""{"params":"testParams"}""")) ~> api.routes ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"requirement failed: The request payload must contain the attribute 'message'."}"""
      }
    }

    "use default params when no params is informed" in {

      val message = "{\"msg\":\"testQuery\"}"
      val response = "check"
      val result = Post("/predictor", HttpEntity(`application/json`, s"""{"message":$message}""")) ~> api.routes ~> runRoute

      val expectedMessage = OnlineExecute(message, defaultParams)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val message = "{\"msg\":\"testQuery\"}"

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 50,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("predictor" -> actor.ref), null, null)

      val result = Post("/predictor", HttpEntity(`application/json`, s"""{"message":$message}""")) ~> api.routes ~> runRoute

      val expectedMessage = OnlineExecute(message, defaultParams)
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }

    "validate the input message when there is a schema file in the root folder" in{
      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 50,
        onlineActionTimeout = 3000,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val messageSchemaFile = getClass.getResource("/engine_home/predictor-message.schema").getPath
      val schemas = Map[String, String]("predictor-message"-> Source.fromFile(messageSchemaFile).mkString)

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("predictor" -> actor.ref), null, schemas)

      val message = "{\n  \"msg\":\"testQuery\"\n}"

      val result = Post("/predictor", HttpEntity(`application/json`, s"""{"message":$message}""")) ~> api.routes ~> runRoute

      check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual "{\"errorMessage\":\"object has missing required properties ([\\\"firstName\\\",\\\"lastName\\\"])\"}"
      }(result)
    }

    "validate the input message and accept when there is a schema file in the root folder" in {
      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 50,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val messageSchemaFile = getClass.getResource("/engine_home/predictor-message.schema").getPath
      val schemas = Map[String, String]("predictor-message"-> Source.fromFile(messageSchemaFile).mkString)

      val response = "responseText"

      val message = "{\"firstName\":\"testName\",\"lastName\":\"testLastName\"}"

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("predictor" -> actor.ref), null, schemas)

      val result = Post("/predictor", HttpEntity(`application/json`, s"""{"message":$message}""")) ~> api.routes ~> runRoute

      val expectedMessage = OnlineExecute(message, defaultParams)
      actor.expectMsg(expectedMessage)
      actor.reply(response)
      check{
        responseAs[String] shouldEqual "{\"result\":\"responseText\"}"
        status shouldEqual StatusCodes.OK
      }(result)
    }

    "validate several predictions and accept when there is a schema file in the root folder" in {
      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 50,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val messageSchemaFile = getClass.getResource("/engine_home/predictor-message.schema").getPath
      val schemas = Map[String, String]("predictor-message"-> Source.fromFile(messageSchemaFile).mkString)

      val response = "ok"

      val message = "{\"firstName\":\"testName\",\"lastName\":\"testLastName\"}"

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("predictor" -> actor.ref), null, schemas)

      for (i <- 0 to 2) {
        println(s"Testing post $i.")
        val result = Post("/predictor", HttpEntity(`application/json`, s"""{"message":$message}""")) ~> api.routes ~> runRoute

        val expectedMessage = OnlineExecute(message, defaultParams)
        actor.expectMsg(expectedMessage)
        actor.reply(response)
        check {
          responseAs[String] shouldEqual "{\"result\":\"ok\"}"
          status shouldEqual StatusCodes.OK
        }(result)
      }
    }
  }

  "/feedback endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("feedback" -> actor.ref), null, null)

    "interpret the input message and respond with media type json" in {
      val message = "{\"msg\":\"testQuery\"}"
      val params = "{\"p1\":\"testParams\"}"
      val response = "fooReply"

      val result = Post("/feedback", HttpEntity(`application/json`, s"""{"params":$params,"message":$message}""")) ~> api.routes ~> runRoute

      val expectedMessage = OnlineExecute(message, params)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "gracefully fail when message is not informed" in {
      Post("/feedback", HttpEntity(`application/json`, s"""{"params":"testParams"}""")) ~> api.routes ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"requirement failed: The request payload must contain the attribute 'message'."}"""
      }
    }

    "use default params when no params is informed" in {

      val message = "{\"msg\":\"testQuery\"}"
      val response = "noParams"

      val result = Post("/feedback", HttpEntity(`application/json`, s"""{"message":$message}""")) ~> api.routes ~> runRoute

      val expectedMessage = OnlineExecute(message, defaultParams)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val message = "{\"msg\":\"testQuery\"}"

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 50,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("feedback" -> actor.ref), null, null)

      val result = Post("/feedback", HttpEntity(`application/json`, s"""{"message":$message}""")) ~> api.routes ~> runRoute

      val expectedMessage = OnlineExecute(message, defaultParams)
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/acquisitor endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val protocol = "acquisitor_mockedProtocol"

    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("acquisitor" -> actor.ref), null, null){
      override def generateProtocol(actionName: String):String = protocol
    }

    "interpret params and call BatchActor" in {

      val params = "{\"p1\":\"testParams\"}"

      val result = Post("/acquisitor", HttpEntity(`application/json`, s"""{"params":$params}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, params)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {

      val result = Post("/acquisitor", HttpEntity(`application/json`, s"""{}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, defaultParams)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }
  }

  "/tpreparator endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val protocol = "tpreparator_mockedProtocol"

    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("tpreparator" -> actor.ref), null, null){
      override def generateProtocol(actionName: String):String = protocol
    }

    "interpret params and call BatchActor" in {

      val params = "{\"p1\":\"testParams\"}"

      val result = Post("/tpreparator", HttpEntity(`application/json`, s"""{"params":$params}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, params)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {

      val result = Post("/tpreparator", HttpEntity(`application/json`, s"""{}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, defaultParams)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }
  }

  "/trainer endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val protocol = "trainer_mockedProtocol"

    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("trainer" -> actor.ref), null, null){
      override def generateProtocol(actionName: String):String = protocol
    }



    "interpret params and call BatchActor" in {

      val params = "{\"p1\":\"testParams\"}"

      val result = Post("/trainer", HttpEntity(`application/json`, s"""{"params":$params}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, params)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {

      val result = Post("/trainer", HttpEntity(`application/json`, s"""{}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, defaultParams)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }
  }

  "/evaluator endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val protocol = "evaluator_mockedProtocol"

    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("evaluator" -> actor.ref), null, null){
      override def generateProtocol(actionName: String):String = protocol
    }

    "interpret params and call BatchActor" in {

      val params = "{\"p1\":\"testParams\"}"

      val result = Post("/evaluator", HttpEntity(`application/json`, s"""{"params":$params}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, params)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {

      val result = Post("/evaluator", HttpEntity(`application/json`, s"""{}""")) ~> api.routes ~> runRoute

      val expectedMessage = BatchExecute(protocol, defaultParams)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }
  }

  "/pipeline endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val protocol = "pipeline_mockedProtocol"

    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("pipeline" -> actor.ref), null, null){
      override def generateProtocol(actionName: String):String = protocol
    }



    "interpret params and call PipelineActor" in {

      val params = "{\"p1\":\"testParams\"}"

      val result = Post("/pipeline", HttpEntity(`application/json`, s"""{"params":$params}""")) ~> api.routes ~> runRoute

      val expectedMessage = PipelineExecute(protocol, params)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }

    "use default params when no params is informed" in {

      val result = Post("/pipeline", HttpEntity(`application/json`, s"""{}""")) ~> api.routes ~> runRoute

      val expectedMessage = PipelineExecute(protocol, defaultParams)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"$protocol"}"""
      }(result)
    }
  }

  "/predictor/reload endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("predictor" -> actor.ref), null, null)

    "call OnlineReload" in {

      val protocol = "testProtocol"

      val result = Put(s"/predictor/reload?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = Reload(protocol)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Work in progress...Thank you folk!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/predictor/reload") ~> Route.seal(api.routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }

  "/feedback/reload endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("feedback" -> actor.ref), null, null)

    "call OnlineReload" in {

      val protocol = "testProtocol"

      val result = Put(s"/feedback/reload?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = Reload(protocol)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Work in progress...Thank you folk!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/predictor/reload") ~> Route.seal(api.routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }

  "/tpreparator/reload endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("tpreparator" -> actor.ref), null, null)

    "call BatchReload" in {

      val protocol = "testProtocol"

      val result = Put(s"/tpreparator/reload?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = BatchReload(protocol)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Work in progress...Thank you folk!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/tpreparator/reload") ~> Route.seal(api.routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }

  "/trainer/reload endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("trainer" -> actor.ref), null, null)

    "call BatchReload" in {

      val protocol = "testProtocol"

      val result = Put(s"/trainer/reload?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = BatchReload(protocol)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Work in progress...Thank you folk!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/trainer/reload") ~> Route.seal(api.routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }

  "/evaluator/reload endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("evaluator" -> actor.ref), null, null)

    "call BatchReload" in {

      val protocol = "testProtocol"

      val result = Put(s"/evaluator/reload?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = BatchReload(protocol)
      actor.expectMsg(expectedMessage)

      check{
        status shouldEqual StatusCodes.OK
        responseAs[String] shouldEqual s"""{"result":"Work in progress...Thank you folk!"}"""
      }(result)
    }

    "fail gracefully when protocol is not informed" in {

      Put("/evaluator/reload") ~> Route.seal(api.routes) ~> check {
        status shouldEqual StatusCodes.BadRequest
        responseAs[String] shouldEqual s"""{"errorMessage":"Missing query parameter. [protocol]"}"""
      }
    }
  }

  "/predictor/health endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("predictor" -> actor.ref), null, null)

    "interpret and respond OK with media type json" in {

      val response = Status.OK

      val result = Get("/predictor/health") ~> api.routes ~> runRoute

      val expectedMessage = OnlineHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "interpret and respond NOK with media type json" in {

      val response = Status.NOK

      val result = Get("/predictor/health") ~> api.routes ~> runRoute

      val expectedMessage = OnlineHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":"Engine did not returned a healthy status."}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 5,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("predictor" -> actor.ref), null, null)

      val result = Get("/predictor/health") ~> api.routes ~> runRoute

      val expectedMessage = OnlineHealthCheck
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/feedback/health endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("feedback" -> actor.ref), null, null)

    "interpret and respond OK with media type json" in {

      val response = Status.OK

      val result = Get("/feedback/health") ~> api.routes ~> runRoute

      val expectedMessage = OnlineHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "interpret and respond NOK with media type json" in {

      val response = Status.NOK

      val result = Get("/feedback/health") ~> api.routes ~> runRoute

      val expectedMessage = OnlineHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":"Engine did not returned a healthy status."}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 5,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("feedback" -> actor.ref), null, null)

      val result = Get("/feedback/health") ~> api.routes ~> runRoute

      val expectedMessage = OnlineHealthCheck
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/acquisitor/health endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("acquisitor" -> actor.ref), null, null)

    "interpret and respond OK with media type json" in {

      val response = Status.OK

      val result = Get("/acquisitor/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "interpret and respond NOK ith media type json" in {

      val response = Status.NOK

      val result = Get("/acquisitor/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":"Engine did not returned a healthy status."}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 5,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("acquisitor" -> actor.ref), null, null)

      val result = Get("/acquisitor/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/tpreparator/health endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("tpreparator" -> actor.ref), null, null)

    "interpret and respond OK with media type json" in {

      val response = Status.OK

      val result = Get("/tpreparator/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "interpret and respond NOK with media type json" in {

      val response = Status.NOK

      val result = Get("/tpreparator/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":"Engine did not returned a healthy status."}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 5,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("tpreparator" -> actor.ref), null, null)

      val result = Get("/tpreparator/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/trainer/health endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("trainer" -> actor.ref), null, null)

    "interpret and respond OK with media type json" in {

      val response = Status.OK

      val result = Get("/trainer/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "interpret and respond NOK with media type json" in {

      val response = Status.NOK

      val result = Get("/trainer/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":"Engine did not returned a healthy status."}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 5,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("trainer" -> actor.ref), null, null)

      val result = Get("/trainer/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }

  }

  "/evaluator/health endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("evaluator" -> actor.ref), null, null)

    "interpret and respond OK with media type json" in {

      val response = Status.OK

      val result = Get("/evaluator/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":""}"""
      }(result)
    }

    "interpret and respond NOK with media type json" in {

      val response = Status.NOK

      val result = Get("/evaluator/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"status":"$response","additionalMessage":"Engine did not returned a healthy status."}"""
      }(result)
    }

    "fail fast when the timeout is reached" in {

      val metadata = EngineMetadata(
        name = "test",
        actions = null,
        artifactsRemotePath = "",
        artifactManagerType = "",
        s3BucketName = "",
        batchActionTimeout = 50,
        engineType = "",
        hdfsHost = "",
        healthCheckTimeout = 5,
        onlineActionTimeout = 50,
        pipelineActions = List(),
        reloadStateTimeout = Some(250),
        reloadTimeout = 50,
        version = "1"
      )

      val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("evaluator" -> actor.ref), null, null)

      val result = Get("/evaluator/health") ~> api.routes ~> runRoute

      val expectedMessage = BatchHealthCheck
      actor.expectMsg(expectedMessage)

      check {
        status shouldEqual StatusCodes.InternalServerError
        responseAs[String] shouldEqual """{"errorMessage":"The engine was not able to provide a response within the specified timeout."}"""
      }(result)

    }
  }

  "/acquisitor/status endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("acquisitor" -> actor.ref), null, null)

    "be requested with a valid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/acquisitor/status?protocol=$protocol") ~> api.routes ~> runRoute

      val response = "foi"
      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "be requested with a invalid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/acquisitor/status?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(akka.actor.Status.Failure(new MarvinEExecutorException(s"Protocol $protocol not found!")))

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"errorMessage":"Protocol testProtocol not found!"}"""
      }(result)
    }
  }

  "/tpreparator/status endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("tpreparator" -> actor.ref), null, null)

    "be requested with a valid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/tpreparator/status?protocol=$protocol") ~> api.routes ~> runRoute

      val response = "foi"
      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "be requested with a invalid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/tpreparator/status?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(akka.actor.Status.Failure(new MarvinEExecutorException(s"Protocol $protocol not found!")))

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"errorMessage":"Protocol testProtocol not found!"}"""
      }(result)
    }
  }

  "/trainer/status endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("trainer" -> actor.ref), null, null)

    "be requested with a valid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/trainer/status?protocol=$protocol") ~> api.routes ~> runRoute

      val response = "foi"
      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "be requested with a invalid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/trainer/status?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(akka.actor.Status.Failure(new MarvinEExecutorException(s"Protocol $protocol not found!")))

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"errorMessage":"Protocol testProtocol not found!"}"""
      }(result)
    }
  }

  "/evaluator/status endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("evaluator" -> actor.ref), null, null)

    "be requested with a valid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/evaluator/status?protocol=$protocol") ~> api.routes ~> runRoute

      val response = "foi"
      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "be requested with a invalid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/evaluator/status?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = BatchExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(akka.actor.Status.Failure(new MarvinEExecutorException(s"Protocol $protocol not found!")))

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"errorMessage":"Protocol testProtocol not found!"}"""
      }(result)
    }
  }

  "/pipeline/status endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val actor = TestProbe()
    val defaultParams = ""
    val api = new GenericAPI(system, metadata, defaultParams, Map[String, ActorRef]("pipeline" -> actor.ref), null, null)

    "be requested with a valid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/pipeline/status?protocol=$protocol") ~> api.routes ~> runRoute

      val response = "foi"
      val expectedMessage = PipelineExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(response)

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"result":"$response"}"""
      }(result)
    }

    "be requested with a invalid protocol" in {
      val protocol = "testProtocol"
      val result = Get(s"/pipeline/status?protocol=$protocol") ~> api.routes ~> runRoute

      val expectedMessage = PipelineExecutionStatus(protocol)
      actor.expectMsg(expectedMessage)
      actor.reply(akka.actor.Status.Failure(new MarvinEExecutorException(s"Protocol $protocol not found!")))

      check {
        status shouldEqual StatusCode.int2StatusCode(503)
        contentType shouldEqual ContentTypes.`application/json`
        responseAs[String] shouldEqual s"""{"errorMessage":"Protocol testProtocol not found!"}"""
      }(result)
    }
  }

  "/docs endpoint" should {

    val metadata = MetadataMock.simpleMockedMetadata()
    val defaultParams = ""
    val docsFilePath = "xxx/xxx.yaml"
    val api = new GenericAPI(system, metadata, defaultParams, null, docsFilePath, null)

    "interpret and respond redirect" in {

      val result = Get("/docs") ~> api.routes ~> runRoute

      check {
        status shouldEqual StatusCode.int2StatusCode(307)
        contentType shouldEqual ContentTypes.`text/html(UTF-8)`
      }(result)
    }

    "interpret and respond ok with media type text/html(UTF-8)" in {

      val result = Get("/docs/index.html") ~> api.routes ~> runRoute

      check {
        status shouldEqual StatusCode.int2StatusCode(200)
        contentType shouldEqual ContentTypes.`text/html(UTF-8)`
      }(result)
    }
  }

  "getMetadata method" should {
    "return right value" in {
      val metadata = MetadataMock.simpleMockedMetadata()
      val api = new GenericAPI(system, metadata, "", null, null, null)

      api.getMetadata shouldEqual metadata
    }
  }

  "getSystem method" should {
    "return right value" in {
      val metadata = MetadataMock.simpleMockedMetadata()
      val api = new GenericAPI(system, metadata, "", null, null, null)

      api.getSystem shouldEqual system
    }
  }

  "getEngineParams method" should {
    "return right value" in {
      val metadata = MetadataMock.simpleMockedMetadata()
      val params = "params"
      val api = new GenericAPI(system, metadata, params, null, null, null)

      api.getEngineParams shouldEqual params
    }
  }

  "manageableActors method" should {
    "return right value" in {
      val metadata = MetadataMock.simpleMockedMetadata()
      val params = "params"
      val actor1 = TestProbe()
      val actors = Map[String, ActorRef]("actor1" -> actor1.ref)
      val api = new GenericAPI(system, metadata, params, actors, null, null)

      api.manageableActors shouldEqual actors
      api.manageableActors("actor1") shouldEqual actor1.ref
    }
  }

  "validate method" should {
    "pass without error" in {
      val metadata = MetadataMock.simpleMockedMetadata()
      val params = "params"
      val actor1 = TestProbe()
      val actors = Map[String, ActorRef]("actor1" -> actor1.ref)
      val messageSchemaFile = getClass.getResource("/engine_home/predictor-message.schema").getPath
      val schemas = Map[String, String]("predictor-message"-> Source.fromFile(messageSchemaFile).mkString)

      val api = new GenericAPI(system, metadata, params, actors, null, schemas)
      val message: Option[JsValue] = Some("{\"firstName\":\"John\", \"lastName\":\"Doe\"}".parseJson)

      try{
        api.validate("predictor-message", message)
        assert(true)

      }catch {
        case _: Throwable =>
          assert(false)
      }

    }
    "not pass with one problem" in {
      val metadata = MetadataMock.simpleMockedMetadata()
      val params = "params"
      val actor1 = TestProbe()
      val actors = Map[String, ActorRef]("actor1" -> actor1.ref)
      val messageSchemaFile = getClass.getResource("/engine_home/predictor-message.schema").getPath
      val schemas = Map[String, String]("predictor-message"-> Source.fromFile(messageSchemaFile).mkString)

      val api = new GenericAPI(system, metadata, params, actors, null, schemas)
      val message: Option[JsValue] = Some("{\"firstName\":\"John\"}".parseJson)

      val caught = intercept[IllegalArgumentException] {
        api.validate("predictor-message", message)
      }

      caught.getMessage() shouldEqual "object has missing required properties ([\"lastName\"])"
    }
    "not pass with two problem" in {
      val metadata = MetadataMock.simpleMockedMetadata()
      val params = "params"
      val actor1 = TestProbe()
      val actors = Map[String, ActorRef]("actor1" -> actor1.ref)
      val messageSchemaFile = getClass.getResource("/engine_home/predictor-message.schema").getPath
      val schemas = Map[String, String]("predictor-message"-> Source.fromFile(messageSchemaFile).mkString)

      val api = new GenericAPI(system, metadata, params, actors, null, schemas)
      val message: Option[JsValue] = Some("{\"xxx\":\"John\"}".parseJson)

      val caught = intercept[IllegalArgumentException] {
        api.validate("predictor-message", message)
      }

      caught.getMessage() shouldEqual "object has missing required properties ([\"firstName\",\"lastName\"])"
    }
  }
}