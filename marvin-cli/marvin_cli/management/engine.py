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
import sys
import json
import time
import wget
import click
import pickle
import pathlib
import subprocess
from cookiecutter.main import cookiecutter
from shutil import which
from ..communication.remote_calls import RemoteCalls
from ..utils.misc import package_folder, extract_folder, get_version
from ..utils.misc import call_logs, package_to_name, get_executor_path_or_download
from ..utils.misc import generate_timestamp, persist_process
from ..utils.git import git_init, bump_version
from ..utils.log import get_logger
from ..utils.benchmark import benchmark_thread, create_poi, make_graph

logger = get_logger('engine')

@click.group("engine")
def cli():
    pass

def _validate_project_name(name):
    return ''.join(c for c in name if c.isalnum())

TEMPLATE_FOLDER = os.path.join(pathlib.Path(__file__).parent.absolute(), 'template')

@cli.command("project-generate", help="Generate engine project")
@click.option('--name', '-n', prompt='Project name', help='Engine name')
@click.option('--description', '-d', prompt='Project description', help='Engine description')
@click.option('--url', '-u', prompt='Project URL', help='Engine URL Address', default='marvin.apache.org')
@click.option('--maintainer', '-m', prompt='Maintainer', help='Engine Maintainer')
@click.option('--email', '-e', prompt='Maintainer E-mail', help='Engine maintainer e-mail')
@click.option('--dest', '-f', envvar='MARVIN_HOME', type=click.Path(exists=True), help='Root folder path for the creation')
@click.option('--template', '-t', help='Base template for engine.', default=TEMPLATE_FOLDER, type=click.Path(exists=True))
def generate(name, description, url, maintainer, email, dest, template):
    _processed_name = _validate_project_name(name)

    _extras_dir = {
        'project_name': _processed_name,
        'project_package': 'marvin_' + _processed_name.lower(),
        'project_url': url,
        'project_description': description,
        'maintainer_name': maintainer,
        'maintainer_email': email
    }
    print(TEMPLATE_FOLDER)
    _init_dir = os.path.join(dest, _processed_name)
    cookiecutter(template, output_dir=dest, extra_context=_extras_dir, no_input=True)
    git_init(_init_dir)

    logger.info("Engine {0} created".format(_processed_name))

EXPORT_PATH = os.path.join(os.environ['MARVIN_DATA_PATH'], 'exports')

@cli.command("project-export", help="Export engine project to a archive file.")
@click.option('--dest', '-d', default=EXPORT_PATH, type=click.Path(exists=True), help='Output folder.')
@click.pass_context
def export(ctx, dest):
    filename = os.path.join(dest, ctx.obj['package_name'] 
                + '-' + get_version(ctx.obj['package_name']) + ".tar.gz")

    package_folder(os.getcwd(), filename)

@cli.command("project-import", help="Import engine project from archive file.")
@click.option('--file', '-f', type=click.Path(exists=True), help='Compressed Engine file.')
@click.option('--dest', '-d', envvar='MARVIN_HOME', type=click.Path(exists=True), help='Path to extract')
def import_project(file, dest):
    extract_folder(file, dest)

@cli.command("engine-dryrun", help="Run engines in a standalone way.")
@click.option('--grpchost', '-gh', help='gRPC Host Address', default='localhost')
@click.option('--grpcport', '-gp', help='gRPC Port', default='50057')
@click.option(
    '--action',
    '-a',
    default='all',
    type=click.Choice(['all', 'acquisitor', 'tpreparator', 'trainer', 'evaluator', 'ppreparator', 'predictor', 'feedback']),
    help='Marvin engine action name')
@click.option('--profiling', '-p', default=False, is_flag=True, help='Deterministic profiling of user code.')
def dryrun(grpchost, grpcport, action, profiling):
    rc = RemoteCalls(host, port)
    rc.run_dryrun(action, profiling)

@cli.command("engine-grpcserver", help="Run gRPC of given actions.")
@click.option('--grpchost', '-gh', help='gRPC Host Address', default='localhost')
@click.option('--grpcport', '-gp', help='gRPC Port', default='50057')
@click.option(
    '--action',
    '-a',
    default='all',
    type=click.Choice(['all', 'acquisitor', 'tpreparator', 'trainer', 'evaluator', 'ppreparator', 'predictor', 'feedback']),
    help='Marvin engine action name')
@click.option('--max-workers', '-w', help='Max Workers', default=None)
@click.option('--max-rpc-workers', '-rw', help='Max gRPC Workers', default=None)
def grpc(grpchost, grpcport, action, max_workers, max_rpc_workers):
    rc = RemoteCalls(grpchost, grpcport)
    rc.run_grpc(action, max_workers, max_rpc_workers)
    try:
        while(True):
            time.sleep(100)
    except KeyboardInterrupt:
        rc.stop_grpc()
        logger.info("gRPC server terminated!")

@cli.command("engine-logs", help="Show daemon execution.")
@click.option('--follow', '-f', is_flag=True)
@click.option('--tail', '-t', default=True, is_flag=True)
@click.option('--buffer', '-b', default=20)
@click.pass_context
def docker_logs(ctx, follow, tail, buffer):
    p_logs = call_logs(ctx.obj['package_name'], follow, buffer)
    if follow:
        persist_process(p_logs)

@cli.command("engine-httpserver", help="Run executor HTTP server.")
@click.option('--grpchost', '-gh', help='gRPC Host Address', default='localhost')
@click.option('--grpcport', '-gp', help='gRPC Port', default='50057')
@click.option('--host', '-h', prompt='API host', help='REST API Host', default='localhost')
@click.option('--port', '-p', prompt='API port', help='REST API Port', default='8000')
@click.option('--protocol', '-pr', help='Marvin protocol to be loaded during initialization.', default='')
@click.option(
    '--action',
    '-a',
    default='all',
    type=click.Choice(['all', 'acquisitor', 'tpreparator', 'trainer', 'evaluator', 'ppreparator', 'predictor', 'feedback']),
    help='Marvin engine action name')
