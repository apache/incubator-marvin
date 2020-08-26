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

from marvin_cli.utils.misc import make_tarfile
from marvin_cli.utils.misc import package_to_name
from marvin_cli.utils.misc import get_version
from marvin_cli.utils.misc import package_folder
from marvin_cli.utils.misc import extract_folder
from marvin_cli.utils.misc import call_logs
from marvin_cli.utils.misc import get_executor_path_or_download

mocked_package = 'marvin_mocked'
mocked_path = '/path/to/nowhere'
mocked_url = 'https://some.url/marvin.jar'

@mock.patch('marvin_cli.utils.misc.tarfile.open')
def test_make_tarfile(open_mocked):
    make_tarfile(mocked_path, mocked_path)
    open_mocked.assert_called_with(mocked_path, 'w:gz')

def test_package_to_name():
    assert package_to_name(mocked_package) == 'mocked'

@mock.patch('marvin_cli.utils.misc.os.path.join')
def test_get_version(join_mocked):
    get_version(mocked_package)
    join_mocked.assert_called_with(os.getcwd(), mocked_package, 'VERSION')

@mock.patch('marvin_cli.utils.misc.tarfile.open')
def test_package_folder(open_mocked):
    package_folder(mocked_path, mocked_path)
    open_mocked.assert_called_with(mocked_path, 'w:gz')

@mock.patch('marvin_cli.utils.misc.tarfile.open')
def test_extract_folder(open_mocked):
    extract_folder(mocked_path, mocked_path)
    open_mocked.assert_called_with(mocked_path)

@mock.patch('marvin_cli.utils.misc.subprocess.Popen')
def test_call_logs(popen_mocked):
    call_logs(mocked_package)
    popen_mocked.assert_called_with(['xterm', '-e', 'docker', 'logs', '-f', 'marvin-cont-mocked'])

@mock.patch('marvin_cli.utils.misc.wget')
def test_get_executor_path_or_download(wget_mocked):
    get_executor_path_or_download(mocked_url)
    wget_mocked.assert_called_with(mocked_url, out=os.path.join(os.environ['MARVIN_DATA_PATH'], 'marvin.jar'))