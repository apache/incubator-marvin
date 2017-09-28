/**
  * Copyright [2017] [B2W Digital]

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
  */
package org.marvin.executor.api

import java.io.FileNotFoundException
import java.lang.Throwable

import actions.HealthCheckResponse.Status
import akka.actor.{ActorRef, ActorSystem, Props, Terminated}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.{HttpApp, Route}
import akka.pattern.ask
import akka.util.Timeout
import org.marvin.executor.actions.BatchAction.{BatchHealthCheckMessage, BatchMessage}
import org.marvin.executor.actions.OnlineAction.{OnlineHealthCheckMessage, OnlineMessage}
import org.marvin.executor.actions.{BatchAction, OnlineAction}
import org.marvin.manager.ArtifactLoader
import org.marvin.manager.ArtifactLoader.{BatchArtifactLoaderMessage, OnlineArtifactLoaderMessage}
import org.marvin.util.{ConfigurationContext, JsonUtil}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._

import scala.concurrent._
import scala.io.{Source, StdIn}
import scala.concurrent.duration._
import org.marvin.executor.api.exception.EngineExceptionAndRejectionHandler._
import spray.json.DefaultJsonProtocol._
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import org.marvin.executor.api.model.HealthStatus
import org.marvin.model.{EngineMetadata, MarvinEExecutorException}

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object GenericHttpAPI extends HttpMarvinApp {
  var system: ActorSystem = _

  var defaultParams: String = _
  var metadata: EngineMetadata = _

  var onlineActor: ActorRef = _
  var batchActor: ActorRef = _
  var artifactLoaderActor: ActorRef = _
  var onlineActionTimeout: Timeout = _

  var api: GenericHttpAPI = new GenericHttpAPIImpl()

  implicit val httpEngineResponseFormat = jsonFormat1(HttpEngineResponse)
  implicit val httpEngineRequestFormat = jsonFormat2(HttpEngineRequest)
  implicit val healthStatusFormat = jsonFormat2(HealthStatus)


  override def routes: Route =
    handleRejections(marvinEngineRejectionHandler){
      handleExceptions(marvinEngineExceptionHandler){
        post {
          path("predictor") {
            entity(as[HttpEngineRequest]) { request =>
              require(!request.message.isEmpty, "The request payload must contain the attribute 'message'.")
              val response_message = api.onlineRequest(
                actionName = "predictor",
                params = request.params.getOrElse(defaultParams),
                message = request.message.get
              )
              onComplete(api.toHttpEngineResponseFuture(response_message)){ response =>
                response match{
                  case Success(httpEngineResponse) => complete(httpEngineResponse)
                  case Failure(e) => throw e
                }
              }
            }
          } ~
          path("acquisitor") {
            entity(as[HttpEngineRequest]) { request =>
              complete {
                val response_message = api.batchRequest("acquisitor", request.params.getOrElse(defaultParams))
                api.toHttpEngineResponse(response_message)
              }
            }
          } ~
          path("tpreparator") {
            entity(as[HttpEngineRequest]) { request =>
              complete {
                val response_message = api.batchRequest("tpreparator", request.params.getOrElse(defaultParams))
                api.toHttpEngineResponse(response_message)
              }
            }
          } ~
          path("trainer") {
            entity(as[HttpEngineRequest]) { request =>
              complete {
                val response_message = api.batchRequest("trainer", request.params.getOrElse(defaultParams))
                api.toHttpEngineResponse(response_message)
              }
            }
          } ~
          path("evaluator") {
            entity(as[HttpEngineRequest]) { request =>
              complete {
                val response_message = api.batchRequest("evaluator", request.params.getOrElse(defaultParams))
                api.toHttpEngineResponse(response_message)
              }
            }
          }
        } ~
        put {
          path("predictor" / "reload") {
            parameters('protocol) { (protocol) =>
              complete {
                val response_message = api.onlineReloadRequest(actionName = "predictor", protocol=protocol)
                api.toHttpEngineResponse(response_message)
              }
            }
          } ~
          path("tpreparator" / "reload") {
            parameters('protocol) { (protocol) =>
              complete {
                val response_message = api.batchReloadRequest(actionName = "tpreparator", protocol=protocol)
                api.toHttpEngineResponse(response_message)
              }
            }
          } ~
          path("trainer" / "reload") {
            parameters('protocol) { (protocol) =>
              complete {
                val response_message = api.batchReloadRequest(actionName = "trainer", protocol=protocol)
                api.toHttpEngineResponse(response_message)
              }
            }
          } ~
          path("evaluator" / "reload") {
            parameters('protocol) { (protocol) =>
              complete {
                val response_message = api.batchReloadRequest(actionName = "evaluator", protocol=protocol)
                api.toHttpEngineResponse(response_message)
              }
            }
          }
        } ~
        get {
          path("predictor" / "health") {
            onComplete(api.onlineActionHealthCheck("predictor")) { response =>
              matchHealthTry(response)
            }
          } ~
          path("tpreparator" / "health") {
            onComplete(api.batchActionHealthCheck("tpreparator")) { response =>
              matchHealthTry(response)
            }
          } ~
          path("trainer" / "health") {
            onComplete(api.batchActionHealthCheck("trainer")) { response =>
              matchHealthTry(response)
            }
          } ~
          path("evaluator" / "health") {
            onComplete(api.batchActionHealthCheck("evaluator")) { response =>
              matchHealthTry(response)
            }
          }
        }
      }
    }

  def matchHealthTry(response: Try[HealthStatus]) = response match {
    case Success(healthStatus) => {
      if(healthStatus.status.equals("OK"))
        complete(healthStatus)
      else
        complete(HttpResponse(StatusCodes.ServiceUnavailable,
          entity = HttpEntity(ContentTypes.`application/json`,
            healthStatusFormat.write(healthStatus).toString())))
    }
    case Failure(e) => throw e
  }

  def main(args: Array[String]): Unit = {
    val engineFilePath = s"${ConfigurationContext.getStringConfigOrDefault("engineHome", ".")}/engine.metadata"
    val paramsFilePath = s"${ConfigurationContext.getStringConfigOrDefault("engineHome", ".")}/engine.params"

    GenericHttpAPI.system = api.setupSystem(engineFilePath, paramsFilePath)
    val ipAddress = ConfigurationContext.getStringConfigOrDefault("ipAddress", "0.0.0.0")
    val port = ConfigurationContext.getIntConfigOrDefault("port", 8080)

    api.startServer(ipAddress, port, GenericHttpAPI.system)
  }
}

