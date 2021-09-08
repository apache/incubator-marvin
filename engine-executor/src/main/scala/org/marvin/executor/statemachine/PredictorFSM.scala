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

import akka.actor.{ActorRef, FSM, Props}
import org.apache.marvin.exception.MarvinEExecutorException
import org.apache.marvin.executor.actions.OnlineAction
import org.apache.marvin.executor.actions.OnlineAction._
import org.apache.marvin.executor.proxies.{FailedToReload, Reloaded}
import org.apache.marvin.model.EngineMetadata

import scala.concurrent.duration._

//receive events
final case class Reload(protocol: String = "")

//states
sealed trait State
case object Unavailable extends State
case object Reloading extends State
case object Ready extends State

sealed trait Data
case object NoModel extends Data
final case class ToReload(protocol: String) extends Data
final case class Model(protocol: String) extends Data

class PredictorFSM(var predictorActor: ActorRef, metadata: EngineMetadata) extends FSM[State, Data]{
  def this(metadata: EngineMetadata) = this(null, metadata)

  var reloadStateTimeout: FiniteDuration = metadata.reloadStateTimeout.getOrElse(180000D) milliseconds

  override def preStart() {
    if (predictorActor == null) predictorActor = context.system.actorOf(Props(new OnlineAction("predictor", metadata)), name = "predictorActor")
  }

  startWith(Unavailable, NoModel)

  when(Unavailable) {
    case Event(Reload(protocol), _) => {
      predictorActor ! OnlineReload(protocol = protocol)
      goto(Reloading) using ToReload(protocol)
    }
    case Event(e, s) => {
      log.warning("Engine is unavailable, not possible to perform event {} in state {}/{}", e, stateName, s)
      sender ! akka.actor.Status.Failure(new MarvinEExecutorException(
        "It's not possible to process the request now, the model is unavailable. Perform a reload and try again."))
      stay
    }
  }

  when(Reloading, stateTimeout = reloadStateTimeout) {
    case Event(Reloaded(protocol), _) => {
      goto(Ready) using Model(protocol)
    }
    case Event(FailedToReload(protocol), _) => {
      if(protocol == null || protocol.isEmpty)
        log.warning("No valid model protocol found, " +
          "if this is the first start up of the server without any trained model, please ignore the previous IOError from toolbox!")
      else
        log.error(s"Failed to reload with protocol {$protocol}")
      goto(Unavailable)
    }
    case Event(StateTimeout, _) => {
      log.warning("Reloading state timed out.")
      goto(Unavailable)
    }
    case Event(e, s) => {
      log.warning("Engine is reloading, not possible to perform event {} in state {}/{}", e, stateName, s)
      sender ! akka.actor.Status.Failure(new MarvinEExecutorException(
        "It's not possible to process the request now, the model is being reloaded."))
      stay
    }
  }

  when(Ready) {
    case Event(OnlineExecute(message, params), _) => {
      predictorActor forward OnlineExecute(message, params)
      stay
    }
    case Event(Reload(protocol), _) => {
      predictorActor ! OnlineReload(protocol = protocol)
      goto(Reloading) using ToReload(protocol)
    }
    case Event(OnlineHealthCheck, _) => {
      predictorActor forward OnlineHealthCheck
      stay
    }
  }

  whenUnhandled {
    case Event(e, s ) =>
      log.warning("Received an unknown event {}. The current state/data is {}/{}.", e, stateName, s)
      stay
  }

  onTransition{
    case Ready -> Reloading =>
      log.info("Received a message to reload the model.")
    case Reloading -> Ready =>
      log.info("Reloaded the model with success.")
  }

  initialize()

}
