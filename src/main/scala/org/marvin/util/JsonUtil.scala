package org.marvin.util

/**
  * Created by taka on 01/09/17.
  */
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





