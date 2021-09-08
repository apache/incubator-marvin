#!/usr/bin/env python
# coding=utf-8

# Copyright [2020] [Apache Software Foundation]
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

import pytest

from marvin_python_daemon.engine_base import EngineBaseDataHandler


@pytest.fixture
def engine_action():
    class EngineAction(EngineBaseDataHandler):
        def execute(self, **kwargs):
            return 1

    return EngineAction(default_root_path="/tmp/.marvin")


class TestEngineBaseDataHandler:

    def test_initial_dataset(self, engine_action):
        engine_action.marvin_initial_dataset = [1]
        assert engine_action.marvin_initial_dataset == engine_action._initialdataset == [
            1]

    def test_dataset(self, engine_action):
        engine_action.marvin_dataset = [1]
        assert engine_action.marvin_dataset == engine_action._dataset == [1]
