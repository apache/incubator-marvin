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
import click
import click.testing

try:
    import mock
except ImportError:
    import unittest.mock as mock

from marvin_cli.management.edit import config
from marvin_cli.management.edit import metadata
mocked_obj = {
    'editor': 'mocked'
}

def test_config():
    ctx = click.Context(click.Command('edit-config'), obj=mocked_obj)
    with ctx:
        runner = click.testing.CliRunner()
        runner.invoke(config)

def test_metadata():
    ctx = click.Context(click.Command('edit-metadata'), obj=mocked_obj)
    with ctx:
        runner = click.testing.CliRunner()
        runner.invoke(metadata)



