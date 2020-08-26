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
import os

try:
    import mock
except ImportError:
    import unittest.mock as mock

from marvin_cli.utils.config import parse_ini
from marvin_cli.utils.config import read_cli_conf
from marvin_cli.utils.config import generate_default_conf

mocked_path = '/path/to/nowhere'

@mock.patch('marvin_cli.utils.config.configparser.ConfigParser.read')
def test_parse_ini(read_mocked):
    parse_ini(mocked_path)
    read_mocked.assert_called_with(mocked_path)

@mock.patch('marvin_cli.utils.config.json.load')
@mock.patch('marvin_cli.utils.config.open')
def test_read_cli_conf(open_mocked, load_mocked):
    _conf_path = os.path.join(os.environ['MARVIN_DATA_PATH'], '.conf', 'cli_conf.json')
    read_cli_conf()
    open_mocked.assert_called_with(_conf_path, 'r')
    load_mocked.assert_called()

@mock.patch('marvin_cli.utils.config.json.dump')
def test_generate_default_conf(dump_mocked):
    generate_default_conf()
    dump_mocked.assert_called()