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

import com.fasterxml.jackson.databind.{DeserializationFeature, JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.github.fge.jsonschema.core.exceptions.ProcessingException
import com.github.fge.jsonschema.core.report.ProcessingMessage
import com.github.fge.jsonschema.main.JsonSchemaFactory
import grizzled.slf4j.Logging
import org.json4s.jackson.JsonMethods.{asJsonNode, parse}
import spray.json._

import scala.io.Source
import scala.reflect.{ClassTag, _}

object JsonUtil extends Logging {
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

  def fromJson[T: ClassTag](jsonString: String, validate: Boolean = false): T = {

    if (validate) validateJson[T](jsonString)

    jacksonMapper.readValue[T](jsonString, classTag[T].runtimeClass.asInstanceOf[Class[T]])
  }

  def validateJson[T: ClassTag](jsonString: String): Unit = {
    val className = classTag[T].runtimeClass.getSimpleName
    val schemaName = className.toString + "Schema.json"

    var jsonSchema: String = null

    try{
      jsonSchema = Source.fromResource(schemaName).mkString
      validateJson(jsonString, jsonSchema)
    } catch {
      case e: NullPointerException => info(s"File ${schemaName} not found, check your schema file")
        throw e
    }
  }

  /**
    * Validates a json against a schema file (Draft-4) informed.
    * @param jsonString - The json string to be validated.
    * @param jsonSchema - The content of the schema file as a string.
    */
  def validateJson(jsonString: String, jsonSchema: String): Unit = {

    val schema: JsonNode = asJsonNode(parse(jsonSchema))
    val jsonToValidate: JsonNode = asJsonNode(parse(jsonString))
    val validator = JsonSchemaFactory.byDefault().getValidator

    val processingReport = validator.validate(schema, jsonToValidate)
    if (!processingReport.isSuccess) {
      val sb = new StringBuilder()
      processingReport.forEach {
        message: ProcessingMessage => {
          warn(message.asJson())
          sb.append(message.getMessage)
        }
      }
      throw new ProcessingException(sb.toString)
    }
  }

  def format(jsonString: String): String ={
    return jsonString.parseJson.prettyPrint
  }

  def format(jsonMap: Map[String, Any]): String ={
    return this.format(this.toJson(jsonMap))
  }
}





