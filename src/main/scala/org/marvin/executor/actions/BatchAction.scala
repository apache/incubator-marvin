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
package org.marvin.executor.actions

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import org.marvin.executor.actions.BatchAction.{BatchExecute, BatchHealthCheck, BatchReload}
import org.marvin.executor.proxies.BatchActionProxy
import org.marvin.executor.proxies.EngineProxy.{ExecuteBatch, HealthCheck, Reload}
import org.marvin.manager.ArtifactSaver
import org.marvin.manager.ArtifactSaver.{SaveToLocal, SaveToRemote}
import org.marvin.model.{EngineActionMetadata, EngineMetadata}
import org.marvin.util.ProtocolUtil

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object BatchAction {
  case class BatchExecute(protocol: String, params: String)
  case class BatchReload(protocol:String)
  case class BatchHealthCheck()
}

class BatchAction(actionName: String, metadata: EngineMetadata) extends Actor with ActorLogging{
  var batchActionProxy: ActorRef = _
  var artifactSaver: ActorRef = _
  var engineActionMetadata: EngineActionMetadata = _
  var artifactsToLoad: String = _
  implicit val ec = context.dispatcher
  var protocolUtil = new ProtocolUtil()

  override def preStart() = {
    engineActionMetadata = metadata.actionsMap(actionName)
    artifactsToLoad = engineActionMetadata.artifactsToLoad.mkString(",")
    batchActionProxy = context.actorOf(Props(new BatchActionProxy(engineActionMetadata)), name = "batchActionProxy")
    artifactSaver = context.actorOf(ArtifactSaver.build(metadata), name = "artifactSaver")
  }

  override def receive  = {
    case BatchExecute(protocol, params) =>
      implicit val futureTimeout = Timeout(metadata.batchActionTimeout milliseconds)

      log.info(s"Starting to process execute to $actionName. Protocol: [$protocol] and params: [$params].")

      (batchActionProxy ? ExecuteBatch(protocol, params)).onComplete {
        case Success(response) =>
          log.info(s"Execute to $actionName completed [$response] ! Protocol: [$protocol]")

          for(artifactName <- engineActionMetadata.artifactsToPersist){
            artifactSaver ! SaveToRemote(artifactName, protocol)
          }

        case Failure(failure) =>
          failure.printStackTrace()
      }

    case BatchReload(protocol) =>
      implicit val futureTimeout = Timeout(metadata.reloadTimeout milliseconds)

      log.info(s"Starting to process reload to $actionName. Protocol: [$protocol].")

      val splitedProtocols = protocolUtil.splitProtocol(protocol, metadata)

      val futures:ListBuffer[Future[Any]] = ListBuffer[Future[Any]]()
      for(artifactName <- engineActionMetadata.artifactsToLoad) {
          futures += (artifactSaver ? SaveToLocal(artifactName, splitedProtocols(artifactName)))
      }

      Future.sequence(futures).onComplete {
        case Success(response) =>
          log.info(s"Reload to $actionName completed [$response] ! Protocol: [$protocol]")

          batchActionProxy ! Reload(protocol)

        case Failure(failure) =>
          failure.printStackTrace()
      }

    case BatchHealthCheck =>
      implicit val futureTimeout = Timeout(metadata.healthCheckTimeout milliseconds)
      log.info(s"Starting to process health to $actionName.")

      val originalSender = sender
      ask(batchActionProxy, HealthCheck) pipeTo originalSender

    case Done =>
      log.info("Work Done!")

    case _ =>
      log.warning(s"Not valid message !!")

  }
}