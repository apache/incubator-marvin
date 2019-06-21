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

import java.util.concurrent.Executors

import actions.HealthCheckResponse.Status
import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.MessageDispatcher
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import akka.http.scaladsl.server.{HttpApp, Route, StandardRoute}
import akka.pattern.ask
import akka.util.Timeout
import com.github.fge.jsonschema.core.exceptions.ProcessingException
import org.apache.marvin.executor.actions.BatchAction.{BatchExecute, BatchExecutionStatus, BatchMetrics, BatchHealthCheck, BatchReload}
import org.apache.marvin.executor.actions.OnlineAction.{OnlineExecute, OnlineHealthCheck}
import org.apache.marvin.executor.actions.PipelineAction.{PipelineExecute, PipelineExecutionStatus}
import org.apache.marvin.executor.api.GenericAPI._
import org.apache.marvin.executor.statemachine.Reload
import org.apache.marvin.model.EngineMetadata
import org.apache.marvin.util.{JsonUtil, ProtocolUtil}
import spray.json.{DefaultJsonProtocol, RootJsonFormat, _}

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait GenericAPIFunctions {
  def asHealthStatus: PartialFunction[Status, HealthStatus]
  def matchHealthTry(response: Try[HealthStatus]): StandardRoute
  def batchExecute(actionName: String, params: String): String
  def onlineExecute(actionName: String, params: String, message: String): Future[String]
  def reload(actionName: String, actionType:String, protocol: String): String
  def check(actionName: String, actionType:String): Future[HealthStatus]
  def status(actionName: String, protocol: String): Future[String]
  def pipeline(params: String): String
  def getMetadata:EngineMetadata
  def getSystem:ActorSystem
  def getEngineParams:String
  def manageableActors:Map[String, ActorRef]
  def generateProtocol(actionName: String):String
  def startServer(ipAddress: String, port: Int): Unit
  def waitForShutdownSignal(system: ActorSystem)(implicit ec: ExecutionContext): Future[Done]
  def routes: Route
}

object GenericAPI {
  case class HealthStatus(status: String, additionalMessage: String)
  case class DefaultHttpResponse(result: String)
  case class DefaultOnlineRequest(params: Option[JsValue] = Option.empty, message: Option[JsValue] = Option.empty)
  case class DefaultBatchRequest(params: Option[JsValue] = Option.empty)
}

