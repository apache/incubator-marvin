package main.scala.org.marvin.repl

import java.util.logging.Logger

import io.grpc.{Server, ServerBuilder}
import io.grpc.helloworld.{GreeterGrpc, HelloRequest, HelloReply}

import scala.concurrent.{ExecutionContext, Future}

object ReplServer{
  private val logger = Logger.getLogger(classOf[ReplServer].getName)

  def main(args: Array[String]): Unit = {
    val server = new ReplServer(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 50051
}

class ReplServer(executionContext: ExecutionContext) { self =>
  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder
      .forPort(ReplServer.port)
      .addService(GreeterGrpc.bindService(new ReplService, executionContext))
      .build
      .start
    ReplServer.logger.info(
      "Server started, listening on " + ReplServer.port)
    sys.addShutdownHook {
      System.err.println(
        "*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class ReplService extends GreeterGrpc.Greeter {
    override def sayHello(req: HelloRequest) = {
      val reply = HelloReply(message = "Hello " + req.name)
      Future.successful(reply)
    }
  }

}

