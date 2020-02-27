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
package org.apache.marvin.executor.proxies

import akka.actor.{Actor, ActorLogging}
import org.apache.marvin.model.EngineActionMetadata

object EngineProxy {
  case class ExecuteBatch(protocol:String, params:String)
  case class ExecuteOnline(message:String, params:String)
  case class Reload(protocol:String = "")
  case class HealthCheck()
}

abstract class EngineProxy(metadata: EngineActionMetadata) extends Actor with ActorLogging {
  var artifacts: String = _
}










