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
package org.marvin.executor.actions

import akka.actor.{Actor, ActorLogging, Props}
import org.marvin.executor.actions.ActionHandler.BatchType
import org.marvin.executor.actions.BatchAction.{BatchHealthCheckMessage, BatchMessage, BatchReloadMessage}
import org.marvin.manager.ArtifactSaver
import org.marvin.manager.ArtifactSaver.SaverMessage
import org.marvin.model.EngineMetadata

object BatchAction {
  case class BatchMessage(actionName:String, params:String, protocol:String)
  case class BatchReloadMessage(actionName: String, artifacts:String, protocol:String)
  case class BatchHealthCheckMessage(actionName: String, artifacts: String)
}

class BatchAction(engineMetadata: EngineMetadata) extends Actor with ActorLogging {
  var actionHandler: ActionHandler = _
  val artifactSaveActor = context.actorOf(Props(new ArtifactSaver(engineMetadata)), name = "artifactSaveActor")

  override def preStart() = {
    log.info(s"${this.getClass().getCanonicalName} actor initialized...")
    this.actionHandler = new ActionHandler(engineMetadata, BatchType)
  }

  def receive = {
    case BatchMessage(actionName, params, protocol) =>
      log.info(s"Sending a message ${params} to $actionName")
      this.actionHandler.send_message(actionName=actionName, params=params)

      log.info(s"Sending a message to SaverMessage [${actionName}]")
      artifactSaveActor ! SaverMessage(actionName=actionName, protocol=protocol)

    case BatchReloadMessage(actionName, artifacts, protocol) =>
      log.info(s"Sending the message to reload the $artifacts of $actionName using protocol $protocol")
      sender ! this.actionHandler.reload(actionName, artifacts, protocol)

    case BatchHealthCheckMessage(actionName, artifacts) =>
      log.debug(s"Sending message to batch health check. Following artifacts included: $artifacts.")
      sender ! this.actionHandler.healthCheck(actionName, artifacts)

    case _ =>
      log.info("Received a bad format message...")
  }
}