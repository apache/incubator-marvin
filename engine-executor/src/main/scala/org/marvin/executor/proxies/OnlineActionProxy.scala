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
package org.apache.marvin.executor.proxies

import actions.OnlineActionHandlerGrpc.{OnlineActionHandler, OnlineActionHandlerBlockingClient, OnlineActionHandlerBlockingStub, OnlineActionHandlerStub}
import actions._
import akka.pattern.pipe
import io.grpc.ManagedChannelBuilder
import org.apache.marvin.executor.proxies.EngineProxy.{ExecuteOnline, HealthCheck, Reload}
import org.apache.marvin.model.EngineActionMetadata

//Reload messages
final case class Reloaded(protocol: String)
final case class FailedToReload(protocol: String = "")

class OnlineActionProxy(metadata: EngineActionMetadata) extends EngineProxy (metadata)  {
  var engineAsyncClient:OnlineActionHandler = _
  var engineClient:OnlineActionHandlerBlockingClient = _

  implicit val ec = context.dispatcher

  override def preStart() = {
    log.info(s"${this.getClass().getCanonicalName} actor initialized...")
    val channel = ManagedChannelBuilder.forAddress(metadata.host, metadata.port).usePlaintext(true).build
    artifacts = metadata.artifactsToLoad.mkString(",")
    engineAsyncClient = OnlineActionHandlerGrpc.stub(channel)
    engineClient = OnlineActionHandlerGrpc.blockingStub(channel)
  }

  override def receive = {
    case ExecuteOnline(requestMessage, params) =>
      log.info(s"Start the execute remote procedure to ${metadata.name}.")
      val responseFuture = engineAsyncClient.RemoteExecute(OnlineActionRequest(message=requestMessage, params=params))
      responseFuture.collect{case response => response.message} pipeTo sender

    case HealthCheck =>
      log.info(s"Start the health check remote procedure to ${metadata.name}.")
      val statusFuture = engineAsyncClient.HealthCheck(HealthCheckRequest(artifacts=artifacts))
      statusFuture.collect{case response => response.status} pipeTo sender

    case Reload(protocol) =>
      log.info(s"Start the reload remote procedure to ${metadata.name}. Protocol [$protocol]")
      try{
        val message = engineClient.RemoteReload(ReloadRequest(artifacts=artifacts, protocol=protocol)).message
        log.info(s"Reload remote procedure to ${metadata.name} Done with [${message}]. Protocol [$protocol]")
        sender ! Reloaded(protocol)
      } catch {
        case _ : Exception => sender ! FailedToReload(protocol)
      }

    case _ =>
      log.warning(s"Not valid message !!")
  }
}