/*
 * Copyright [2017] [B2W Digital]
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
package org.marvin.executor.actions

import akka.Done
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.marvin.executor.actions.BatchAction.{BatchExecute, BatchHealthCheck, BatchReload}
import org.marvin.executor.proxies.EngineProxy.{ExecuteBatch, HealthCheck, Reload}
import org.marvin.manager.ArtifactSaver.{SaveToLocal, SaveToRemote}
import org.marvin.model.EngineMetadata
import org.marvin.testutil.MetadataMock
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class BatchActionTest extends TestKit(
  ActorSystem("BatchActionTest", ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "batch action actor" must {

    "send Done message" in {

      val mockedProxy = TestProbe()
      val mockedSaver = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()

      val batchAction = system.actorOf(Props(new MockedBatchAction("acquisitor", metadata, mockedProxy.ref, mockedSaver.ref)))

      batchAction ! Done
      expectNoMsg()
      EventFilter.info("Work Done!")
    }

    "send BatchExecute message" in {

      val mockedProxy = TestProbe()
      val mockedSaver = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()

      val batchAction = system.actorOf(Props(new MockedBatchAction("acquisitor", metadata, mockedProxy.ref, mockedSaver.ref)))

      val protocol = "fake_protocol"
      val params = "fakeParams"

      batchAction ! BatchExecute(protocol, params)

      mockedProxy.expectMsg(ExecuteBatch(protocol, params))
      mockedProxy.reply(Done)

      mockedSaver.expectMsg(SaveToRemote("initial_dataset", protocol))
      mockedSaver.reply(Done)

      expectNoMsg()
    }

    "send BatchReload message with single protocol" in {

      val mockedProxy = TestProbe()
      val mockedSaver = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()

      val batchAction = system.actorOf(Props(new MockedBatchAction("tpreparator", metadata, mockedProxy.ref, mockedSaver.ref)))

      val protocol = "acquisitor_12345protocol"

      batchAction ! BatchReload(protocol)

      mockedSaver.expectMsg(SaveToLocal("initial_dataset", protocol))
      mockedSaver.reply(Done)

      mockedProxy.expectMsg(Reload(protocol))
      mockedProxy.reply(Done)

      expectNoMsg()
    }

    "send BatchReload message with multiple protocol" in {

      val mockedProxy = TestProbe()
      val mockedSaver = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()

      val batchAction = system.actorOf(Props(new MockedBatchAction("evaluator", metadata, mockedProxy.ref, mockedSaver.ref)))

      val protocol = "tpreparator_12345protocol,trainer_12345protocol"

      batchAction ! BatchReload(protocol)

      mockedSaver.expectMsg(SaveToLocal("dataset", "tpreparator_12345protocol"))
      mockedSaver.reply(Done)

      mockedSaver.expectMsg(SaveToLocal("model", "trainer_12345protocol"))
      mockedSaver.reply(Done)

      mockedProxy.expectMsg(Reload(protocol))
      mockedProxy.reply(Done)

      expectNoMsg()
    }

    "send BatchHealthCheck message with OK" in {

      val mockedProxy = TestProbe()
      val mockedSaver = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()

      val batchAction = system.actorOf(Props(new MockedBatchAction("evaluator", metadata, mockedProxy.ref, mockedSaver.ref)))

      batchAction ! BatchHealthCheck

      mockedProxy.expectMsg(HealthCheck)
      mockedProxy.reply("OK")

      expectMsg("OK")
    }

    "send BatchHealthCheck message with NOK" in {

      val mockedProxy = TestProbe()
      val mockedSaver = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()

      val batchAction = system.actorOf(Props(new MockedBatchAction("evaluator", metadata, mockedProxy.ref, mockedSaver.ref)))

      batchAction ! BatchHealthCheck

      mockedProxy.expectMsg(HealthCheck)
      mockedProxy.reply("NOK")

      expectMsg("NOK")
    }

  }

  class MockedBatchAction (actionName: String,
                           metadata: EngineMetadata,
                           _batchActionProxy: ActorRef,
                           _artifactSaver: ActorRef
                          ) extends BatchAction (actionName, metadata){

    override def preStart() = {
      engineActionMetadata = metadata.actionsMap(actionName)
      artifactsToLoad = engineActionMetadata.artifactsToLoad.mkString(",")
      batchActionProxy = _batchActionProxy
      artifactSaver = _artifactSaver
    }
  }
}