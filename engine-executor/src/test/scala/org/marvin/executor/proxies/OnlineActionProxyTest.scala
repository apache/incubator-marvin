package org.apache.marvin.executor.proxies

import actions.OnlineActionHandlerGrpc.{OnlineActionHandler, OnlineActionHandlerBlockingClient}
import actions._
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.util.Timeout
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import org.apache.marvin.executor.proxies.EngineProxy.{ExecuteOnline, HealthCheck, Reload}
import org.apache.marvin.fixtures.MetadataMock
import org.apache.marvin.model.EngineActionMetadata
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Future


class OnlineActionProxyTest extends TestKit(
  ActorSystem("OnlineActionProxyTest", ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with MockFactory {

  implicit val timeout = Timeout(1000 milliseconds).duration
  implicit val ec = system.dispatcher

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "online action proxy" should {
    "receive ExecuteOnline message" in {
      val metadata = MetadataMock.simpleMockedEngineActionMetadata("online")
      val artifacts = metadata.artifactsToLoad.mkString(",")
      val params = ""
      val client = mock[OnlineActionHandlerBlockingClient]
      val asyncClient = mock[OnlineActionHandler]
      val actor = system.actorOf(Props(new OnlineActionProxyMock(metadata, client, asyncClient, artifacts)))

      val inMessage = "ping"
      val outMessage = "pong"

      val response = Future(OnlineActionResponse(outMessage))

      (asyncClient.RemoteExecute _ ).expects(OnlineActionRequest(inMessage, params)).once().returns(response)

      actor ! ExecuteOnline(inMessage, params)

      expectMsg(outMessage)

    }

    "receive OK HealthCheck message" in {
      val metadata = MetadataMock.simpleMockedEngineActionMetadata("online")
      val artifacts = metadata.artifactsToLoad.mkString(",")
      val client = mock[OnlineActionHandlerBlockingClient]
      val asyncClient = mock[OnlineActionHandler]
      val actor = system.actorOf(Props(new OnlineActionProxyMock(metadata, client, asyncClient, artifacts)))

      val status = HealthCheckResponse.Status.OK
      val response = Future(HealthCheckResponse(status))

      (asyncClient.HealthCheck _ ).expects(HealthCheckRequest(artifacts)).once().returns(response)

      actor ! HealthCheck

      expectMsg(status)
    }

    "receive NOK HealthCheck message" in {
      val metadata = MetadataMock.simpleMockedEngineActionMetadata("online")
      val artifacts = metadata.artifactsToLoad.mkString(",")
      val client = mock[OnlineActionHandlerBlockingClient]
      val asyncClient = mock[OnlineActionHandler]
      val actor = system.actorOf(Props(new OnlineActionProxyMock(metadata, client, asyncClient, artifacts)))

      val status = HealthCheckResponse.Status.NOK
      val response = Future(HealthCheckResponse(status))

      (asyncClient.HealthCheck _ ).expects(HealthCheckRequest(artifacts)).once().returns(response)

      actor ! HealthCheck

      expectMsg(status)
    }

    "receive Reload message" in {
      val metadata = MetadataMock.simpleMockedEngineActionMetadata("online")
      val artifacts = metadata.artifactsToLoad.mkString(",")
      val client = mock[OnlineActionHandlerBlockingClient]
      val asyncClient = mock[OnlineActionHandler]
      val actor = system.actorOf(Props(new OnlineActionProxyMock(metadata, client, asyncClient, artifacts)))

      val protocol = "protocol"

      (client.RemoteReload _ ).expects(ReloadRequest(protocol, artifacts)).once().returns(ReloadResponse("Done"))

      actor ! Reload(protocol)

      expectMsg(Reloaded(protocol))
    }

    "receive Reload message and throw exception" in {
      val metadata = MetadataMock.simpleMockedEngineActionMetadata("online")
      val artifacts = metadata.artifactsToLoad.mkString(",")
      val client = mock[OnlineActionHandlerBlockingClient]
      val asyncClient = mock[OnlineActionHandler]
      val actor = system.actorOf(Props(new OnlineActionProxyMock(metadata, client, asyncClient, artifacts)))

      val protocol = "protocol"

      (client.RemoteReload _ ).expects(ReloadRequest(protocol, artifacts)).once().throwing(new Exception())

      actor ! Reload(protocol)

      expectMsg(FailedToReload(protocol))
    }

    "call preStart method wth success" in {
      val metadata = MetadataMock.simpleMockedEngineActionMetadata("online")
      try{
        system.actorOf(Props(new OnlineActionProxy(metadata)))
        assert(true)
      }catch {
        case _ =>
          assert(false)
      }
    }
  }

  class OnlineActionProxyMock(metadata: EngineActionMetadata, _engineClient: OnlineActionHandlerBlockingClient, _engineAsyncClient: OnlineActionHandler, _artifacts: String) extends OnlineActionProxy(metadata) {
    def _preStart(): Unit = super.preStart()
    override def preStart(): Unit = {
      engineClient = _engineClient
      engineAsyncClient = _engineAsyncClient
      artifacts = _artifacts
    }
  }
}
