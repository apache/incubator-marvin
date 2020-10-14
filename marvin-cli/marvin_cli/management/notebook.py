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
from ..communication.remote_calls import RemoteCalls

@click.group("notebook")
def cli():
    pass

@cli.command("notebook", help="Run custom engine Jupyter Notebook.")
@click.option('--grpchost', '-gh', prompt='gRPC host', help='gRPC Host Address', default='localhost')
@click.option('--grpcport', '-gp', prompt='gRPC port', help='gRPC Port', default='50057')
@click.option('--notebook-port', '-np', prompt='Notebook port', help='Notebook port', default='8888')
def notebook(grpchost, grpcport, notebook_port):
    rc = RemoteCalls(grpchost, grpcport)
    rc.run_notebook(notebook_port)

@cli.command("lab", help="Run custom engine Jupyter Lab.")
@click.option('--host', '-gh', prompt='gRPC host', help='gRPC Host Address', default='localhost')
@click.option('--port', '-gp', prompt='gRPC port', help='gRPC Port', default='50057')
@click.option('--notebook-port', '-np', prompt='Notebook port', help='Notebook port', default='8888')
@click.option('--enable-security', '-s', default=False, is_flag=True, help='Enable notebook security.')
def lab(grpchost, grpcport, notebook_port, enable_security):
    rc = RemoteCalls(grpchost, grpcport)
    rc.run_lab(notebook_port, enable_security)
