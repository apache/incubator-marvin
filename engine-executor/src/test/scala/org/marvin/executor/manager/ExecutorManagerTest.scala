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
package org.apache.marvin.executor.manager

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.apache.marvin.executor.api.GenericAPIFunctions
import org.apache.marvin.executor.manager.ExecutorManager.{GetMetadata, StopActor}
import org.apache.marvin.fixtures.MetadataMock
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class ExecutorManagerTest extends TestKit(
  ActorSystem("ExecutorManagerTest", ConfigFactory.parseString("""akka.loggers = ["akka.testkit.TestEventListener"]""")))
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll with MockFactory {

  implicit val timeout = Timeout(1000 milliseconds).duration

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  "send StopActor message" must {

    "with a valid actorName" in {

      val api = mock[GenericAPIFunctions]
      val actorName = "test"
      val managedActor = system.actorOf(Props(new MyActor()), actorName)

      val executorManager = TestActorRef(new ExecutorManager(api))

      (api.manageableActors _).expects().twice().returns(Map[String, ActorRef](actorName -> managedActor))

      watch(managedActor)

      val future = (executorManager ? StopActor(actorName))(timeout)

      future.value.get shouldBe Success(Success)

      expectTerminated(managedActor)

    }

    "with an invalid actionName" in {

      val api = mock[GenericAPIFunctions]
      val executorManager = TestActorRef(new ExecutorManager(api))

      (api.manageableActors _).expects().once().returns(Map[String, ActorRef]())

      val future = (executorManager ? StopActor("teste2"))(timeout)

      future.value.get shouldBe Success(Failure)

    }
  }

  "send GetMetadata message" must {

    "with success" in {
      val api = mock[GenericAPIFunctions]
      val metadata = MetadataMock.simpleMockedMetadata()

      (api.getMetadata _).expects().once().returns(metadata)

      val executorManager = TestActorRef(new ExecutorManager(api))

      val future = ((executorManager ? GetMetadata)(timeout))

      future.value.get shouldBe Success(metadata)

    }
  }

  class MyActor extends Actor with ActorLogging {
    override def receive  = {
      case _ =>
        log.info("Message Received!")
    }
  }
}