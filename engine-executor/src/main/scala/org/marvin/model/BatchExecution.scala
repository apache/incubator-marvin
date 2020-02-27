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
package org.apache.marvin.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed abstract class ExecutionStatus(val name: String, val code: Int) {
  override def toString: String = name
}

case object Working extends ExecutionStatus(name="working", code=0)
case object Finished extends ExecutionStatus(name="finished", code=1)
case object Failed extends ExecutionStatus(name="failed", code=(-1))

case class BatchExecution(actionName: String, protocol: String, datetime: LocalDateTime, status: ExecutionStatus){
  override def toString: String = s"$actionName | $protocol | $status"
  override def equals(obj: scala.Any): Boolean = this.toString == obj.toString
  val formattedDatetime: String = datetime.format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
}
