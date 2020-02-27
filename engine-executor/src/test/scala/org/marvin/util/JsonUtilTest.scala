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

import java.io.File

import com.github.fge.jsonschema.core.exceptions.ProcessingException
import org.apache.marvin.model.EngineMetadata
import org.scalatest.{Matchers, WordSpec}

import scala.io.Source


class JsonUtilTest extends WordSpec with Matchers {

  "A Metadata validation" should {
    "return Unit if metadataToValidate is valid" in {
      val testFilePath = new File(getClass.getClassLoader.getResource("metadataToValidate.json").getPath)
      assert { JsonUtil.validateJson[EngineMetadata](Source.fromFile(testFilePath).mkString) == () }
    }
    "throw Exception if metadataToValidateWithError is invalid" in {
      assertThrows[ProcessingException] {
        val errorTestFilePath = new File(getClass.getClassLoader.getResource("metadataToValidateWithError.json").getPath)
        JsonUtil.validateJson[EngineMetadata](Source.fromFile(errorTestFilePath).mkString)
      }
    }
    "throw Exception if metadataToValidateWithRefError is invalid" in {
      assertThrows[ProcessingException] {
        val errorTestFilePath = new File(getClass.getClassLoader.getResource("metadataToValidateWithRefError.json").getPath)
        JsonUtil.validateJson[EngineMetadata](Source.fromFile(errorTestFilePath).mkString)
      }
    }
  }

  "A JsonUtil.fromJson method" should {
    "return Engine name (iris_species)" in {
      val testFilePath = new File(getClass.getClassLoader.getResource("metadataToValidate.json").getPath)
      assert { JsonUtil.fromJson[EngineMetadata](Source.fromFile(testFilePath).mkString).toString == "iris_species"}
    }
  }

  "Schema files in main resource and test resource" should {
    "synchronize" in {
      val testSchema = Source.fromResource("EngineMetadataSchema.json").getLines().filterNot(line => line.trim.startsWith("\"$ref\":")).toList
      val mainSchema = Source.fromFile("src/main/resources/EngineMetadataSchema.json").getLines().filterNot(line => line.trim.startsWith("\"$ref\":")).toList
      assert { testSchema === mainSchema }

      val testActionSchema = Source.fromResource("EngineActionSchema.json").getLines().toList
      val mainActionSchema = Source.fromFile("src/main/resources/EngineActionSchema.json").getLines().toList
      assert { testActionSchema === mainActionSchema }
    }
  }

}
