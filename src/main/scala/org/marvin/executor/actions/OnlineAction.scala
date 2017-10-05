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

import akka.Done
import akka.actor.{Actor, ActorLogging}
import org.marvin.executor.actions.ActionHandler.OnlineType
import org.marvin.executor.actions.OnlineAction.{OnlineHealthCheckMessage, OnlineMessage, OnlineReloadMessage}
import org.marvin.model.EngineMetadata

object OnlineAction {
  case class OnlineMessage(actionName:String, message:String, params:String)
  case class OnlineReloadMessage(actionName: String, artifacts:String, protocol:String)
  case class OnlineHealthCheckMessage(actionName: String, artifacts: String)
}

class OnlineAction(engineMetadata: EngineMetadata) extends Actor with ActorLogging {
  var actionHandler: ActionHandler = _

  override def preStart() = {
    log.info(s"${this.getClass().getCanonicalName} actor initializing...")
    this.actionHandler = new ActionHandler(engineMetadata, OnlineType)
  }

  def receive = {
    case OnlineMessage(actionName, message, params) =>
      log.info(s"Sending the message ${message} and params ${params} to $actionName")
      sender ! this.actionHandler.send_message(actionName=actionName, params=params, message=message)

    case OnlineReloadMessage(actionName, artifacts, protocol) =>
      log.info(s"Sending the message to reload the $artifacts of $actionName using protocol $protocol")
      this.actionHandler.reload(actionName=actionName, artifacts=artifacts, protocol=protocol)
      sender ! Done

    case OnlineHealthCheckMessage(actionName, artifacts) =>
      log.info("Sending online health check request.")
      sender ! this.actionHandler.healthCheck(actionName = actionName, artifacts = artifacts)

    case _ =>
      log.info("Received a bad format message...")
  }
}