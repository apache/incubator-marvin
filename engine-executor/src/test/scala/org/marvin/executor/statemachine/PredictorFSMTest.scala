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
package org.apache.marvin.executor.statemachine

import actions.HealthCheckResponse.Status
import actions.OnlineActionResponse
import akka.actor.ActorSystem
import akka.testkit.{EventFilter, ImplicitSender, TestFSMRef, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.apache.marvin.exception.MarvinEExecutorException
import org.apache.marvin.executor.actions.OnlineAction.{OnlineExecute, OnlineHealthCheck, OnlineReload}
import org.apache.marvin.executor.proxies.Reloaded
import org.apache.marvin.fixtures.MetadataMock
import org.scalatest.{Matchers, WordSpecLike}

class PredictorFSMTest extends TestKit(
  ActorSystem("EngineFSMTest", ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with ImplicitSender with WordSpecLike with Matchers {

  "engine finite state machine" should {

    "start with Unavailable" in {
      val probe = TestProbe()
      val fsm = TestFSMRef[State, Data, PredictorFSM](new PredictorFSM(probe.ref, MetadataMock.simpleMockedMetadata()))
      fsm.stateName should be (Unavailable)
      fsm.stateData should be (NoModel)
    }

    "go to Reloading when Unavailable and receive Reload" in {
      val probe = TestProbe()
      val fsm = TestFSMRef[State, Data, PredictorFSM](new PredictorFSM(probe.ref, MetadataMock.simpleMockedMetadata()))
      val testProtocol = "protocol1234"
      fsm ! Reload(testProtocol)

      probe.expectMsg(OnlineReload(testProtocol))
      fsm.stateName should be (Reloading)
      fsm.stateData should be (ToReload(testProtocol))
    }

    "go to Reloading when Unavailable and receive Reload with no protocol" in {
      val probe = TestProbe()
      val fsm = TestFSMRef[State, Data, PredictorFSM](new PredictorFSM(probe.ref, MetadataMock.simpleMockedMetadata()))

      fsm ! Reload()
      probe.expectMsg(OnlineReload(""))
      fsm.stateName should be (Reloading)
      fsm.stateData should be (ToReload(""))
    }

    "stay unavailable and send a failure when unavailable and receive unknown message" in {
      val probe = TestProbe()
      val fsm = TestFSMRef[State, Data, PredictorFSM](new PredictorFSM(probe.ref, MetadataMock.simpleMockedMetadata()))
      fsm ! "any message"
      probe.expectNoMsg()
      fsm.stateName shouldBe Unavailable
      val returnedMessage = expectMsgType[akka.actor.Status.Failure]
      returnedMessage.cause shouldBe a[MarvinEExecutorException]
    }

    "go to Ready when Reloading and receive Reloaded" in {
      val probe = TestProbe()
      val fsm = TestFSMRef[State, Data, PredictorFSM](new PredictorFSM(probe.ref, MetadataMock.simpleMockedMetadata()))
      fsm.setState(Reloading)
      fsm ! Reloaded("protocol123")
      fsm.stateName should be (Ready)
      fsm.stateData should be (Model("protocol123"))
    }

    "receive failure and go to Unavailable when Reloading" in {
      val probe = TestProbe()
      val fsm = TestFSMRef[State, Data, PredictorFSM](new PredictorFSM(probe.ref, MetadataMock.simpleMockedMetadata()))
      fsm.setState(Reloading)
      fsm ! OnlineExecute("test", "test")
      probe.expectNoMsg
      fsm.stateName should be (Unavailable)
      val returnedMessage = expectMsgType[akka.actor.Status.Failure]
      returnedMessage.cause shouldBe a[MarvinEExecutorException]
    }

    "forward the message when Ready" in {
      val probe = TestProbe()
      val fsm = TestFSMRef[State, Data, PredictorFSM](new PredictorFSM(probe.ref, MetadataMock.simpleMockedMetadata()))
      fsm.setState(Ready)
      fsm ! OnlineExecute("test", "testMessage")
      probe.expectMsg(OnlineExecute("test", "testMessage"))
      probe.reply(OnlineActionResponse(message = "testResult"))
      expectMsg(OnlineActionResponse(message = "testResult"))
      fsm.stateName should be (Ready)
    }

    "forward the health message when Ready" in {
      val probe = TestProbe()
      val fsm = TestFSMRef[State, Data, PredictorFSM](new PredictorFSM(probe.ref, MetadataMock.simpleMockedMetadata()))
      fsm.setState(Ready)
      fsm ! OnlineHealthCheck
      probe.expectMsg(OnlineHealthCheck)
      probe.reply(Status.OK)
      expectMsg(Status.OK)
      fsm.stateName should be (Ready)
    }

    "go to Reloading when Ready" in {
      val probe = TestProbe()
      val fsm = TestFSMRef[State, Data, PredictorFSM](new PredictorFSM(probe.ref, MetadataMock.simpleMockedMetadata()))
      fsm.setState(Ready)
      val protocol = "protocol99"
      fsm ! Reload(protocol)
      probe.expectMsg(OnlineReload(protocol))
      fsm.stateName should be (Reloading)
      fsm.stateData should be (ToReload(protocol))
    }

    "go to Reloading when Ready and receive Reload with no protocol" in {
      val probe = TestProbe()
      val fsm = TestFSMRef[State, Data, PredictorFSM](new PredictorFSM(probe.ref, MetadataMock.simpleMockedMetadata()))
      fsm.setState(Ready)
      val protocol = null
      fsm ! Reload(protocol)
      probe.expectMsg(OnlineReload(null))
      fsm.stateName should be (Reloading)
      fsm.stateData should be (ToReload(null))
    }

    "stay in the same state and log a warning when unknown event is sent" in {
      val probe = TestProbe()
      val fsm = TestFSMRef[State, Data, PredictorFSM](new PredictorFSM(probe.ref, MetadataMock.simpleMockedMetadata()))
      fsm.setState(Ready)
      EventFilter.warning(message = "Received an unknown event unhandled message. The current state/data is Ready/NoModel.", occurrences = 1) intercept {
        fsm ! "unhandled message"
      }
      fsm.stateName should be (Ready)
    }
  }
}
