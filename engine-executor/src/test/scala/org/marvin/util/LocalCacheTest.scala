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

import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class LocalCacheTest extends WordSpec with Matchers {

  "load" should {

    "get a value from local cache by key" in {
      val cache = new LocalCache[String](maximumSize = 2L, defaultTTL = 1 minute)
      cache.save("key1", "value1")
      cache.save("key2", "value2")
      cache.load("key1").mkString should be("value1")
      cache.load("key2").mkString should be("value2")
    }
  }

  "save" should {

    "put a value into local cache" in {
      val cache = new LocalCache[String](maximumSize = 2L, defaultTTL = 1 minute)
      cache.save("key", "value")
      cache.load("key").mkString should be("value")
    }

    "update key in local cache" in {
      val cache = new LocalCache[String](maximumSize = 2L, defaultTTL = 1 minute)
      cache.save("key", "value1")
      cache.load("key").mkString should be("value1")

      cache.save("key", "value2")
      cache.load("key").mkString should be("value2")
    }

    "store only maximum entries" in {
      val cache = new LocalCache[String](maximumSize = 2L, defaultTTL = 1 minute)
      cache.save("key1", "value1")
      cache.save("key2", "value2")
      cache.save("key3", "value3")

      assert(cache.load("key1") == None)
      cache.load("key2").mkString should be("value2")
      cache.load("key3").mkString should be("value3")

      cache.save("key4", "value4")
      assert(cache.load("key2") == None)
      cache.load("key3").mkString should be("value3")
      cache.load("key4").mkString should be("value4")
    }
  }
}
