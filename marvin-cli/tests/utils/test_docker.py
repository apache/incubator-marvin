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

from marvin_cli.utils.docker import _get_client
from marvin_cli.utils.docker import _search_docker_image
from marvin_cli.utils.docker import _search_docker_container
from marvin_cli.utils.docker import search_engine_container
from marvin_cli.utils.docker import search_engine_images
from marvin_cli.utils.docker import create_engine_image
from marvin_cli.utils.docker import create_daemon_container

@mock.patch('marvin_cli.utils.docker.DockerClient.from_env')
def test_get_client(env_mocked):
    _get_client()
    env_mocked.assert_called()

@mock.patch('marvin_cli.utils.docker.DockerClient')
def test_get_client_remote(client_mocked):
    _get_client(env=False, url='docker://mocked_url')
    client_mocked.assert_called_with(base_url='docker://mocked_url')

def test_search_docker_image():
    assert not _search_docker_image('mocked_name')

def test_search_docker_container():
    assert not _search_docker_container('mocked_name')

@mock.patch('marvin_cli.utils.docker._search_docker_container')
def test_search_engine_container(search_mocked):
    search_engine_container('marvin_mocked')
    search_mocked.assert_called_with('marvin-cont-mocked')

@mock.patch('marvin_cli.utils.docker._search_docker_image')
def test_search_engine_images(search_mocked):
    search_engine_images('marvin_mocked')
    search_mocked.assert_called_with('marvin-mocked')

@mock.patch('marvin_cli.utils.docker._get_client')
@mock.patch('marvin_cli.utils.docker.generate_engine_package')
def test_create_engine_image(generate_mocked, client_mocked):
    create_engine_image('marvin_mocked')
    client_mocked.assert_called()
    generate_mocked.assert_called_with('marvin_mocked')

@mock.patch('marvin_cli.utils.docker._get_client')
def test_create_daemon_container(client_mocked):
    create_daemon_container('marvin_mocked', 'mocked')
    client_mocked.assert_called()