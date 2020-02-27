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
package org.apache.marvin.executor.actions

import akka.Done
import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, Status}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import io.grpc.StatusRuntimeException
import org.apache.marvin.artifact.manager.ArtifactSaver
import org.apache.marvin.executor.actions.OnlineAction.{OnlineExecute, OnlineHealthCheck, OnlineReload}
import org.apache.marvin.executor.proxies.EngineProxy.{ExecuteOnline, HealthCheck, Reload}
import org.apache.marvin.executor.proxies.OnlineActionProxy
import org.apache.marvin.artifact.manager.ArtifactSaver.SaveToLocal
import org.apache.marvin.model.{EngineActionMetadata, EngineMetadata}
import org.apache.marvin.util.ProtocolUtil

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object OnlineAction {
  case class OnlineExecute(message: String, params: String)
  case class OnlineReload(protocol: String)
  case class OnlineHealthCheck()
}

class OnlineAction(actionName: String, metadata: EngineMetadata) extends Actor with ActorLogging {
  var onlineActionProxy: ActorRef = _
  var artifactSaver: ActorRef = _
  var engineActionMetadata: EngineActionMetadata = _
  var artifactsToLoad: String = _
  implicit val ec = context.dispatcher

  override def preStart() = {
    engineActionMetadata = metadata.actionsMap(actionName)
    artifactsToLoad = engineActionMetadata.artifactsToLoad.mkString(",")
    onlineActionProxy = context.actorOf(Props(new OnlineActionProxy(engineActionMetadata)), name = "onlineActionProxy")
    artifactSaver = context.actorOf(ArtifactSaver.build(metadata), name = "artifactSaver")
  }

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = metadata.onlineActionTimeout milliseconds) {
      case _: StatusRuntimeException => Restart
      case _: Exception => Escalate
  }

  override def receive  = {
    case OnlineExecute(message, params) =>
      implicit val futureTimeout = Timeout(metadata.onlineActionTimeout milliseconds)

      log.info(s"Starting to process execute to $actionName. Message: [$message] and params: [$params].")

      val originalSender = sender
      ask(onlineActionProxy, ExecuteOnline(message, params)) pipeTo originalSender


    case OnlineReload(protocol) =>
      implicit val futureTimeout = Timeout(metadata.reloadTimeout milliseconds)

      log.info(s"Starting to process reload to $actionName. Protocol: [$protocol].")

      if(protocol == null || protocol.isEmpty){
        onlineActionProxy forward Reload()

      }else{
        val splitedProtocols = ProtocolUtil.splitProtocol(protocol, metadata)

        val futures:ListBuffer[Future[Any]] = ListBuffer[Future[Any]]()
        for(artifactName <- engineActionMetadata.artifactsToLoad) {
          futures += (artifactSaver ? SaveToLocal(artifactName, splitedProtocols(artifactName)))
        }

        val origSender = sender()
        Future.sequence(futures).onComplete{
          case Success(_) => onlineActionProxy.ask(Reload(protocol)) pipeTo origSender
          case Failure(e) => {
            log.error(s"Failure to reload artifacts using protocol $protocol.")
            origSender ! Status.Failure(e)
          }
        }
      }

    case OnlineHealthCheck =>
      implicit val futureTimeout = Timeout(metadata.healthCheckTimeout milliseconds)
      log.info(s"Starting to process health to $actionName.")

      val originalSender = sender
      ask(onlineActionProxy, HealthCheck) pipeTo originalSender

    case Done =>
      log.info("Work Done!")

    case _ =>
      log.warning(s"Not valid message !!")

  }
}
