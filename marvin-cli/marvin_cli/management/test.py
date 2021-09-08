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

@click.group("test")
def cli():
    pass

@cli.command('test', help='Run tests.')
@click.option('--grpchost', '-gh', help='gRPC Host Address', default=None)
@click.option('--grpcport', '-gp', help='gRPC Port', default='50057')
@click.option('--cov/--no-cov', default=True)
@click.option('--no-capture', is_flag=True)
@click.option('--pdb', is_flag=True)
@click.argument('args', default='')
@click.pass_context
def test(ctx, grpchost, grpcport, cov, no_capture, pdb, args):
    if not grpchost:
        grpchost = ctx.obj['default_host']

    rc = RemoteCalls(grpchost, grpcport)
    rc.run_test(cov, no_capture, pdb, args)

@cli.command('test-tox', help='Run tests using Tox environment.')
@click.option('--grpchost', '-gh', help='gRPC Host Address', default=None)
@click.option('--grpcport', '-gp', help='gRPC Port', default='50057')
@click.argument('args', default='--current-env')
@click.pass_context
def tox(ctx, grpchost, grpcport, args):
    if not grpchost:
        grpchost = ctx.obj['default_host']

    rc = RemoteCalls(grpchost, grpcport)
    rc.run_tox(args)

@cli.command('test-tdd', help='Watch for changes to run tests automatically.')
@click.option('--grpchost', '-gh', help='gRPC Host Address', default=None)
@click.option('--grpcport', '-gp', help='gRPC Port', default='50057')
@click.option('--cov/--no-cov', default=False)
@click.option('--no-capture', is_flag=True)
@click.option('--pdb', is_flag=True)
@click.option('--partial', is_flag=True)
@click.argument('args', default='')
@click.pass_context
def tdd(ctx, grpchost, grpcport, cov, no_capture, pdb, partial, args):
    if not grpchost:
        grpchost = ctx.obj['default_host']

    rc = RemoteCalls(grpchost, grpcport)
    rc.run_tdd(cov, no_capture, pdb, partial, args)
