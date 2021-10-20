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

from marvin_cli.utils.git import git_init
from marvin_cli.utils.git import bump_version

mocked_path = '/path/to/nowhere'

@mock.patch('marvin_cli.utils.git.os.system')
@mock.patch('marvin_cli.utils.git.os.chdir')
def test_git_init(chdir_mocked, system_mocked):
    git_init(mocked_path)
    system_mocked.assert_called_with('git init .')
    chdir_mocked.assert_called()

@mock.patch('marvin_cli.utils.git.os.system')
def test_bumpversion(system_mocked):
    bump_version('patch', True, True)
    system_mocked.assert_called_with('bump2version patch --verbose --dry-run')