class GenericHttpAPIImpl extends GenericHttpAPI

trait GenericHttpAPI {

  protected def setupSystem(engineFilePath:String, paramsFilePath:String): ActorSystem = {
    val metadata = readJsonIfFileExists[EngineMetadata](engineFilePath)
    GenericHttpAPI.metadata = metadata
    GenericHttpAPI.defaultParams = readJsonIfFileExists[Map[String, String]](paramsFilePath).mkString(",")

    val system = ActorSystem("MarvinExecutorSystem")

    GenericHttpAPI.onlineActor = system.actorOf(Props(new OnlineAction(metadata)), name = "onlineActor")
    GenericHttpAPI.batchActor = system.actorOf(Props(new BatchAction(metadata)), name = "batchActor")
    GenericHttpAPI.artifactLoaderActor = system.actorOf(Props(new ArtifactLoader(metadata)), name = "artifactLoaderActor")

    GenericHttpAPI.onlineActionTimeout = Timeout(metadata.onlineActionTimeout seconds)
    system
  }

  private def readJsonIfFileExists[T: ClassTag](filePath: String): T = {
    Try(JsonUtil.fromJson[T](Source.fromFile(filePath).mkString)) match {
      case Success(json) => json
      case Failure(ex) => {
        ex match {
          case ex: FileNotFoundException => throw new MarvinEExecutorException(s"The file [$filePath] does not exists." +
            s" Check your engine configuration.", ex)
          case _ => throw ex
        }
      }
    }
  }

  protected def startServer(ipAddress: String, port: Int, system: ActorSystem): Unit = {
    scala.sys.addShutdownHook{
      system.terminate()
    }
    GenericHttpAPI.startServer(ipAddress, port, system)
  }

  protected def terminate(): Future[Terminated] = {
    GenericHttpAPI.system.terminate()
  }

  protected def batchRequest(actionName: String, params: String): String = {
    val batchMessage = BatchMessage(actionName=actionName, params=params)
    GenericHttpAPI.batchActor ! batchMessage
    "Working in progress!"
  }

  protected def onlineRequest(actionName: String, params: String, message: String): Future[String] = {
    val onlineMessage = OnlineMessage(actionName=actionName, params=params, message=message)
    implicit val futureTimeout = GenericHttpAPI.onlineActionTimeout
    implicit val ec = GenericHttpAPI.system.dispatcher
    val futureResponse: Future[String] = (GenericHttpAPI.onlineActor ? onlineMessage).mapTo[String]
    futureResponse
  }

  protected def onlineActionHealthCheck(actionName: String): Future[HealthStatus] = {
    val onlineHealthCheck = OnlineHealthCheckMessage(
      actionName = actionName, artifacts = getArtifactsToLoad(actionName).mkString(","))
    implicit val futureTimeout = GenericHttpAPI.onlineActionTimeout
    implicit val ec = GenericHttpAPI.system.dispatcher
    (GenericHttpAPI.onlineActor ? onlineHealthCheck).mapTo[Status] collect asHealthStatus
  }

  protected def batchActionHealthCheck(actionName: String): Future[HealthStatus] = {
    val batchHealthCheck = BatchHealthCheckMessage(
      actionName = actionName, artifacts = getArtifactsToLoad(actionName).mkString(","))
    implicit val futureTimeout = GenericHttpAPI.onlineActionTimeout
    implicit val ec = GenericHttpAPI.system.dispatcher
    (GenericHttpAPI.batchActor ? batchHealthCheck).mapTo[Status] collect asHealthStatus
  }

  private def getArtifactsToLoad(actionName: String): List[String] = {
    GenericHttpAPI.metadata.actions.foreach{case action =>
      if(action.name.equals(actionName)) return action.artifactsToLoad
    }
    throw new IllegalArgumentException(s"$actionName is not a valid action.")
  }

  private def asHealthStatus: PartialFunction[Status, HealthStatus] = new PartialFunction[Status, HealthStatus] {
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

  protected def onlineReloadRequest(actionName: String, protocol: String): String = {
    GenericHttpAPI.artifactLoaderActor ! OnlineArtifactLoaderMessage(actionName, protocol)
    "Let's start!"
  }

  protected def batchReloadRequest(actionName: String, protocol: String): String = {
    GenericHttpAPI.artifactLoaderActor ! BatchArtifactLoaderMessage(actionName, protocol)
    "Let's start!"
  }

  protected def toHttpEngineResponse(message:String): HttpEngineResponse = {
    HttpEngineResponse(result = message)
  }

  protected def toHttpEngineResponseFuture(message:Future[String]): Future[HttpEngineResponse] = {
    implicit val ec: ExecutionContext = GenericHttpAPI.system.dispatcher
    message collect { case response => HttpEngineResponse(result = response)}
  }

}

case class HttpEngineResponse(result: String)
case class HttpEngineRequest(params: Option[String] = Option.empty, message: Option[String] = Option.empty)