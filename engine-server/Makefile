# Copyright [2019] [Apache Software Foundation]
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

.PHONY: help package test clean sonatype-publish sonatype-createfile

SBT_VERSION?=0.13.9

help:
	@echo "    package"
	@echo "        Builds a uber-jar with all engine-executor code and it's dependencies."
	@echo "    test"
	@echo "        Run all marvin engine-executor unit tests."
	@echo "    clean"
	@echo "        Clean the build artifacts."
	@echo "    sonatype-publish"
	@echo "        Publish package to maven central repository."
	@echo "    sonatype-createfile"
	@echo "        Create a new sonatype.sbt file to register repository and credentials."

package:
	sbt assembly

test:
	sbt coverage test coverageReport

clean:
	sbt clean

sonatype-publish:
	sbt publishSigned

sonatype-createfile:
	mkdir -p $(HOME)/.sbt/$(SBT_VERSION)
	echo "credentials += Credentials(\"Sonatype Nexus Repository Manager\",\"oss.sonatype.org\",\"(Sonatype user name)\",\"(Sonatype password)\")" >> $(HOME)/.sbt/$(SBT_VERSION)/sonatype.sbt

