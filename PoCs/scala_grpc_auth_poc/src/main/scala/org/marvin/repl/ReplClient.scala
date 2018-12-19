package org.marvin.repl

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import io.grpc.netty.{GrpcSslContexts, NettyChannelBuilder}
import io.grpc.{ManagedChannel, StatusRuntimeException}
import main.scala.org.marvin.repl.ToolboxGrpc.ToolboxBlockingStub
import main.scala.org.marvin.repl.{CommandRequest, ToolboxGrpc}

object ReplClient {
  def apply(host: String, port: Int): ReplClient = {
    val channel: ManagedChannel = NettyChannelBuilder
      .forAddress(host, port)
      .sslContext(GrpcSslContexts.forClient().trustManager(new java.io.File("src/main/resources/serverCC.crt")).build)
      //.usePlaintext(true)
      .build
    val blockingStub = ToolboxGrpc.blockingStub(channel)
    new ReplClient(channel, blockingStub)
  }

  def main(args: Array[String]): Unit = {
    val client = ReplClient("localhost", 50051)
    try {
      val user = args.headOption.getOrElse("engine-dryrun")
      client.sendCMD(user)
    } finally {
      client.shutdown()
    }
  }
}

class ReplClient private(private val channel: ManagedChannel, private val blockingStub: ToolboxBlockingStub) {
  private[this] val logger = Logger.getLogger(classOf[ReplClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def sendCMD(name: String): Unit = {
    logger.info("Sending cmd: " + name + " to toolbox")
    val request = CommandRequest(cmd = name)
    try {
      val response = blockingStub.toolboxControl(request)
      logger.info("Greeting: " + response.logInfo)
    } catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
  }
}
