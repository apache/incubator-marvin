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

import org.scalatest.{Matchers, WordSpec}

class ProtocolUtilTest extends WordSpec with Matchers {

  val protocolUtil = new ProtocolUtil()

  "generateProtocol" should {

    "generate a protocol with valid format" in {
      val protocol = protocolUtil.generateProtocol("test")
      protocol should startWith("test_")

      val protocolWithoutPrefix = protocol.replace("test_", "")
      val uuidRegex = """[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}""".r

      uuidRegex.findFirstIn(protocolWithoutPrefix) shouldNot be(Option.empty)
    }
  }
}
