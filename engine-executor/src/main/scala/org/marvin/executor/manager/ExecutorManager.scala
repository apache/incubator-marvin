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
package org.apache.marvin.executor.manager

import akka.actor.{Actor, ActorLogging}
import akka.util.Timeout
import org.apache.marvin.executor.api.{GenericAPI, GenericAPIFunctions}
import org.apache.marvin.executor.manager.ExecutorManager.{GetMetadata, StopActor}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object ExecutorManager {
  case class StopActor(actorName: String)
  case class GetMetadata()
}

class ExecutorManager(api: GenericAPIFunctions) extends Actor with ActorLogging {
  implicit val ec = ExecutionContext.global
  implicit val futureTimeout = Timeout(30 seconds)

  override def preStart() = {
    log.info(s"Executor Manager enabled and starting!!!")
    log.info(s"Executor Manager path ${self.path}")
  }

  override def receive  = {
    case StopActor(actorName) =>

      if(api.manageableActors.contains(actorName)){
        val actorRef = api.manageableActors(actorName)

        log.info(s"Actor ${actorRef.path} found. Trying to stop selected actor..")

        context.stop(actorRef)

        log.info(s"Actor ${actorRef.path} successfully stopped!")

        sender ! Success

      }else{
        log.info(s"Actor related with the key ${actorName} is not a valid manageable actor.")
        sender ! Failure
      }

    case GetMetadata =>
      log.info(s"Getting Metadata object from engine ...")
      sender ! api.getMetadata

  }
}