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

try:
    import mock
except ImportError:
    import unittest.mock as mock

from mock import call
from mock import ANY
from marvin_python_daemon.management.engine import MarvinDryRun
from marvin_python_daemon.management.engine import dryrun
import os


mocked_conf = {
    'marvin_package': 'test_package',
    'inidir': 'test_dir'
}


def mocked_sleep(value):
    if value == 100:
        raise KeyboardInterrupt()


class AcquisitorAndCleaner():

    def __init__(self, persistence_mode, is_remote_calling, default_root_path):
        self.persistence_mode = persistence_mode
        self.is_remote_calling = is_remote_calling
        self.default_root_path = default_root_path

    def execute(self, **kwargs):
        print('test')


@mock.patch('marvin_python_daemon.management.engine.time.time')
@mock.patch('marvin_python_daemon.management.engine.MarvinDryRun')
@mock.patch('marvin_python_daemon.management.engine.os.system')
def test_dryrun(system_mocked, MarvinDryRun_mocked, time_mocked):
    time_mocked.return_value = 555
    action = 'all'

    dryrun(config=mocked_conf, action=action, profiling=None)

    time_mocked.assert_called()
    MarvinDryRun_mocked.assert_called_with(config=mocked_conf, messages=[
                                           {}, {}])

    MarvinDryRun_mocked.return_value.execute.assert_called_with(clazz='Feedback',
                                                                params={}, profiling_enabled=None)

    action = 'acquisitor'

    dryrun(config=mocked_conf, action=action, profiling=None)

    time_mocked.assert_called()
    MarvinDryRun_mocked.assert_called_with(config=mocked_conf, messages=[
                                           {}, {}])


@mock.patch('marvin_python_daemon.management.engine.dynamic_import')
def test_marvindryrun(import_mocked):
    messages = ['/tmp/messages', '/tmp/feedback']
    response = 'response'
    clazz = 'PredictionPreparator'
    import_mocked.return_value = AcquisitorAndCleaner

    test_dryrun = MarvinDryRun(
        config=mocked_conf, messages=messages)
    test_dryrun.execute(clazz=clazz, params=None, profiling_enabled=True)

    import_mocked.assert_called_with("{}.{}".format(
        'test_package', 'PredictionPreparator'))

    clazz = 'Feedback'
    test_dryrun.execute(clazz=clazz, params=None, profiling_enabled=False)

    import_mocked.assert_called_with(
        "{}.{}".format('test_package', 'Feedback'))

    clazz = 'Predictor'
    test_dryrun.execute(clazz=clazz, params=None, profiling_enabled=False)

    import_mocked.assert_called_with("{}.{}".format(
        'test_package', 'PredictionPreparator'))

    clazz = 'test'
    test_dryrun.execute(clazz=clazz, params=None, profiling_enabled=True)
    test_dryrun.execute(clazz=clazz, params=None,  profiling_enabled=False)

    import_mocked.assert_called_with("{}.{}".format('test_package', 'test'))

    response = False
    clazz = 'PredictionPreparator'

    MarvinDryRun(config=mocked_conf, messages=messages)
    test_dryrun = MarvinDryRun(config=mocked_conf, messages=messages)
    test_dryrun.execute(clazz=clazz, params=None, profiling_enabled=False)
