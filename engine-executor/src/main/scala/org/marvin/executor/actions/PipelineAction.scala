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
import akka.pattern.ask
import akka.util.Timeout
import org.apache.marvin.artifact.manager.ArtifactSaver
import org.apache.marvin.artifact.manager.ArtifactSaver.SaveToRemote
import org.apache.marvin.exception.MarvinEExecutorException
import org.apache.marvin.executor.actions.PipelineAction.{PipelineExecute, PipelineExecutionStatus}
import org.apache.marvin.executor.proxies.BatchActionProxy
import org.apache.marvin.executor.proxies.EngineProxy.{ExecuteBatch, Reload}
import org.apache.marvin.model._
import org.apache.marvin.util.{JsonUtil, LocalCache}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Success

object PipelineAction {
  case class PipelineExecute(protocol:String, params:String)
  case class PipelineExecutionStatus(protocol:String)
}

class PipelineAction(metadata: EngineMetadata) extends Actor with ActorLogging{
  implicit val ec = context.dispatcher

  var artifactSaver: ActorRef = _
  var cache: LocalCache[BatchExecution] = _

  override def preStart() = {
    artifactSaver = context.actorOf(ArtifactSaver.build(metadata), name = "artifactSaver")
    cache = new LocalCache[BatchExecution](maximumSize = 10000L, defaultTTL = 30.days)
  }

  override def receive  = {
    case PipelineExecute(protocol, params) =>
      implicit val futureTimeout = Timeout(metadata.pipelineTimeout milliseconds)

      log.info(s"Starting to process pipeline process with. Protocol: [$protocol] and Params: [$params].")
      cache.save(protocol, new BatchExecution("pipeline", protocol, LocalDateTime.now, Working))

      try{
        for(actionName <- metadata.pipelineActions){
          val engineActionMetadata = metadata.actionsMap(actionName)
          val _actor: ActorRef = context.actorOf(Props(new BatchActionProxy(engineActionMetadata)), name = actionName.concat("Actor"))
          Await.result((_actor ? Reload(protocol)), futureTimeout.duration)
          Await.result((_actor ? ExecuteBatch(protocol, params)), futureTimeout.duration)
          context stop _actor

          val futures:ListBuffer[Future[Done]] = ListBuffer[Future[Done]]()

          for(artifactName <- engineActionMetadata.artifactsToPersist) {
            futures += (artifactSaver ? SaveToRemote(artifactName, protocol)).mapTo[Done]
          }

          if (!futures.isEmpty) Future.sequence(futures).onComplete{
            case Success(response) =>
              log.info(s"All artifacts from [$actionName] were saved with success!! [$response]")
          }
        }
      }catch {
        case e: Exception =>
          cache.save(protocol, new BatchExecution("pipeline", protocol, LocalDateTime.now, Failed))
          throw e
      }

      cache.save(protocol, new BatchExecution("pipeline", protocol, LocalDateTime.now, Finished))

    case PipelineExecutionStatus(protocol) =>
      log.info(s"Getting pipeline execution status to protocol $protocol.")

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