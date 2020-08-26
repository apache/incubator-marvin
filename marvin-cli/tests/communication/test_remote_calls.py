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

from marvin_cli.communication.remote_calls import RemoteCalls

mocked_host = '0.0.0.0'
mocked_port = 0

mocked_name = 'GRPC'
mocked_params = {
    'mocked': 'params'
}
mocked_args = 'mocked_args'

@mock.patch('marvin_cli.communication.remote_calls.grpc.insecure_channel')
def test_RemoteCall_init(channel_mocked):
    RemoteCalls(mocked_host, mocked_port)
    channel_mocked.assert_called_with('0.0.0.0:0')

@mock.patch('marvin_cli.communication.remote_calls.daemon_pb2_grpc.CommandCallStub')
def test_call_command(stub_mocked):
    stub = stub_mocked.return_value
    rc = RemoteCalls()
    rc.call_command(mocked_name, mocked_params)
    stub.callCommand.assert_called()

@mock.patch('marvin_cli.communication.remote_calls.daemon_pb2_grpc.CommandCallStub')
def test_stop_command(stub_mocked):
    stub = stub_mocked.return_value
    rc = RemoteCalls()
    rc.stop_command(mocked_name)
    stub.stopCommand.assert_called()

@mock.patch('marvin_cli.communication.remote_calls.RemoteCalls.call_command')
def test_run_grpc(call_mocked):
    rc = RemoteCalls()
    rc.run_grpc('all', None, None)
    call_mocked.assert_called()

@mock.patch('marvin_cli.communication.remote_calls.RemoteCalls.stop_command')
def test_stop_grpc(stop_mocked):
    rc = RemoteCalls()
    rc.stop_grpc()
    stop_mocked.assert_called()

@mock.patch('marvin_cli.communication.remote_calls.RemoteCalls.call_command')
def test_run_dryrun(call_mocked):
    rc = RemoteCalls()
    rc.run_dryrun('all', True)
    call_mocked.assert_called()

@mock.patch('marvin_cli.communication.remote_calls.RemoteCalls.call_command')
def test_run_notebook(call_mocked):
    rc = RemoteCalls()
    rc.run_notebook(True, True)
    call_mocked.assert_called()

@mock.patch('marvin_cli.communication.remote_calls.RemoteCalls.call_command')
def test_run_lab(call_mocked):
    rc = RemoteCalls()
    rc.run_lab(True, True)
    call_mocked.assert_called()

@mock.patch('marvin_cli.communication.remote_calls.RemoteCalls.call_command')
def test_run_test(call_mocked):
    rc = RemoteCalls()
    rc.run_test(True, True, True, mocked_args)
    call_mocked.assert_called()

@mock.patch('marvin_cli.communication.remote_calls.RemoteCalls.call_command')
def test_run_tox(call_mocked):
    rc = RemoteCalls()
    rc.run_tox(mocked_args)
    call_mocked.assert_called()

@mock.patch('marvin_cli.communication.remote_calls.RemoteCalls.call_command')
def test_run_tdd(call_mocked):
    rc = RemoteCalls()
    rc.run_tdd(True, True, True, True, mocked_args)
    call_mocked.assert_called()
