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
package org.apache.marvin.executor.api

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ExceptionHandler, MissingQueryParamRejection, RejectionHandler}
import grizzled.slf4j.Logger
import org.apache.marvin.exception.MarvinEExecutorException
import spray.json.DefaultJsonProtocol._

import scala.concurrent.TimeoutException

case class ErrorResponse(errorMessage: String)

object GenericAPIHandlers {

  lazy val logger = Logger[this.type]

  implicit val errorFormatter = jsonFormat1(ErrorResponse)

  val exceptions: ExceptionHandler =
    ExceptionHandler {
      case ex: IllegalArgumentException => {
        logger.debug("Endpoint thrown illegal argument exception.", ex)

        val error = ErrorResponse(errorMessage = ex.getMessage)
        complete(HttpResponse(StatusCodes.BadRequest, entity = toResponseEntityJson(error)))
      }
      case ex: TimeoutException => {
        logger.debug("Endpoint thrown timeout exception", ex)

        val error = ErrorResponse(errorMessage = "The engine was not able to provide a response within the specified timeout.")
        complete(HttpResponse(StatusCodes.InternalServerError, entity = toResponseEntityJson(error)))
      }
      case ex: MarvinEExecutorException => {
        logger.debug("Endpoint thrown Marvin EExecutor Exception", ex)

        val error = ErrorResponse(errorMessage = ex.getMessage())
        complete(HttpResponse(StatusCodes.ServiceUnavailable, entity = toResponseEntityJson(error)))
      }
      case _ => {
        val error = ErrorResponse(errorMessage = "Unexpected error.")
        complete(HttpResponse(StatusCodes.InternalServerError, entity = toResponseEntityJson(error)))
      }
    }

  val rejections: RejectionHandler =
    RejectionHandler.newBuilder().handle {
      case rj: MissingQueryParamRejection => {
        logger.debug("Missing query parameters.")

        val error = ErrorResponse(errorMessage = s"Missing query parameter. [${rj.parameterName}]")
        complete(HttpResponse(StatusCodes.BadRequest, entity = toResponseEntityJson(error)))
      }
    }.result()

  def toResponseEntityJson(error: ErrorResponse): ResponseEntity = {
    HttpEntity(ContentTypes.`application/json`, errorFormatter.write(error).toString())
  }
}
