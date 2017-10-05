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

import java.util.concurrent.Executors

import io.grpc.ManagedChannelBuilder
import org.marvin.executor.actions.ActionHandler.{BatchType, OnlineType}
import actions.BatchActionHandlerGrpc.BatchActionHandlerBlockingStub
import actions.HealthCheckResponse.Status
import actions.OnlineActionHandlerGrpc.OnlineActionHandlerBlockingStub
import actions._
import org.marvin.model.{EngineActionMetadata, EngineMetadata}

import scala.collection.mutable
import scala.concurrent.ExecutionContext

object ActionHandler {
  sealed abstract class ActionTypes(val name:String) {
    override def toString:String = name
  }
  case object BatchType extends ActionTypes(name="batch")
  case object OnlineType extends ActionTypes(name="online")
}

class ActionHandler(metadata:EngineMetadata, actionType:ActionHandler.ActionTypes){
  val clients = mutable.Map[String, Any]()

  for (action:EngineActionMetadata <- metadata.actions) {
    if (actionType.toString == action.actionType) {
      println(s"Creating a channel to connect with a ${metadata} ${action.actionType} engine ${action.host}:${action.port} action [${action.name}]")
      val channel = ManagedChannelBuilder.forAddress(action.host, action.port).usePlaintext(true).build

      actionType match {
        case BatchType =>
          clients(action.name) = BatchActionHandlerGrpc.blockingStub(channel)
        case OnlineType =>
          clients(action.name) = OnlineActionHandlerGrpc.blockingStub(channel)
      }

      println(s"Connection with ${action.host}:${action.port} prepared!")
    }
  }

  def send_message(actionName: String, params: String): String ={
    return send_message(actionName=actionName, params=params, message=null)
  }

  def send_message(actionName: String, params: String, message: String): String ={
    this.actionType match {
      case OnlineType =>
        val request = OnlineActionRequest(message=message, params=params)
        this.clients(actionName).asInstanceOf[OnlineActionHandlerBlockingStub].RemoteExecute(request).message

      case BatchType =>
        val request = BatchActionRequest(params=params)
        this.clients(actionName).asInstanceOf[BatchActionHandlerBlockingStub].RemoteExecute(request).message

      case _ =>
        throw new UnsupportedOperationException(s"The action type ${this.actionType.toString} is not implemented.")
    }
  }

  def reload(actionName: String, artifacts:String, protocol:String): String ={
    val request = ReloadRequest(artifacts=artifacts, protocol=protocol)

    this.actionType match {
      case OnlineType =>
        this.clients(actionName).asInstanceOf[OnlineActionHandlerBlockingStub].RemoteReload(request).message

      case BatchType =>
        this.clients(actionName).asInstanceOf[BatchActionHandlerBlockingStub].RemoteReload(request).message

      case _ =>
        throw new UnsupportedOperationException(s"The action type ${this.actionType.toString} is not implemented.")
    }
  }

  def healthCheck(actionName: String, artifacts: String): Status = {
    val request = HealthCheckRequest(artifacts = artifacts)

    this.actionType match {
      case OnlineType =>
        this.clients(actionName).asInstanceOf[OnlineActionHandlerBlockingStub].HealthCheck(request).status

      case BatchType =>
        this.clients(actionName).asInstanceOf[BatchActionHandlerBlockingStub].HealthCheck(request).status

      case _ =>
        throw new UnsupportedOperationException(s"The action type ${this.actionType.toString} is not implemented.")

    }
  }
}