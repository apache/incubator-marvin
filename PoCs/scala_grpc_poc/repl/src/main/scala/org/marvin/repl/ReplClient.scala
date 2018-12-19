package main.scala.org.marvin.repl

import java.util.concurrent.TimeUnit
import java.util.logging.{Level, Logger}

import main.scala.org.marvin.repl.{RplRequest, GreeterGrpc}
import main.scala.org.marvin.repl.GreeterGrpc.GreeterBlockingStub
import main.scala.org.marvin.{StatusRuntimeException, ManagedChannelBuilder, ManagedChannel}

object ReplClient {
  def apply(host: String, port: Int): ReplClient = {
    val channel =
      ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build
    val blockingStub = GreeterGrpc.blockingStub(channel)
    new ReplClient(channel, blockingStub)
  }

  def main(args: Array[String]): Unit = {
    val client = ReplClient("localhost", 50051)
    try {
      val user = args.headOption.getOrElse("world")
      client.greet(user)
    } finally {
      client.shutdown()
    }
  }
}

class ReplClient private (
                                 private val channel: ManagedChannel,
                                 private val blockingStub: GreeterBlockingStub
                               ) {
  private[this] val logger = Logger.getLogger(classOf[ReplClient].getName)

  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }

  def greet(name: String): Unit = {
    logger.info("Will try to greet " + name + " ...")
    val request = RplRequest(name = name)
    try {
      val response = blockingStub.sayRpl(request)
      logger.info("Greeting: " + response.message)
    } catch {
      case e: StatusRuntimeException =>
        logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus)
    }
  }
}
