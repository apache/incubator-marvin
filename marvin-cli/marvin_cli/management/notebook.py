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

import sys
import click
import time
from ..utils.log import get_logger
from ..communication.remote_calls import RemoteCalls
from ..utils.misc import init_port_forwarding

logger = get_logger('management.notebook')

@click.group("notebook")
def cli():
    pass

@cli.command("notebook", help="Run custom engine Jupyter Notebook.")
@click.option('--grpchost', '-gh', help='gRPC Host Address', default=None)
@click.option('--grpcport', '-gp', help='gRPC Port', default='50057')
@click.option('--notebook-port', '-np', help='Notebook port', default='8888')
@click.option('--no-port-forwarding', '-npf', is_flag=True, default=False, 
                help='Connect ports between this system and remote host with SSH tunnel')
@click.pass_context
def notebook(ctx, grpchost, grpcport, notebook_port, no_port_forwarding):
    if not grpchost:
        grpchost = 'localhost'

    rc = RemoteCalls(grpchost, grpcport)
    rc.run_notebook(notebook_port)
    if not no_port_forwarding:
        time.sleep(5)
        logger.info("Enabling port forwarding {0}:{0}...".format(notebook_port))
        init_port_forwarding(ctx.obj['engine_name'], ctx.obj['default_host'], 
                                ports_list=[int(notebook_port)])
    sys.exit(0)
