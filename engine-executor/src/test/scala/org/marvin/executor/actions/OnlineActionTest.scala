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
package org.apache.marvin.executor.actions

import akka.Done
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.apache.marvin.artifact.manager.ArtifactSaver.SaveToLocal
import org.apache.marvin.executor.actions.OnlineAction.{OnlineExecute, OnlineHealthCheck, OnlineReload}
import org.apache.marvin.executor.proxies.EngineProxy.{ExecuteOnline, HealthCheck, Reload}
import org.apache.marvin.executor.proxies.Reloaded
import org.apache.marvin.fixtures.MetadataMock
import org.apache.marvin.model.EngineMetadata
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class OnlineActionTest extends TestKit(
  ActorSystem("OnlineActionTest", ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "online action actor" must {

    "send Done message" in {

      val mockedProxy = TestProbe()
      val mockedSaver = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()

      val onlineAction = system.actorOf(Props(new MockedOnlineAction("predictor", metadata, mockedProxy.ref, mockedSaver.ref)))

      onlineAction ! Done
      expectNoMsg()
      EventFilter.info("Work Done!")
    }

    "send OnlineExecute message" in {

      val mockedProxy = TestProbe()
      val mockedSaver = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()

      val onlineAction = system.actorOf(Props(new MockedOnlineAction("predictor", metadata, mockedProxy.ref, mockedSaver.ref)))

      val protocol = "fake_protocol"
      val params = "fakeParams"

      onlineAction ! OnlineExecute(protocol, params)

      mockedProxy.expectMsg(ExecuteOnline(protocol, params))
      mockedProxy.reply("response")

      expectMsg("response")
    }

    "send OnlineReload message with single protocol" in {

      val mockedProxy = TestProbe()
      val mockedSaver = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()

      val onlineAction = system.actorOf(Props(new MockedOnlineAction("predictor", metadata, mockedProxy.ref, mockedSaver.ref)))

      val protocol = "trainer_12345protocol"

      onlineAction ! OnlineReload(protocol)

      mockedSaver.expectMsg(SaveToLocal("model", protocol))
      mockedSaver.reply(Done)

      //wait for one message to be returned for at least 3 seconds
      receiveOne(3 seconds)
      mockedProxy.expectMsg(Reload(protocol))
      mockedProxy.reply(Reloaded(protocol))
    }

    "send OnlineHealthCheck message with OK" in {

      val mockedProxy = TestProbe()
      val mockedSaver = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()

      val onlineAction = system.actorOf(Props(new MockedOnlineAction("predictor", metadata, mockedProxy.ref, mockedSaver.ref)))

      onlineAction ! OnlineHealthCheck

      mockedProxy.expectMsg(HealthCheck)
      mockedProxy.reply("OK")

      expectMsg("OK")
    }

    "send OnlineHealthCheck message with NOK" in {

      val mockedProxy = TestProbe()
      val mockedSaver = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()

      val onlineAction = system.actorOf(Props(new MockedOnlineAction("predictor", metadata, mockedProxy.ref, mockedSaver.ref)))

      onlineAction ! OnlineHealthCheck

      mockedProxy.expectMsg(HealthCheck)
      mockedProxy.reply("NOK")

      expectMsg("NOK")
    }

  }

  class MockedOnlineAction (actionName: String,
                           metadata: EngineMetadata,
                           _onlineActionProxy: ActorRef,
                           _artifactSaver: ActorRef
                          ) extends OnlineAction (actionName, metadata){

    override def preStart() = {
      engineActionMetadata = metadata.actionsMap(actionName)
      artifactsToLoad = engineActionMetadata.artifactsToLoad.mkString(",")
      onlineActionProxy = _onlineActionProxy
      artifactSaver = _artifactSaver
    }
  }
}