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
import multiprocessing
from .stubs import daemon_pb2
from .stubs import daemon_pb2_grpc
from concurrent import futures
from distutils.util import strtobool
from ..common.log import get_logger
from ..management.engine import dryrun, engine_server
from ..management.notebook import notebook, lab
from ..management.test import test, tox, tdd

logger = get_logger('daemon_base.command_server')

command_list = ['DRYRUN', 'TEST', 'TDD',
                'TOX', 'NOTEBOOK', 'LAB', 'GRPC']


def call_dryrun(config, parameters):
    profiling = strtobool(parameters['profiling'])

    dryrun(config, parameters['action'], bool(profiling))


def call_grpc(config, parameters):
    max_workers = int(parameters['max_workers']) if parameters['max_workers'] else multiprocessing.cpu_count()
    max_rpc_workers = int(parameters['max_rpc_workers']) if parameters['max_rpc_workers'] else multiprocessing.cpu_count()

    return engine_server(config, parameters['action'], max_workers,
                        max_rpc_workers)


def call_notebook(config, parameters):
    security = strtobool(parameters['enable_security'])

    notebook(config, bool(security), parameters['port'])


def call_lab(config, parameters):
    security = strtobool(parameters['enable_security'])

    lab(config, bool(security), parameters['port'])


def call_test(config, parameters):
    cov = strtobool(parameters['cov'])
    no_capture = strtobool(parameters['no_capture'])
    pdb = strtobool(parameters['pdb'])

    test(config, bool(cov), bool(no_capture), bool(pdb), parameters['args'])


def call_tdd(config, parameters):
    cov = strtobool(parameters['cov'])
    no_capture = strtobool(parameters['no_capture'])
    pdb = strtobool(parameters['pdb'])
    partial = strtobool(parameters['partial'])

    tdd(config, bool(cov), bool(no_capture), bool(
        pdb), bool(partial), parameters['args'])


def call_tox(config, parameters):
    tox(config, parameters['args'])


CALLS = {
    'DRYRUN': call_dryrun,
    'TEST': call_test,
    'TDD': call_tdd,
    'TOX': call_tox,
    'NOTEBOOK': call_notebook,
    'LAB': call_lab,
    'GRPC': call_grpc
}


class CommandServicer(daemon_pb2_grpc.CommandCall):

    def __init__(self, config):
        self.config = config
        self.command_running = None
        self.command_processes = None

    def callCommand(self, request, context):
        response = daemon_pb2.Status()
        self.command_running = command_list[request.command]
        command_call = CALLS[self.command_running]

        try:
            logger.info("Command {0} called!".format(self.command_running))
            self.command_processes = command_call(
                self.config, request.parameters)
            logger.info("Command {0} successful!".format(self.command_running))
            print(self.command_processes)
            if not self.command_processes:
                self.command_running = None
            response.status = daemon_pb2.Status.StatusType.OK
        except:
            logger.exception(
                "Command {0} failed!".format(self.command_running))
            response.status = daemon_pb2.Status.StatusType.NOK

        return response

    def stopCommand(self, request, context):
        response = daemon_pb2.Status()

        try:
            n_servers = 1
            for server in self.command_processes:
                logger.info("{0} servers terminated.".format(n_servers))
                server.stop(0)
                n_servers += 1
            logger.info("Command {0} terminated.".format(self.command_running))
            self.command_running = None
            self.command_processes = None
            response.status = daemon_pb2.Status.StatusType.OK
        except:
            logger.exception(
                "Unable to stop command: {0}.".format(self.command_running))
            response.status = daemon_pb2.Status.StatusType.NOK

        return response

    def getState(self, request, response):
        response = daemon_pb2.State()

        response.engine_name = self.config['marvin_package']
        response.command = 'None' if not self.command_running else self.command_running

        return response


def init_server(config):
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=multiprocessing.cpu_count()))

    daemon_pb2_grpc.add_CommandCallServicer_to_server(
        CommandServicer(config), server)

    logger.info('Starting server. Listening on port 50057.')
    server.add_insecure_port('[::]:50057')
    server.start()

    try:
        while True:
            time.sleep(10)
    except KeyboardInterrupt:
        server.stop(0)
