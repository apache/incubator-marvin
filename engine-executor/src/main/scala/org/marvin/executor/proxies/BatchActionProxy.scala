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

import actions.BatchActionHandlerGrpc.BatchActionHandlerBlockingClient
import actions.{BatchActionHandlerGrpc, BatchActionRequest, HealthCheckRequest, ReloadRequest}
import akka.Done
import io.grpc.ManagedChannelBuilder
import org.apache.marvin.executor.proxies.EngineProxy.{ExecuteBatch, HealthCheck, Reload}
import org.apache.marvin.model.EngineActionMetadata

class BatchActionProxy(metadata: EngineActionMetadata) extends EngineProxy (metadata)  {
  var engineClient:BatchActionHandlerBlockingClient = _

  override def preStart() = {
    log.info(s"${this.getClass().getCanonicalName} actor initialized...")
    val channel = ManagedChannelBuilder.forAddress(metadata.host, metadata.port).usePlaintext(true).build
    artifacts = metadata.artifactsToLoad.mkString(",")
    engineClient = BatchActionHandlerGrpc.blockingStub(channel)
  }

  override def receive = {
    case ExecuteBatch(protocol, params) =>
      log.info(s"Start the execute remote procedure to ${metadata.name}. Protocol [$protocol]")
      val message = engineClient.RemoteExecute(BatchActionRequest(params=params)).message
      log.info(s"Execute remote procedure to ${metadata.name} Done with [${message}]. Protocol [$protocol]")
      sender ! Done

    case HealthCheck =>
      log.info(s"Start the health check remote procedure to ${metadata.name}.")
      val status = engineClient.HealthCheck(HealthCheckRequest(artifacts=artifacts)).status
      log.info(s"Health check remote procedure to ${metadata.name} Done with [${status}].")
      sender ! status

    case Reload(protocol) =>
      log.info(s"Start the reload remote procedure to ${metadata.name}. Protocol [$protocol].")
      val message = engineClient.RemoteReload(ReloadRequest(artifacts=artifacts, protocol=protocol)).message
      log.info(s"Reload remote procedure to ${metadata.name} Done with [${message}]. Protocol [$protocol].")
      sender ! Done

    case _ =>
      log.warning(s"Not valid message !!")
  }
}