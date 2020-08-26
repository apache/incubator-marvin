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
import os
import sys
import time
import wget
import subprocess
import pathlib
from cookiecutter.main import cookiecutter
from ..communication.remote_calls import RemoteCalls
from ..utils.misc import package_folder, extract_folder, get_version
from ..utils.misc import call_logs, package_to_name, get_executor_path_or_download
from ..utils.git import git_init, bump_version
from ..utils.log import get_logger

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
@click.option('--host', '-h', prompt='gRPC host', help='gRPC Host Address', default='localhost')
@click.option('--port', '-p', prompt='gRPC port', help='gRPC Port', default='50057')
@click.option(
    '--action',
    '-a',
    default='all',
    type=click.Choice(['all', 'acquisitor', 'tpreparator', 'trainer', 'evaluator', 'ppreparator', 'predictor', 'feedback']),
    help='Marvin engine action name')
@click.option('--profiling', '-p', default=False, is_flag=True, help='Deterministic profiling of user code.')
def dryrun(host, port, action, profiling):
    rc = RemoteCalls(host, port)
    rc.run_dryrun(action, profiling)

@cli.command("engine-grpcserver", help="Run gRPC of given actions.")
@click.option('--host', '-h', prompt='gRPC host', help='gRPC Host Address', default='localhost')
@click.option('--port', '-p', prompt='gRPC port', help='gRPC Port', default='50057')
@click.option(
    '--action',
    '-a',
    default='all',
    type=click.Choice(['all', 'acquisitor', 'tpreparator', 'trainer', 'evaluator', 'ppreparator', 'predictor', 'feedback']),
    help='Marvin engine action name')
@click.option('--max-workers', '-w', help='Max Workers', default=None)
@click.option('--max-rpc-workers', '-rw', help='Max gRPC Workers', default=None)
def grpc(host, port, action, max_workers, max_rpc_workers):
    rc = RemoteCalls(host, port)
    rc.run_grpc(action, max_workers, max_rpc_workers)
    try:
        while(True):
            time.sleep(100)
    except KeyboardInterrupt:
        rc.stop_grpc()
        logger.info("gRPC server terminated!")

@cli.command("engine-logs", help="Show daemon execution.")
@click.pass_context
def docker_logs(ctx):
    call_logs(ctx.obj['package_name'])

@cli.command("engine-httpserver", help="Run executor HTTP server.")
@click.option('--grpchost', '-gh', prompt='gRPC host', help='gRPC Host Address', default='localhost')
@click.option('--grpcport', '-gp', prompt='gRPC port', help='gRPC Port', default='50057')
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
@click.pass_context
def http(ctx, grpchost, grpcport, host, port, protocol, action, max_workers, 
            max_rpc_workers, executor_path, extra_executor_parameters):

    rc = RemoteCalls(grpchost, grpcport)

    try:
        rc.run_grpc(action, max_workers, max_rpc_workers)
        time.sleep(3)
    except:
        print("Could not start grpc server!")
        sys.exit(1)

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

        print(command_list)

        httpserver = subprocess.Popen(command_list)

    except:
        print("Could not start http server!")
        rc.stop_grpc()
        sys.exit(1)

    try:
        while True:
            time.sleep(100)

    except KeyboardInterrupt:
        logger.info("Terminating http and grpc servers...")
        rc.stop_grpc()
        httpserver.terminate() if httpserver else None
        logger.info("Http and grpc servers terminated!")
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
