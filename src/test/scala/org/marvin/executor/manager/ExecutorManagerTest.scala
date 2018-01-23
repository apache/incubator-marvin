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
import org.marvin.executor.api.GenericAPIFunctions
import org.marvin.executor.manager.ExecutorManager.{GetMetadata, StopActor}
import org.marvin.fixtures.MetadataMock
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.util.{Failure, Success}

class ExecutorManagerTest extends TestKit(
  ActorSystem("ExecutorManagerTest", ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with MockFactory {

  "send StopActor message" must {

    "with a valid actorName" in {

      val api = mock[GenericAPIFunctions]
      val actorName = "test"
      val mySystem = ActorSystem()
      val managedActor = mySystem.actorOf(Props(new MyActor()), actorName)

      val executorManager = TestActorRef(new ExecutorManager(api))

      (api.manageableActors _).expects().twice().returns(Map[String, ActorRef](actorName -> managedActor))

      watch(managedActor)

      executorManager ! StopActor(actorName)

      expectMsg(Success)

      expectTerminated(managedActor)

      TestKit.shutdownActorSystem(mySystem)

    }

    "with an invalid actionName" in {

      val api = mock[GenericAPIFunctions]
      val mySystem = ActorSystem()
      val executorManager = TestActorRef(new ExecutorManager(api))

      (api.manageableActors _).expects().once().returns(Map[String, ActorRef]())

      executorManager ! StopActor(actorName="test2")

      expectMsg(Failure)

      TestKit.shutdownActorSystem(mySystem)

    }
  }

  "send GetMetadata message" must {

    "with success" in {
      val mySystem = ActorSystem()
      val api = mock[GenericAPIFunctions]
      val metadata = MetadataMock.simpleMockedMetadata()

      (api.getMetadata _).expects().once().returns(metadata)

      val executorManager = TestActorRef(new ExecutorManager(api))

      executorManager ! GetMetadata

      expectMsg(metadata)

      TestKit.shutdownActorSystem(mySystem)

    }
  }

  class MyActor extends Actor with ActorLogging {
    override def receive  = {
      case _ =>
        log.info("Message Received!")
    }
  }
}