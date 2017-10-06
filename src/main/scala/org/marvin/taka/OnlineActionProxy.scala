package org.marvin.taka

import actions.OnlineActionHandlerGrpc.OnlineActionHandlerBlockingStub
import actions._
import io.grpc.ManagedChannelBuilder
import org.marvin.model.EngineActionMetadata
import org.marvin.taka.ActionHandler.{ExecuteOnline, HealthCheck, Reload}

class OnlineActionProxy(metadata: EngineActionMetadata) extends ActionHandler (metadata)  {
  var engineClient:OnlineActionHandlerBlockingStub = _

  override def preStart() = {
    log.info(s"${this.getClass().getCanonicalName} actor initialized...")
    val channel = ManagedChannelBuilder.forAddress(metadata.host, metadata.port).usePlaintext(true).build
    artifacts = metadata.artifactsToLoad.mkString(",")
    engineClient = OnlineActionHandlerGrpc.blockingStub(channel)
  }

  override def receive = {
    case ExecuteOnline(requestMessage, params) =>
      log.info(s"Start the execute remote procedure to ${metadata.name}.")
      val response_message = engineClient.RemoteExecute(OnlineActionRequest(message=requestMessage, params=params)).message
      log.info(s"Execute remote procedure to ${metadata.name} Done with [${response_message}].")
      sender ! response_message

    case HealthCheck =>
      log.info(s"Start the health check remote procedure to ${metadata.name}.")
      val status = engineClient.HealthCheck(HealthCheckRequest(artifacts=artifacts)).status
      log.info(s"Health check remote procedure to ${metadata.name} Done with [${status}].")
      sender ! status

    case Reload(protocol) =>
      log.info(s"Start the reload remote procedure to ${metadata.name}.")
      val message = engineClient.RemoteReload(ReloadRequest(artifacts=artifacts, protocol=protocol)).message
      log.info(s"Reload remote procedure to ${metadata.name} Done with [${message}].")

    case _ =>
      log.warning(s"Not valid message !!")
  }
}