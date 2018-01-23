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
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.typesafe.config.ConfigFactory
import org.marvin.executor.manager.ExecutorManager.{GetMetadata, StopActor}
import org.marvin.fixtures.MetadataMock
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.util.{Failure, Success}

class ExecutorManagerTest extends TestKit(
  ActorSystem("ExecutorManagerTest", ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {

  }

  "send StopActor message" must {

    "with a valid actorName" in {

      val metadata = MetadataMock.simpleMockedMetadata()
      val actorName = "test"
      val managedActor = system.actorOf(Props(new MockedManagedActor()), actorName)
      val managedActorRefs = Map[String, ActorRef](actorName -> managedActor)

      val executorManager = TestActorRef(new ExecutorManager(metadata, managedActorRefs))

      watch(managedActor)

      executorManager ! StopActor(actorName)

      expectMsg(Success)

      expectTerminated(managedActor)

    }

    "with an invalid actionName" in {

      val metadata = MetadataMock.simpleMockedMetadata()

      val managedActorRefs = Map[String, ActorRef]()

      val executorManager = TestActorRef(new ExecutorManager(metadata, managedActorRefs))

      executorManager ! StopActor(actorName="test2")

      expectMsg(Failure)

    }
  }

  "send GetMetadata message" must {

    "with success" in {

      val metadata = MetadataMock.simpleMockedMetadata()

      val executorManager = TestActorRef(new ExecutorManager(metadata, Map[String, ActorRef]()))

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