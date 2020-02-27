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
package org.apache.marvin.util

import io.jvm.uuid.UUID
import org.apache.marvin.model.EngineMetadata

import scala.collection.immutable.HashMap

object ProtocolUtil {

  def generateProtocol(actionName:String): String ={
    s"${actionName}_${UUID.randomString}"
  }

  def splitProtocol(protocol: String, metadata: EngineMetadata): HashMap[String, String] = {
    var splitedProtocols = new HashMap[String, String]()

    for (_p <- protocol.split(",")){
      val _action = _p.substring(0, _p.indexOf("_"))

      if (_action != "pipeline") {
        for (_artifact <- metadata.actionsMap(_action).artifactsToPersist) splitedProtocols += (_artifact -> _p)
      }
      else{
        for (_paction <- metadata.pipelineActions) for (_artifact <- metadata.actionsMap(_paction).artifactsToPersist) splitedProtocols += (_artifact -> _p)
      }
    }

    splitedProtocols
  }

}
