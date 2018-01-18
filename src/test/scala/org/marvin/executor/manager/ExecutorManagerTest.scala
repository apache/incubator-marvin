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

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorPath, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.marvin.executor.manager.ExecutorManager.StopActor
import org.marvin.testutil.MetadataMock
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ExecutorManagerTest extends TestKit(
  ActorSystem("ExecutorManagerTest", ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "executor manager actor" must {

    "send Stop Actor message" in {

      val metadata = MetadataMock.simpleMockedMetadata()

      val managedActor = system.actorOf(Props(new MockedManagedActor()), "testActor")
      val managedActorPaths = Map[String, ActorPath](
        "testActor" -> managedActor.path
      )

      val executorManager = system.actorOf(Props(new ExecutorManager(metadata, managedActorPaths)))

      executorManager ! StopActor(actionName="testActor")
      expectMsg(Done)
      EventFilter.info("Service checked!")
    }
  }

  class MockedManagedActor extends Actor with ActorLogging {

    override def postStop(): Unit = {
      log.info("stoping...")
      super.postStop()
    }

    override def preStart(): Unit = {
      log.info("starting...")
      super.preStart()
    }

    override def receive  = {
      case Done =>
        log.info("Done Message Received!")
        sender ! Done
    }
  }
}