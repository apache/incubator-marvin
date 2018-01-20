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
package org.marvin.executor.manager

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.marvin.executor.manager.ExecutorManager.{GetMetadata, StopActor}
import org.marvin.testutil.MetadataMock
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.util.Success

class ExecutorManagerTest extends TestKit(
  ActorSystem("ExecutorManagerTest", ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "send StopActor message" must {

    "with a valid actionName" in {

      val metadata = MetadataMock.simpleMockedMetadata()

      val managedActor = system.actorOf(Props(new MockedManagedActor()), "testActor")
      val managedActorRefs = Map[String, ActorRef]("test" -> managedActor)

      val executorManager = system.actorOf(Props(new ExecutorManager(metadata, managedActorRefs)))

      watch(managedActor)

      executorManager ! StopActor(actionName="test")

      expectMsg(Success)

      expectTerminated(managedActor)
    }

    "with an invalid actionName" in {

      val metadata = MetadataMock.simpleMockedMetadata()

      val managedActorRefs = Map[String, ActorRef]()

      val executorManager = system.actorOf(Props(new ExecutorManager(metadata, managedActorRefs)))

      executorManager ! StopActor(actionName="test")

      expectNoMsg()

    }
  }

  "send GetMetadata message" must {

    "with success" in {

      val metadata = MetadataMock.simpleMockedMetadata()

      val executorManager = system.actorOf(Props(new ExecutorManager(metadata, Map[String, ActorRef]())))

      executorManager ! GetMetadata

      expectMsg(metadata)

    }
  }

  class MockedManagedActor extends Actor with ActorLogging {
    override def receive  = {
      case _ =>
        log.info("Message Received!")
    }
  }
}