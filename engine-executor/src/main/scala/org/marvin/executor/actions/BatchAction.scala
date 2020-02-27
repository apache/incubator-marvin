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

import java.time.LocalDateTime
import java.util.NoSuchElementException

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import org.apache.marvin.artifact.manager.ArtifactSaver
import org.apache.marvin.artifact.manager.ArtifactSaver.{GetArtifact, SaveToLocal, SaveToRemote}
import org.apache.marvin.exception.MarvinEExecutorException
import org.apache.marvin.executor.actions.BatchAction.{BatchExecute, BatchExecutionStatus, BatchHealthCheck, BatchReload, BatchMetrics}
import org.apache.marvin.executor.proxies.BatchActionProxy
import org.apache.marvin.executor.proxies.EngineProxy.{ExecuteBatch, HealthCheck, Reload}
import org.apache.marvin.model._
import org.apache.marvin.util.{JsonUtil, LocalCache, ProtocolUtil}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object BatchAction {
  case class BatchExecute(protocol: String, params: String)
  case class BatchReload(protocol: String)
  case class BatchHealthCheck()
  case class BatchExecutionStatus(protocol: String)
  case class BatchMetrics(protocol: String)
}

class BatchAction(actionName: String, metadata: EngineMetadata) extends Actor with ActorLogging{
  var batchActionProxy: ActorRef = _
  var artifactSaver: ActorRef = _
  var engineActionMetadata: EngineActionMetadata = _
  var artifactsToLoad: String = _
  var cache: LocalCache[BatchExecution] = _
  implicit val ec = context.dispatcher

  override def preStart() = {
    engineActionMetadata = metadata.actionsMap(actionName)
    artifactsToLoad = engineActionMetadata.artifactsToLoad.mkString(",")
    batchActionProxy = context.actorOf(Props(new BatchActionProxy(engineActionMetadata)), name = "batchActionProxy")
    artifactSaver = context.actorOf(ArtifactSaver.build(metadata), name = "artifactSaver")
    cache = new LocalCache[BatchExecution](maximumSize = 10000L, defaultTTL = 30.days)
  }

  override def receive  = {
    case BatchExecute(protocol, params) =>
      implicit val futureTimeout = Timeout(metadata.batchActionTimeout milliseconds)

      log.info(s"Starting to process execute to $actionName. Protocol: [$protocol] and params: [$params].")

      cache.save(protocol, new BatchExecution(actionName, protocol, LocalDateTime.now, Working))

      (batchActionProxy ? ExecuteBatch(protocol, params)).onComplete {
        case Success(response) =>
          log.info(s"Execute to $actionName completed [$response] ! Protocol: [$protocol]")

          val futures:ListBuffer[Future[Any]] = ListBuffer[Future[Any]]()
          for(artifactName <- engineActionMetadata.artifactsToPersist) {
            futures += (artifactSaver ? SaveToRemote(artifactName, protocol))
          }

          Future.sequence(futures).onComplete {
            case Success(response) =>
              log.info(s"Save to remote to $actionName completed [$response] ! Protocol: [$protocol]")
              cache.save(protocol, new BatchExecution(actionName, protocol, LocalDateTime.now, Finished))

            case Failure(failure) =>
              cache.save(protocol, new BatchExecution(actionName, protocol, LocalDateTime.now, Failed))
              throw failure
          }

        case Failure(failure) =>
          cache.save(protocol, new BatchExecution(actionName, protocol, LocalDateTime.now, Failed))
          throw failure
      }

    case BatchReload(protocol) =>
      implicit val futureTimeout = Timeout(metadata.reloadTimeout milliseconds)

      log.info(s"Starting to process reload to $actionName. Protocol: [$protocol].")

      val splitedProtocols = ProtocolUtil.splitProtocol(protocol, metadata)

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

    case BatchMetrics(protocol) =>
      implicit val futureTimeout = Timeout(metadata.metricsTimeout milliseconds)
      log.info(s"Starting to process BatchMetrics for Protocol: [$protocol].")

      val originalSender = sender
      ask(artifactSaver, GetArtifact("metrics", protocol)) pipeTo originalSender

    case BatchHealthCheck =>
      implicit val futureTimeout = Timeout(metadata.healthCheckTimeout milliseconds)
      log.info(s"Starting to process health to $actionName.")

      val originalSender = sender
      ask(batchActionProxy, HealthCheck) pipeTo originalSender

    case BatchExecutionStatus(protocol) =>
      log.info(s"Getting batch execution status to protocol $protocol.")

      try {
        sender ! JsonUtil.toJson(cache.load(protocol).get)

      }catch {
        case _: NoSuchElementException =>
          sender ! akka.actor.Status.Failure(new MarvinEExecutorException(s"Protocol $protocol not found!"))
      }

    case Done =>
      log.info("Work Done!")

    case _ =>
      log.warning(s"Not valid message !!")

  }

}