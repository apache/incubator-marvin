#!/usr/bin/env python
# coding=utf-8

# Copyright [2017] [B2W Digital]
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
try:
    import mock
except ImportError:
    import unittest.mock as mock

from marvin_python_toolbox.engine_base import EngineBasePrediction


@pytest.fixture
def engine_action():
    class EngineAction(EngineBasePrediction):
        def execute(self, **kwargs):
            return 1

    return EngineAction(
        default_root_path="/tmp/.marvin",
        persistence_mode="local"
    )


class TestEngineBasePrediction:

    def test_model(self, engine_action):
        engine_action.marvin_model = [2]
        assert engine_action.marvin_model == engine_action._model == [2]

    def test_metrics(self, engine_action):
        engine_action.marvin_metrics = [3]
        assert engine_action.marvin_metrics == engine_action._metrics == [3]


class TestEnsureReloadActionReplaceObjectAttr:

    @mock.patch('marvin_python_toolbox.engine_base.engine_base_action.EngineBaseAction._serializer_load')
    def test_first_load_from_artifact_works(self, mock_serializer, engine_action):
        mock_serializer.return_value = "MOCKED"

        assert engine_action._model == None

        engine_action._load_obj(object_reference="model")
        
        assert engine_action._model == engine_action.marvin_model == "MOCKED"

    @mock.patch('marvin_python_toolbox.engine_base.engine_base_action.EngineBaseAction._serializer_load')
    def test_reload_works_before_first_load(self, mock_serializer, engine_action):
        mock_serializer.return_value = "MOCKED"

        assert engine_action._model == None

        engine_action._load_obj(object_reference="model")

        assert engine_action._model == engine_action.marvin_model == "MOCKED"

        mock_serializer.return_value = "NEW MOCKED"

        engine_action._load_obj(object_reference="model", force=True)

        assert engine_action._model == engine_action.marvin_model == "NEW MOCKED"