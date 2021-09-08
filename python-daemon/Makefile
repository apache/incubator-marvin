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

.PHONY: help install test clean clean-build

help:
	@echo "    install"
	@echo "        Install the python-daemon on system."
	@echo "    test"
	@echo "        Run unit tests."
	@echo "    clean"
	@echo "        Clean former builds"

install:
	pip install .

test:
	tox

clean-pyc:
	find . -name '*.pyc' -exec rm -f {} +
	find . -name '*.pyo' -exec rm -f {} +
	find . -name '*~' -exec rm -f  {} +
	find . -name '__pycache__' -exec rm -rf {} +

clean-build:
	rm -f .prod
	rm -rf *.egg-info
	rm -rf .cache
	rm -rf .eggs
	rm -rf dist

clean-reports:
	rm -rf coverage_report/
	rm -rf html-cov/
	rm -f coverage.xml
	rm -f .coverage
	rm -rf .tox
	rm -rf .pytest_cache

clean: clean-pyc clean-build clean-reports
