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
package org.marvin.executor.manager

import akka.actor.{Actor, ActorLogging, ActorPath}
import akka.util.Timeout
import org.marvin.executor.manager.ExecutorManager.{GetMetadata, StopActor}
import org.marvin.model.EngineMetadata

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ExecutorManager {
  case class StopActor(actionName: String)
  case class GetMetadata()
}

class ExecutorManager(metadata: EngineMetadata, managedActorPaths: Map[String, ActorPath]) extends Actor with ActorLogging {
  implicit val ec = ExecutionContext.global
  implicit val futureTimeout = Timeout(30 seconds)

  override def preStart() = {
    log.info(s"Executor Manager enabled !!!")
    log.info(s"This is the manager path ${self.path}")
  }

  override def receive  = {

    case StopActor(actionName) =>

      val originalSender = sender

      context.actorSelection(managedActorPaths(actionName)).resolveOne(futureTimeout.duration).onComplete {
        case Success(actorRef) =>
          log.info(s"Actor ${actorRef.path} found. Trying to stop selected actor..")
          context.stop(actorRef)

          log.info(s"Actor ${actorRef.path} successfully stopped!")

          originalSender ! "Done"

        case Failure(ex) =>
          ex.printStackTrace()

          originalSender ! "Failure"
      }

    case GetMetadata =>
      sender ! metadata

    case _ : String =>
      log.info("Message receivd")

  }
}