@click.option('--max-workers', '-w', help='Max Workers', default=None)
@click.option('--max-rpc-workers', '-rw', help='Max gRPC Workers', default=None)
@click.option('--executor-path', '-e', help='Marvin engine executor jar path', type=click.Path(exists=True))
@click.option('--extra-executor-parameters', '-jvm', help='Use to send extra JVM parameters to engine executor process')
@click.option('--benchmark', '-b', default=False, is_flag=True, help='Run benchmark.')
@click.pass_context
def http(ctx, grpchost, grpcport, host, port, protocol, action, max_workers, 
            max_rpc_workers, executor_path, extra_executor_parameters, benchmark):

    rc = RemoteCalls(grpchost, grpcport)

    try:
        rc.run_grpc(action, max_workers, max_rpc_workers)
        time.sleep(3)
    except:
        print("Could not start grpc server!")
        sys.exit(1)

    bench_thread = None

    try:
        if not executor_path:
            executor_path = get_executor_path_or_download(ctx.obj['executor_url'])

        command_list = ['java']
        command_list.append('-DmarvinConfig.engineHome={}'.format(os.getcwd()))
        command_list.append('-DmarvinConfig.ipAddress={}'.format(host))
        command_list.append('-DmarvinConfig.port={}'.format(port))
        command_list.append('-DmarvinConfig.protocol={}'.format(protocol))

        if extra_executor_parameters:
            command_list.append(extra_executor_parameters)

        command_list.append('-jar')
        command_list.append(executor_path)

        if benchmark:
            logger.info("Init benchmark...")
            timestamp = generate_timestamp()
            bench_thread = benchmark_thread(ctx.obj['package_name'], timestamp) 
            bench_thread.start()

        httpserver = subprocess.Popen(command_list)

    except:
        logger.error("Could not start http server!")
        rc.stop_grpc()
        if benchmark:
            bench_thread.terminate()
        sys.exit(1)

    try:
        while True:
            time.sleep(100)

    except KeyboardInterrupt:
        logger.info("Terminating http and grpc servers...")
        rc.stop_grpc()
        httpserver.terminate() if httpserver else None
        logger.info("Http and grpc servers terminated!")
        if benchmark:
            bench_thread.terminate()
        logger.info("Benchmark terminated!")
        sys.exit(0)


@cli.command('engine-bumpversion', help='Bump, commit and tag engine version.')
@click.option('--verbose', is_flag=True)
@click.option('--dry-run', is_flag=True)
@click.option(
    '--part',
    '-p',
    default='patch',
    type=click.Choice(['major', 'minor', 'patch']),
    help='The part of the version to increase.')
def bumpversion(verbose, dry_run, part):
    bump_version(part, verbose, dry_run)

POI_LABELS = {
    'acquisitor': 'ac',
    'tpreparator': 'tp',
    'trainer': 't',
    'evaluator': 'e'
}

def _sleep(sec):
    logger.info("Sleeping for {0} seconds...".format(sec))
    time.sleep(5)

@cli.command("benchmark", help="Collect engine benchmark stats.")
@click.option('--grpchost', '-gh', help='gRPC Host Address', default='localhost')
@click.option('--grpcport', '-gp', help='gRPC Port', default='50057')
@click.option('--profiling', '-p', default=False, is_flag=True, help='Deterministic profiling of user code.')
@click.option('--delay', '-d', default=False, is_flag=True, help='Delay the benchmark for 5 seconds.')
@click.pass_context
def btest(ctx, grpchost, grpcport, profiling, delay):
    timestamp = generate_timestamp()
    b_thread = benchmark_thread(ctx.obj['package_name'], timestamp)
    actions = ['acquisitor', 'tpreparator', 'trainer',
                'evaluator']
    rc = RemoteCalls(grpchost, grpcport)
    initial_time = time.time()
    b_thread.start()
    if delay:
        _sleep(5)
    for action in actions:
        logger.info("Executing {0} action...".format(action))
        create_poi('{0}-i'.format(POI_LABELS[action]), 
                    time.time() - initial_time,
                    timestamp)
        rc.run_dryrun(action, profiling)
        create_poi('{0}-e'.format(POI_LABELS[action]), 
                    time.time() - initial_time,
                    timestamp)
        if delay:
            _sleep(5)
    b_thread.terminate()
    sys.exit(0)

PLOTS = {
    'cpu': 'CPU Usage',
    'memory': 'Memory Usage',
    'r_disk': 'Disk read',
    'w_disk': 'Disk write',
    'r_net': 'Network received',
    't_net': 'Network transfered'
}

@cli.command("benchmark-plot", help="Plot engine benchmark stats.")
@click.option('--protocol', '-p', prompt='Protocol', 
                help='Unique protocol from poi and benchmark files.')
def plot(protocol):
    options = (
        'cpu',
        'memory',
        'r_disk',
        'w_disk',
        'r_net',
        't_net'
    )
    try:
        while(True):
            print('1 - CPU Usage')
            print('2 - Memory Usage')
            print('3 - Disk read')
            print('3 - Disk write')
            print('4 - Network received')
            print('5 - Network transfered')
            print('Press Ctrl-c to end.')
            option = int(input('Option:'))
            if option < 1 or option > 5:
                logger.error('Option not available.')
                sys.exit(1)
            op_name = options[option - 1]
            label = PLOTS[op_name]
            make_graph(op_name, label, protocol)
    except KeyboardInterrupt:
        sys.exit(0)
