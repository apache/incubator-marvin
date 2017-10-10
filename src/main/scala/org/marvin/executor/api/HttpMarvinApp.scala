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
package org.marvin.executor.api

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.server.HttpApp

import scala.concurrent._

abstract class HttpMarvinApp extends HttpApp {

  /**
    * Overriding default akka behavior for when the server is started.
    * ATTENTION: This method will cause the code to block "forever". Only an OS signal
    * will finish the application.
    * If you want to add post application exit request logic, use scala.sys.addShutdownHook
    * @param system
    * @param ec
    * @return
    */
  override protected def waitForShutdownSignal(system: ActorSystem)(implicit ec: ExecutionContext): Future[Done] = {
    val promise = Promise[Done]()
    sys.addShutdownHook {
      promise.trySuccess(Done)
    }
    Future {
      blocking {
        while(true) {
          Thread.sleep(10000)
        } //the app will wait forever
      }
    }
    promise.future
  }

}
