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

.PHONY: help marvin update clean-pyc clean-build clean-reports clean-deps clean

help:
	@echo "    marvin"
	@echo "        Prepare project to be used as a marvin package."
	@echo "    update"
	@echo "        Reinstall requirements and setup.py dependencies."
	@echo "    clean-all"
	@echo "        Remove all generated artifacts."
	@echo "    clean-pyc"
	@echo "        Remove python artifacts."
	@echo "    clean-build"
	@echo "        Remove build artifacts."
	@echo "    clean-reports"
	@echo "        Remove coverage reports."
	@echo "    clean-deps"
	@echo "        Remove marvin setup.py dependencies."

marvin:
	pip install -e . --process-dependency-links
	marvin --help

update:
	pip install -e . -U --process-dependency-links

clean-pyc:
	find . -name '*.pyc' -exec rm -f {} +
	find . -name '*.pyo' -exec rm -f {} +
	find . -name '*~' -exec rm -f  {} +

clean-build:
	rm -rf *.egg-info
	rm -rf .cache
	rm -rf .eggs
	rm -rf dist

clean-reports:
	rm -rf coverage_report/
	rm -f coverage.xml
	rm -f .coverage

clean-deps:
	pip freeze | grep -v "^-e" | xargs pip uninstall -y

clean: clean-build clean-pyc clean-reports clean-deps
