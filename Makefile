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

.PHONY: help python-daemon-base engine-server python-daemon executor-base

help:
	@echo "    python-daemon-base"
	@echo "        Builds a docker image that can run engines in Python."
	@echo "    python-daemon"
	@echo "        Builds the python-daemon and make it available for be included into docker images."
	@echo "    engine-server"
	@echo "        Builds a jar with the engine interpreter server."
	@echo "    executor-base"
	@echo "        Builds a docker image that can run engine executor."

python-daemon-base:
	$(MAKE) python-daemon
	docker build -t marvin-daemon:python -f build/daemon/Dockerfile build/daemon

engine-server:
	cd engine-executor && $(MAKE) package
	mv engine-executor/target/scala-2.12/marvin-engine-executor-assembly-*.jar build/executor/marvin-engine-executor-assembly.jar

python-daemon:
	tar -zcvf build/daemon/python-daemon.tar.gz python-daemon

executor-base:
	$(MAKE) engine-server
	docker build -t marvin-executor -f build/executor/Dockerfile build/executor