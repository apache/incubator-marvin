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

import java.time.LocalDateTime

import akka.Done
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.apache.marvin.artifact.manager.ArtifactSaver.{SaveToLocal, SaveToRemote}
import org.apache.marvin.exception.MarvinEExecutorException
import org.apache.marvin.executor.actions.BatchAction.{BatchExecute, BatchExecutionStatus, BatchHealthCheck, BatchReload}
import org.apache.marvin.executor.proxies.EngineProxy.{ExecuteBatch, HealthCheck, Reload}
import org.apache.marvin.fixtures.MetadataMock
import org.apache.marvin.model._
import org.apache.marvin.util.{JsonUtil, LocalCache}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class BatchActionTest extends TestKit(
  ActorSystem("BatchActionTest", ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with MockFactory {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "batch action actor" must {

    "send Done message" in {

      val mockedProxy = TestProbe()
      val mockedSaver = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()
      val mockedCache = mock[LocalCache[BatchExecution]]

      val batchAction = system.actorOf(Props(new MockedBatchAction("acquisitor", metadata, mockedProxy.ref, mockedSaver.ref, mockedCache)))

      batchAction ! Done
      expectNoMsg()
      EventFilter.info("Work Done!")
    }

    "send BatchExecutionStatus message with valid protocol" in {

      val metadata = MetadataMock.simpleMockedMetadata()
      val mockedCache = mock[LocalCache[BatchExecution]]

      val batchAction = system.actorOf(Props(new MockedBatchAction("acquisitor", metadata, null, null, mockedCache)))

      val protocol = "fake_protocol"
      val batchExecution = new BatchExecution("acquisitor", protocol, LocalDateTime.now, Working)

      (mockedCache.load _).expects(protocol).returning(Option(batchExecution)).once()

      batchAction ! BatchExecutionStatus(protocol)

      expectMsg(JsonUtil.toJson(batchExecution))
    }

    "send BatchExecutionStatus message with invalid protocol" in {

      val metadata = MetadataMock.simpleMockedMetadata()
      val mockedCache = new LocalCache[BatchExecution](10, 1.minute)

      val batchAction = system.actorOf(Props(new MockedBatchAction("acquisitor", metadata, null, null, mockedCache)))

      val protocol = "fake_protocol"

      batchAction ! BatchExecutionStatus(protocol)

      val returnedMessage = expectMsgType[akka.actor.Status.Failure]
      returnedMessage.cause shouldBe a[MarvinEExecutorException]
    }

    "send BatchExecute message" in {

      val mockedProxy = TestProbe()
      val mockedSaver = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()
      val mockedCache = mock[LocalCache[BatchExecution]]

      val batchAction = system.actorOf(Props(new MockedBatchAction("acquisitor", metadata, mockedProxy.ref, mockedSaver.ref, mockedCache)))

      val protocol = "fake_protocol"
      val params = "fakeParams"

      (mockedCache.save(_: String, _: BatchExecution)).expects(protocol, new BatchExecution("acquisitor", protocol, LocalDateTime.now, Working)).once()
      (mockedCache.save(_: String, _: BatchExecution)).expects(protocol, new BatchExecution("acquisitor", protocol, LocalDateTime.now, Finished)).once()

      batchAction ! BatchExecute(protocol, params)

      mockedProxy.expectMsg(ExecuteBatch(protocol, params))
      mockedProxy.reply(Done)

      mockedSaver.expectMsg(SaveToRemote("initial_dataset", protocol))
      mockedSaver.reply(Done)

      expectNoMsg()
    }

    "send BatchExecute message to get exception from proxy [execute msg]" in {

      val mockedProxy = TestActorRef(new Actor {
        def receive = {
          case ExecuteBatch(protocol, params) => throw new Exception("boom")
        }
      })

      val metadata = MetadataMock.simpleMockedMetadata()
      val mockedCache = mock[LocalCache[BatchExecution]]

      val batchAction = system.actorOf(Props(new MockedBatchAction("acquisitor", metadata, mockedProxy, null, mockedCache)))

      val protocol = "fake_protocol"
      val params = "fakeParams"

      (mockedCache.save(_: String, _: BatchExecution)).expects(protocol, new BatchExecution("acquisitor", protocol, LocalDateTime.now, Working)).once()
      (mockedCache.save(_: String, _: BatchExecution)).expects(protocol, new BatchExecution("acquisitor", protocol, LocalDateTime.now, Failed)).once()

      batchAction ! BatchExecute(protocol, params)

      expectNoMsg()
    }

    "send BatchExecute message to get exception from saver" in {

      val mockedSaver = TestActorRef(new Actor {
        def receive = {
          case SaveToRemote(artifactName, protocol) => throw new Exception("boom")
        }
      })

      val mockedProxy = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()
      val mockedCache = mock[LocalCache[BatchExecution]]

      val batchAction = system.actorOf(Props(new MockedBatchAction("acquisitor", metadata, mockedProxy.ref, mockedSaver, mockedCache)))

      val protocol = "fake_protocol"
      val params = "fakeParams"

      (mockedCache.save(_: String, _: BatchExecution)).expects(protocol, new BatchExecution("acquisitor", protocol, LocalDateTime.now, Working)).once()
      (mockedCache.save(_: String, _: BatchExecution)).expects(protocol, new BatchExecution("acquisitor", protocol, LocalDateTime.now, Failed)).once()

      batchAction ! BatchExecute(protocol, params)

      mockedProxy.expectMsg(ExecuteBatch(protocol, params))
      mockedProxy.reply(Done)

      expectNoMsg()
    }

    "send BatchReload message with single protocol" in {

      val mockedProxy = TestProbe()
      val mockedSaver = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()
      val mockedCache = mock[LocalCache[BatchExecution]]

      val batchAction = system.actorOf(Props(new MockedBatchAction("tpreparator", metadata, mockedProxy.ref, mockedSaver.ref, mockedCache)))

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
      val mockedCache = mock[LocalCache[BatchExecution]]

      val batchAction = system.actorOf(Props(new MockedBatchAction("evaluator", metadata, mockedProxy.ref, mockedSaver.ref, mockedCache)))

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
      val mockedCache = mock[LocalCache[BatchExecution]]

      val batchAction = system.actorOf(Props(new MockedBatchAction("evaluator", metadata, mockedProxy.ref, mockedSaver.ref, mockedCache)))

      batchAction ! BatchHealthCheck

      mockedProxy.expectMsg(HealthCheck)
      mockedProxy.reply("OK")

      expectMsg("OK")
    }

    "send BatchHealthCheck message with NOK" in {

      val mockedProxy = TestProbe()
      val mockedSaver = TestProbe()
      val metadata = MetadataMock.simpleMockedMetadata()
      val mockedCache = mock[LocalCache[BatchExecution]]

      val batchAction = system.actorOf(Props(new MockedBatchAction("evaluator", metadata, mockedProxy.ref, mockedSaver.ref, mockedCache)))

      batchAction ! BatchHealthCheck

      mockedProxy.expectMsg(HealthCheck)
      mockedProxy.reply("NOK")

      expectMsg("NOK")
    }

  }

  class MockedBatchAction (actionName: String,
                           metadata: EngineMetadata,
                           _batchActionProxy: ActorRef,
                           _artifactSaver: ActorRef,
                          _cache: LocalCache[BatchExecution]
                          ) extends BatchAction (actionName, metadata){

    override def preStart() = {
      engineActionMetadata = metadata.actionsMap(actionName)
      artifactsToLoad = engineActionMetadata.artifactsToLoad.mkString(",")
      batchActionProxy = _batchActionProxy
      artifactSaver = _artifactSaver
      cache = _cache
    }
  }
}