class GenericAPI(system: ActorSystem,
                 metadata: EngineMetadata,
                 engineParams: String,
                 actors: Map[String, ActorRef],
                 docsFilePath: String,
                 schemas: Map[String, String]) extends HttpApp with SprayJsonSupport with DefaultJsonProtocol with GenericAPIFunctions {

  val onlineActionTimeout = Timeout(metadata.onlineActionTimeout milliseconds)
  val healthCheckTimeout = Timeout(metadata.healthCheckTimeout milliseconds)
  val batchActionTimeout = Timeout(metadata.batchActionTimeout milliseconds)
  val reloadTimeout = Timeout(metadata.reloadTimeout milliseconds)
  val pipelineTimeout = Timeout(metadata.pipelineTimeout milliseconds)
  val metricsTimeout = Timeout(metadata.metricsTimeout milliseconds)

  val log: LoggingAdapter = Logging.getLogger(system, this)

  implicit val defaultHttpResponseFormat: RootJsonFormat[DefaultHttpResponse] = jsonFormat1(DefaultHttpResponse)
  implicit val defaultOnlineRequestFormat: RootJsonFormat[DefaultOnlineRequest] = jsonFormat2(DefaultOnlineRequest)
  implicit val defaultBatchRequestFormat: RootJsonFormat[DefaultBatchRequest] = jsonFormat1(DefaultBatchRequest)
  implicit val healthStatusFormat: RootJsonFormat[HealthStatus] = jsonFormat2(HealthStatus)

  def routes: Route = handleRejections(GenericAPIHandlers.rejections){
    handleExceptions(GenericAPIHandlers.exceptions){
      post {
        path("predictor") {
          entity(as[DefaultOnlineRequest]) { request =>
            require(request.message.isDefined, "The request payload must contain the attribute 'message'.")

            validate("predictor-message", request.message)

            val responseFuture = onlineExecute("predictor", request.params.getOrElse(engineParams).toString, request.message.get.toString)

            onComplete(responseFuture) {
              case Success(response) => complete(DefaultHttpResponse(response))
              case Failure(e) =>
                log.info("RECEIVE FAILURE!!! " + e.getMessage + e.getClass)
                failWith(e)
            }
          }
        } ~
        path("acquisitor") {
          entity(as[DefaultBatchRequest]) { request =>
            complete {
              val response = batchExecute("acquisitor", request.params.getOrElse(engineParams).toString)
              DefaultHttpResponse(response)
            }
          }
        } ~
        path("tpreparator") {
          entity(as[DefaultBatchRequest]) { request =>
            complete {
              val response = batchExecute("tpreparator", request.params.getOrElse(engineParams).toString)
              DefaultHttpResponse(response)
            }
          }
        } ~
        path("trainer") {
          entity(as[DefaultBatchRequest]) { request =>
            complete {
              val response = batchExecute("trainer", request.params.getOrElse(engineParams).toString)
              DefaultHttpResponse(response)
            }
          }
        } ~
        path("evaluator") {
          entity(as[DefaultBatchRequest]) { request =>
            complete {
              val response = batchExecute("evaluator", request.params.getOrElse(engineParams).toString)
              DefaultHttpResponse(response)
            }
          }
        } ~
        path("pipeline") {
          entity(as[DefaultBatchRequest]) { request =>
            complete {
              val response = pipeline(request.params.getOrElse(engineParams).toString)
              DefaultHttpResponse(response)
            }
          }
        } ~
        path("feedback") {
          entity(as[DefaultOnlineRequest]) { request =>
            require(request.message.isDefined, "The request payload must contain the attribute 'message'.")

            validate("feedback-message", request.message)

            val responseFuture = onlineExecute("feedback", request.params.getOrElse(engineParams).toString, request.message.get.toString)

            onComplete(responseFuture) {
              case Success(response) => complete(DefaultHttpResponse(response))
              case Failure(e) =>
                log.info("RECEIVE FAILURE!!! " + e.getMessage + e.getClass)
                failWith(e)
            }
          }
        }
      } ~
      put {
        path("predictor" / "reload") {
          parameters('protocol) { (protocol) =>
            complete {
              val response = reload("predictor", "online", protocol=protocol)
              DefaultHttpResponse(response)
            }
          }
        } ~
          path("tpreparator" / "reload") {
            parameters('protocol) { (protocol) =>
              complete {
                val response = reload("tpreparator", "batch", protocol=protocol)
                DefaultHttpResponse(response)
              }
            }
          } ~
          path("trainer" / "reload") {
            parameters('protocol) { (protocol) =>
              complete {
                val response = reload("trainer", "batch", protocol=protocol)
                DefaultHttpResponse(response)
              }
            }
          } ~
          path("evaluator" / "reload") {
            parameters('protocol) { (protocol) =>
              complete {
                val response = reload("evaluator", "batch", protocol=protocol)
                DefaultHttpResponse(response)
              }
            }
          } ~
          path("feedback" / "reload") {
            parameters('protocol) { (protocol) =>
              complete {
                val response = reload("feedback", "online", protocol=protocol)
                DefaultHttpResponse(response)
              }
            }
          }
      } ~
      get {
        pathPrefix("docs"){
          (pathEndOrSingleSlash & redirectToTrailingSlashIfMissing(StatusCodes.TemporaryRedirect)) {
            getFromResource("docs/index.html")
          } ~  {
            path(docsFilePath.split("/").last){
              getFromFile(docsFilePath)
            } ~ {
              getFromResourceDirectory("docs")
            }
          }
        } ~
        path("predictor" / "health") {
          onComplete(check("predictor", "online")) { response =>
            matchHealthTry(response)
          }
        } ~
        path("acquisitor" / "health") {
          onComplete(check("acquisitor", "batch")) { response =>
            matchHealthTry(response)
          }
        } ~
        path("tpreparator" / "health") {
          onComplete(check("tpreparator", "batch")) { response =>
            matchHealthTry(response)
          }
        } ~
        path("trainer" / "health") {
          onComplete(check("trainer", "batch")) { response =>
            matchHealthTry(response)
          }
        } ~
        path("evaluator" / "health") {
          onComplete(check("evaluator", "batch")) { response =>
            matchHealthTry(response)
          }
        } ~
        path("feedback" / "health") {
          onComplete(check("feedback", "online")) { response =>
            matchHealthTry(response)
          }
        } ~
        path("acquisitor" / "status") {
          parameters('protocol) { (protocol) =>
            val responseFuture = status("acquisitor", protocol)

            onComplete(responseFuture) {
              case Success(response) => complete(DefaultHttpResponse(response))
              case Failure(e) =>
                log.info("RECEIVE FAILURE!!! " + e.getMessage + e.getClass)
                failWith(e)
            }
          }
        } ~
        path("tpreparator" / "status") {
          parameters('protocol) { (protocol) =>
            val responseFuture = status("tpreparator", protocol)

            onComplete(responseFuture) {
              case Success(response) => complete(DefaultHttpResponse(response))
              case Failure(e) =>
                log.info("RECEIVE FAILURE!!! " + e.getMessage + e.getClass)
                failWith(e)
            }
          }
        } ~
        path("trainer" / "status") {
          parameters('protocol) { (protocol) =>
            val responseFuture = status("trainer", protocol)

            onComplete(responseFuture) {
              case Success(response) => complete(DefaultHttpResponse(response))
              case Failure(e) =>
                log.info("RECEIVE FAILURE!!! " + e.getMessage + e.getClass)
                failWith(e)
            }
          }
        } ~
        path("evaluator" / "status") {
          parameters('protocol) { (protocol) =>
            val responseFuture = status("evaluator", protocol)

            onComplete(responseFuture) {
              case Success(response) => complete(DefaultHttpResponse(response))
              case Failure(e) =>
                log.info("RECEIVE FAILURE!!! " + e.getMessage + e.getClass)
                failWith(e)
            }
          }
        } ~
        path("evaluator" / "metrics") {
          parameters('protocol) { (protocol) =>
            val responseFuture = metrics(protocol)

            onComplete(responseFuture) {
              case Success(response) => complete(DefaultHttpResponse(response))
              case Failure(e) =>
                log.info("RECEIVE FAILURE!!! " + e.getMessage + e.getClass)
                failWith(e)
            }
          }
        } ~
        path("pipeline" / "status") {
          parameters('protocol) { (protocol) =>
            val responseFuture = status("pipeline", protocol)

            onComplete(responseFuture) {
              case Success(response) => complete(DefaultHttpResponse(response))
              case Failure(e) =>
                log.info("RECEIVE FAILURE!!! " + e.getMessage + e.getClass)
                failWith(e)
            }
          }
        }
      }
    }
  }

  def validate(schemaName: String, target: Option[JsValue]): Unit = {
    if (schemas != null) {
      val schema: String = schemas.getOrElse(schemaName, null)

      if (schema != null && !target.isEmpty) {
        try {
          JsonUtil.validateJson(target.get.prettyPrint, schema)
        } catch {
          case e: ProcessingException => {
            throw new IllegalArgumentException(e.getShortMessage)
          }
          case t: Throwable => {
            throw t
          }
        }
      }
    }
  }

  def asHealthStatus: PartialFunction[Status, HealthStatus] = new PartialFunction[Status, HealthStatus] {
    override def apply(status: Status): HealthStatus = {
      val statusTyped = status.asInstanceOf[Status]
      if(statusTyped.isOk){
        HealthStatus(status = "OK", additionalMessage = "")
      } else {
        HealthStatus(status = "NOK", additionalMessage = "Engine did not returned a healthy status.")
      }
    }
    override def isDefinedAt(status: Status): Boolean = status != null
  }

  def matchHealthTry(response: Try[HealthStatus]): StandardRoute = response match {
    case Success(healthStatus) =>
      if(healthStatus.status.equals("OK"))
        complete(healthStatus)
      else
        complete(HttpResponse(StatusCodes.ServiceUnavailable, entity = HttpEntity(ContentTypes.`application/json`, healthStatusFormat.write(healthStatus).toString())))

    case Failure(e) => throw e
  }

  def batchExecute(actionName: String, params: String): String = {
    log.info(s"Request for $actionName] received.")

    implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
    implicit val futureTimeout: Timeout = batchActionTimeout

    val protocol = generateProtocol(actionName)
    actors(actionName) ! BatchExecute(protocol, params)

    protocol
  }

  def onlineExecute(actionName: String, params: String, message: String): Future[String] = {
    log.info(s"Request for $actionName] received.")

    implicit val ec: MessageDispatcher = system.dispatchers.lookup("marvin-online-dispatcher")
    implicit val futureTimeout: Timeout = onlineActionTimeout

    (actors(actionName) ? OnlineExecute(message, params)).mapTo[String]
  }

  def metrics(protocol: String): Future[String] = {
    log.info(s"Request metrics for protocol $protocol] received.")

    implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
    implicit val futureTimeout: Timeout = onlineActionTimeout

    (actors("evaluator") ? BatchMetrics(protocol)).mapTo[String]
  }

  def reload(actionName: String, actionType:String, protocol: String): String = {
    implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
    implicit val futureTimeout: Timeout = reloadTimeout

    actionType match {
      case "online" =>
        actors(actionName) ! Reload(protocol)

      case "batch" =>
        actors(actionName) ! BatchReload(protocol)
    }

    "Work in progress...Thank you folk!"
  }

  def check(actionName: String, actionType:String): Future[HealthStatus] = {
    implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
    implicit val futureTimeout: Timeout = healthCheckTimeout

    actionType match {
      case "online" =>
        (actors(actionName) ? OnlineHealthCheck).mapTo[Status] collect asHealthStatus

      case "batch" =>
        (actors(actionName) ? BatchHealthCheck).mapTo[Status] collect asHealthStatus
    }
  }

  def status(actionName: String, protocol: String): Future[String] = {
    implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
    implicit val futureTimeout: Timeout = healthCheckTimeout

    actionName match {
      case "pipeline" => (actors(actionName) ? PipelineExecutionStatus(protocol)).mapTo[String]
      case _ => (actors(actionName) ? BatchExecutionStatus(protocol)).mapTo[String]
    }
  }

  def pipeline(params: String): String = {
    log.info(s"Request pipeline process received.")

    implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())
    implicit val futureTimeout: Timeout = pipelineTimeout

    val protocol = generateProtocol("pipeline")
    actors("pipeline") ! PipelineExecute(protocol, params)

    protocol
  }

  def getMetadata:EngineMetadata = metadata

  def getSystem:ActorSystem = system

  def getEngineParams:String = engineParams

  def manageableActors:Map[String, ActorRef] = actors

  def generateProtocol(actionName: String):String = {ProtocolUtil.generateProtocol(actionName)}

  override def waitForShutdownSignal(system: ActorSystem)(implicit ec: ExecutionContext): Future[Done] = {
    val promise = Promise[Done]()

    sys.addShutdownHook {
      promise.trySuccess(Done)
    }

    Future {
      blocking {
        while(true) {
          Thread.sleep(10000)
        } //the app will wait forever
      }
    }

    promise.future
  }

  override def startServer(ipAddress: String, port: Int): Unit = {
    scala.sys.addShutdownHook{system.terminate()}
    super.startServer(ipAddress, port, system)
  }

}