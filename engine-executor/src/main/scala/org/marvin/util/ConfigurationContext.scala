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

import com.typesafe.config.ConfigFactory

import scala.util.Try

object ConfigurationContext {

  val config = ConfigFactory.load()
  val configPrefix = "marvinConfig"

  def getStringConfigOrDefault(configKey: String, default: String): String = {
    Try(config.getString(s"${configPrefix}.${configKey}")).getOrElse(default)
  }

  def getIntConfigOrDefault(configKey: String, default: Int): Int = {
    Try(config.getInt(s"${configPrefix}.${configKey}")).getOrElse(default)
  }

  def getBooleanConfigOrDefault(configKey: String, default: Boolean): Boolean = {
    Try(config.getBoolean(s"${configPrefix}.${configKey}")).getOrElse(default)
  }

}
