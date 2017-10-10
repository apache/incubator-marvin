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
package org.marvin.util

import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import spray.json._

import scala.reflect.{ClassTag, _}

object JsonUtil {
  val jacksonMapper = new ObjectMapper()
  jacksonMapper.registerModule(DefaultScalaModule)
  jacksonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def toJson(value: Map[Symbol, Any]): String = {
    toJson(value map { case (k,v) => k.name -> v})
  }

  def toJson(value: Any): String = {
    jacksonMapper.writeValueAsString(value)
  }

  def toMap(jsonString: String): Map[String, Any] = {
    JsonUtil.fromJson[Map[String, List[Map[String, String]]]](jsonString)
  }

  def fromJson[T: ClassTag](jsonString: String): T = {
    jacksonMapper.readValue[T](jsonString, classTag[T].runtimeClass.asInstanceOf[Class[T]])
  }

  def format(jsonString: String): String ={
    return jsonString.parseJson.prettyPrint
  }

  def format(jsonMap: Map[String, Any]): String ={
    return this.format(this.toJson(jsonMap))
  }
}





