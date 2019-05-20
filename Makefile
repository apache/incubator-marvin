
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

.PHONY: help docker-base docker-python docker-r toolbox engine-server

help:
	@echo "    docker-base"
	@echo "        Builds the base docker image with common dependecies among languages."
	@echo "    docker-python"
	@echo "        Builds a docker image that can run engines in Python."
	@echo "    docker-r"
	@echo "        Builds a docker image that can run engines in R."
	@echo "    toolbox"
	@echo "        Builds the toolbox and make it available for be included into docker images."
	@echo "    engine-server"
	@echo "        Builds a jar with the engine interpreter server."

docker-base:
	docker build -t marvin-base -f engine-server/build/Dockerfile engine-server/build

docker-python:
	$(MAKE) engine-server
	$(MAKE) toolbox
	docker build -t marvin-python -f engine-server/build/docker-python/Dockerfile engine-server/build

docker-r:
	$(MAKE) engine-server
	$(MAKE) toolbox
	docker build -t marvin-r -f engine-server/build/docker-r/Dockerfile engine-server/build

engine-server:
	cd engine-server && $(MAKE) package
	mv engine-server/target/scala-2.12/marvin-engine-server-assembly-*.jar engine-server/build/marvin-engine-server-assembly.jar

toolbox:
	tar -cf engine-server/build/python-toolbox.tgz python-toolbox