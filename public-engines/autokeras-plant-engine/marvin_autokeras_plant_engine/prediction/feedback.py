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

"""Feedback engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBasePrediction

__all__ = ['Feedback']


logger = get_logger('feedback')


class Feedback(EngineBasePrediction):

    def __init__(self, **kwargs):
        super(Feedback, self).__init__(**kwargs)

    def execute(self, input_message, params, **kwargs):
        """
        Receive feedback message, user can manipulate this message for any use.
        Return "Done" to signal that the message is received and processed.
        """
        return {"message": "Done"}