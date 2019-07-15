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

import scalacache.{Cache, put, get, Entry}
import scalacache.guava._
import scalacache.modes.sync._
import com.google.common.cache.CacheBuilder
import scala.concurrent.duration._


class LocalCache[E](maximumSize:Long, defaultTTL: Duration) {
  implicit var cache: Cache[E] = GuavaCache(CacheBuilder.newBuilder().maximumSize(maximumSize).build[String, Entry[E]])

  def load(key: String): Option[E] = {
    get(key).asInstanceOf[Option[E]]
  }

  def save(key: String, value: E): Unit = {
    put(key)(value, ttl = Some(defaultTTL))
  }
}
