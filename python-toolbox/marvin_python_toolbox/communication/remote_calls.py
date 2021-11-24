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

import grpc
import time
from ..utils.log import get_logger
from .stubs import daemon_pb2
from .stubs import daemon_pb2_grpc

logger = get_logger('communication')

class RemoteError(Exception):
    pass

COMMANDS = {
    'DRYRUN': daemon_pb2.Command.CommandType.Value('DRYRUN'),
    'TEST': daemon_pb2.Command.CommandType.Value('TEST'),
    'GRPC': daemon_pb2.Command.CommandType.Value('GRPC'),
    'TDD': daemon_pb2.Command.CommandType.Value('TDD'),
    'TOX': daemon_pb2.Command.CommandType.Value('TOX'),
    'NOTEBOOK': daemon_pb2.Command.CommandType.Value('NOTEBOOK'),
    'LAB': daemon_pb2.Command.CommandType.Value('LAB')
}

class RemoteCalls:

    stub = None

    def __init__(self, host='localhost', port=50057):
        channel = grpc.insecure_channel("{}:{}".format(host, port))
        self.stub = daemon_pb2_grpc.CommandCallStub(channel)

    def call_command(self, name, parameters):
        call = daemon_pb2.Command(command=COMMANDS[name], parameters=parameters)
        response = self.stub.callCommand(call)
        if response.status == daemon_pb2.Status.StatusType.Value('NOK'):
            raise RemoteError("Error during {}.".format(name))
        else:
            logger.info("{} triggered!".format(name))

    def stop_command(self, name):
        call = daemon_pb2.Interruption()
        response = self.stub.stopCommand(call)
        if response.status == daemon_pb2.Status.StatusType.Value('NOK'):
            raise RemoteError("Error during stop {}.".format(name))
        else:
            logger.info("{} stopped!".format(name))

    def run_dryrun(self, actions, profiling):
        parameters = {
            'action': actions,
            'profiling': str(profiling)
        }

        self.call_command('DRYRUN', parameters)

    def run_grpc(self, actions, max_workers, max_rpc_workers):
        parameters = {
            'action': actions
        }
        self.call_command('GRPC', parameters)

    def stop_grpc(self):
        self.stop_command('GRPC')

    def run_notebook(self, port):
        parameters = {
            'port': port
        }
        self.call_command('NOTEBOOK', parameters)

    def run_lab(self, port):
        parameters = {
            'port': port
        }
        self.call_command('LAB', parameters)

    def run_test(self, cov, no_capture, pdb, args):
        parameters = {
            'cov': str(cov),
            'no_capture': str(no_capture),
            'pdb': str(pdb),
            'args': args
        }

        self.call_command('TEST', parameters)

    def run_tdd(self, cov, no_capture, pdb, partial, args):
        parameters = {
            'cov': str(cov),
            'no_capture': str(no_capture),
            'pdb': str(pdb),
            'partial': str(partial),
            'args': args
        }

        self.call_command('TDD', parameters)

    def run_tox(self, args):
        parameters = {
            'args': args
        }

        self.call_command('TOX', parameters)
