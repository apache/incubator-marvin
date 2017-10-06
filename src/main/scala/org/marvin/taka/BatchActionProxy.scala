package org.marvin.taka

import actions.BatchActionHandlerGrpc.BatchActionHandlerBlockingStub
import actions.{BatchActionHandlerGrpc, BatchActionRequest, HealthCheckRequest, ReloadRequest}
import akka.Done
import io.grpc.ManagedChannelBuilder
import org.marvin.model.EngineActionMetadata
import org.marvin.taka.ActionHandler.{ExecuteBatch, HealthCheck, Reload}

class BatchActionProxy(metadata: EngineActionMetadata) extends ActionHandler (metadata)  {
  var engineClient:BatchActionHandlerBlockingStub = _

  override def preStart() = {
    log.info(s"${this.getClass().getCanonicalName} actor initialized...")
    val channel = ManagedChannelBuilder.forAddress(metadata.host, metadata.port).usePlaintext(true).build
    artifacts = metadata.artifactsToLoad.mkString(",")
    engineClient = BatchActionHandlerGrpc.blockingStub(channel)
  }

  override def receive = {
    case ExecuteBatch(protocol, params) =>
      log.info(s"Start the execute remote procedure to ${metadata.name}.")
      val message = engineClient.RemoteExecute(BatchActionRequest(params=params)).message
      log.info(s"Execute remote procedure to ${metadata.name} Done with [${message}].")
      sender ! Done

    case HealthCheck =>
      log.info(s"Start the health check remote procedure to ${metadata.name}.")
      val status = engineClient.HealthCheck(HealthCheckRequest(artifacts=artifacts)).status
      log.info(s"Health check remote procedure to ${metadata.name} Done with [${status}].")
      sender ! status

    case Reload(protocol) =>
      log.info(s"Start the reload remote procedure to ${metadata.name}.")
      val message = engineClient.RemoteReload(ReloadRequest(artifacts=artifacts, protocol=protocol)).message
      log.info(s"Reload remote procedure to ${metadata.name} Done with [${message}].")
      sender ! Done

    case _ =>
      log.warning(s"Not valid message !!")
  }
}