package org.apache.marvin.executor.proxies

import actions.BatchActionHandlerGrpc.BatchActionHandlerBlockingClient
import actions._
import akka.Done
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.apache.marvin.executor.proxies.EngineProxy.{ExecuteBatch, HealthCheck, Reload}
import org.apache.marvin.fixtures.MetadataMock
import org.apache.marvin.model.EngineActionMetadata
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}


class BatchActionProxyTest extends TestKit(
  ActorSystem("BatchActionProxyTest", ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with MockFactory {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "batch action proxy" should {
    "receive ExecuteBatch message" in {
      val metadata = MetadataMock.simpleMockedEngineActionMetadata("batch")
      val artifacts = metadata.artifactsToLoad.mkString(",")
      val client = mock[BatchActionHandlerBlockingClient]
      val actor = system.actorOf(Props(new BatchActionProxyMock(metadata, client, artifacts)))

      val protocol = "protocol"
      val params = "{}"
      val message = "check"

      (client.RemoteExecute _ ).expects(BatchActionRequest(params)).once().returns(BatchActionResponse(message))

      actor ! ExecuteBatch(protocol, params)

      expectMsg(Done)

    }

    "receive OK HealthCheck message" in {
      val metadata = MetadataMock.simpleMockedEngineActionMetadata("batch")
      val artifacts = metadata.artifactsToLoad.mkString(",")
      val client = mock[BatchActionHandlerBlockingClient]
      val actor = system.actorOf(Props(new BatchActionProxyMock(metadata, client, artifacts)))

      val status = HealthCheckResponse.Status.OK

      (client.HealthCheck _ ).expects(HealthCheckRequest(artifacts)).once().returns(HealthCheckResponse(status))

      actor ! HealthCheck

      expectMsg(status)
    }

    "receive NOK HealthCheck message" in {
      val metadata = MetadataMock.simpleMockedEngineActionMetadata("batch")
      val artifacts = metadata.artifactsToLoad.mkString(",")
      val client = mock[BatchActionHandlerBlockingClient]
      val actor = system.actorOf(Props(new BatchActionProxyMock(metadata, client, artifacts)))

      val status = HealthCheckResponse.Status.NOK

      (client.HealthCheck _ ).expects(HealthCheckRequest(artifacts)).once().returns(HealthCheckResponse(status))

      actor ! HealthCheck

      expectMsg(status)
    }

    "receive Reload message" in {
      val metadata = MetadataMock.simpleMockedEngineActionMetadata("batch")
      val artifacts = metadata.artifactsToLoad.mkString(",")
      val client = mock[BatchActionHandlerBlockingClient]
      val actor = system.actorOf(Props(new BatchActionProxyMock(metadata, client, artifacts)))

      val protocol = "protocol"

      (client.RemoteReload _ ).expects(ReloadRequest(protocol, artifacts)).once().returns(ReloadResponse("Done"))

      actor ! Reload(protocol)

      expectMsg(Done)
    }

    "call preStart method wth success" in {
      val metadata = MetadataMock.simpleMockedEngineActionMetadata("batch")
      try{
        system.actorOf(Props(new BatchActionProxy(metadata)))
        assert(true)
      }catch {
        case _ =>
          assert(false)
      }
    }
  }

  class BatchActionProxyMock(metadata: EngineActionMetadata, _engineClient: BatchActionHandlerBlockingClient, _artifacts: String) extends BatchActionProxy(metadata) {
    def _preStart(): Unit = super.preStart()
    override def preStart(): Unit = {
      engineClient = _engineClient
      artifacts = _artifacts
    }
  }
}
