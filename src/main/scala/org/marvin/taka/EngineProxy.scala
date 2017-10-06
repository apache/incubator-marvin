package org.marvin.taka

import akka.actor.{Actor, ActorLogging}
import org.marvin.model.{EngineActionMetadata}

object ActionHandler {
  case class ExecuteBatch(protocol:String, params:String)
  case class ExecuteOnline(message:String, params:String)
  case class Reload(protocol:String)
  case class HealthCheck()
}

abstract class ActionHandler(metadata: EngineActionMetadata) extends Actor with ActorLogging {
  var artifacts: String = _
}










