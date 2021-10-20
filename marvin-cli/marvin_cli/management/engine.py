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
import shutil
import subprocess
import getpass
from cookiecutter.main import cookiecutter
from shutil import which
from ..utils.docker import DaemonManagement
from ..utils.docker import search_engine_container, search_docker_volume, shutdown_and_delete_container
from ..utils.docker import create_engine_image, create_deploy_image_and_push, create_docker_volume, create_daemon_container
from ..utils.docker import create_executor_container, delete_image_and_volume
from ..utils.docker import rename_image, create_tfserving_container
from ..communication.remote_calls import RemoteCalls
from ..utils.misc import package_folder, extract_folder, get_version
from ..utils.misc import call_logs, package_to_name, get_executor_path_or_download
from ..utils.misc import generate_timestamp, write_tmp_info, generate_keys
from ..utils.misc import init_port_forwarding
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

def check_engine(engine):
    _engine_path = os.path.join(os.environ['MARVIN_HOME'], engine)
    daemon = DaemonManagement(engine)

    if os.path.exists(_engine_path):
        if click.confirm('Do you want to update the cached engine?', default=True):
            shutil.rmtree(_engine_path)
            daemon.clone_engine()
    else:
        logger.info("Caching engine...")
        daemon.clone_engine()
        logger.info("Caching engine... Done!")

@cli.command("project-generate", help="Generate engine project")
@click.option('--name', '-n', prompt='Project name', help='Engine name')
@click.option('--description', '-d', prompt='Project description', help='Engine description', default='Marvin project')
@click.option('--url', '-u', prompt='Project URL', help='Engine URL Address', default='marvin.apache.org')
@click.option('--maintainer', '-m', prompt='Maintainer', help='Engine Maintainer', default='Marvin-AI')
@click.option('--email', '-e', prompt='Maintainer E-mail', help='Engine maintainer e-mail', default='dev@marvin.apache.org')
@click.option(
    '--ptype',
    '-pt',
    default='python',
    type=click.Choice(['python', 'tfx']),
    help='Project type: Regular python project or TFX.')
@click.option('--template', '-t', help='Base template for engine.', default=TEMPLATE_FOLDER, type=click.Path(exists=True))
def generate(name, description, url, maintainer, email, ptype, template):
    #create engine files in /tmp/marvin
    _dest = '/tmp/marvin'
    _processed_name = _validate_project_name(name)
    _extras_dir = {
        'project_name': _processed_name,
        'project_package': 'marvin_' + _processed_name.lower(),
        'project_url': url,
        'project_description': description,
        'maintainer_name': maintainer,
        'maintainer_email': email,
        'project_type': ptype
    }
    _init_dir = os.path.join(_dest, _processed_name)
    cookiecutter(template, output_dir=_dest, extra_context=_extras_dir, no_input=True)
    git_init(_init_dir)

    #generate and put key in docker context
    _pubkey_path = generate_keys(_processed_name)
    _new_pubkey_path = os.path.join(_init_dir, 'docker', 'develop', 'daemon', 'id_rsa.pub')
    shutil.move(_pubkey_path, _new_pubkey_path)

    logger.info("Engine {0} created in /tmp/marvin.".format(_processed_name))
    create_engine_image(name, _init_dir)
    logger.info("Removing temporary files.")
    shutil.rmtree(_init_dir, ignore_errors=True)
    logger.info("Engine creation done!")

EXPORT_PATH = os.path.join(os.environ['MARVIN_DATA_PATH'], 'exports')

@cli.command("clone", help="Clone files from daemon container.")
@click.pass_context
def engine_clone(ctx):
    daemon = DaemonManagement(ctx.obj['engine_name'])
    daemon.clone_engine()
    daemon.clone_artifacts()
    daemon.clone_logs()

@cli.command("push", help="Rewrite engine files of daemon container.")
@click.option('--compress/--not-compress', '-c/-nc', default=True, is_flag=True, help='Compress the stream.')
@click.pass_context
def engine_push(ctx, compress):
    daemon = DaemonManagement(ctx.obj['engine_name'])
    daemon.push_engine(compress)

@cli.command("data", help="Actions related to data folder of daemon container.")
@click.option(
    '--action',
    '-a',
    default='list',
    type=click.Choice(['push', 'delete', 'list']),
    help='Data folder action type')
@click.option('--compress/--not-compress', '-c/-nc', default=True, is_flag=True, help='Compress the stream.')
@click.pass_context
def data_push(ctx, action, compress):
    daemon = DaemonManagement(ctx.obj['engine_name'])
    if action == 'list':
        daemon.list_data_files()
    elif action == 'push':
        _path = click.prompt('File to push', type=click.Path(exists=True))
        daemon.push_data(_path, compress)
    else:
        _path = click.prompt('File to delete')
        daemon.delete_data(_path)

@cli.command("setup", help="Setup docker container and volumes to development.")
@click.argument('engine', nargs=1)
@click.pass_context
def setup(ctx, engine):
    _engine_volume = "marvin-{}-vol".format(engine)

    logger.info("Setting up engine components...")
    if not search_docker_volume("marvin-log"):
        create_docker_volume("marvin-log")
    if not search_docker_volume("marvin-data"):
        create_docker_volume("marvin-data")
    if not search_docker_volume(_engine_volume):
        create_docker_volume(_engine_volume)
    if not search_engine_container(engine):
        create_daemon_container(engine)
    write_tmp_info('engine', engine)
    logger.info("Setting up engine components... Done!")
    logger.info("Attaching logs...")
    call_logs(engine)
    logger.info("Attaching logs... Done!")
    logger.info("Enabling port forwarding...")
    init_port_forwarding(engine, ctx.obj['default_host'], [50057], background=True)
    logger.info("Enabling port forwarding... Done!")

@cli.command("stop", help="Stop docker container and workon.")
@click.option('--delete', '-d', default=False, is_flag=True, help='Delete engine files permanently.')
@click.pass_context
def stop(ctx, delete):
    _container_name = "marvin-cont-{}".format(ctx.obj['engine_name'])
    _lock_path = '/tmp/marvin/engine'

    logger.info("Stopping and deleting engine container...")
    shutdown_and_delete_container(_container_name)
    os.remove(_lock_path)
    logger.info("Stopping and deleting engine container... Done!")

    if delete:
        check = click.prompt('If you are sure, type the engine name: ')
        if check == ctx.obj['engine_name']:
            _image_name = "marvin-{}".format(ctx.obj['engine_name'])
            _volume_name = "marvin-{}-vol".format(ctx.obj['engine_name'])
            delete_image_and_volume(_image_name, _volume_name)
            logger.warning("Deleting private key...")
            _key_path = os.path.join(os.environ['MARVIN_DATA_PATH'], '.keys', 
                        ctx.obj['engine_name'])
            shutil.rmtree(_key_path)
            logger.warning("Deleting private key... Done!")

@cli.command("project-export", help="Export engine project to a archive file.")
@click.option('--dest', '-d', default=EXPORT_PATH, type=click.Path(exists=True), help='Output folder.')
@click.pass_context
def export(ctx, dest):
    check_engine(ctx.obj['engine_name'])
    path = os.path.join(os.environ['MARVIN_HOME'], ctx.obj['engine_name'])
    filename = os.path.join(dest, ctx.obj['engine_name'] 
                + '-' + get_version(path, ctx.obj['engine_name']) + ".tar.gz")

    package_folder(path, filename)

@cli.command("project-import", help="Import engine project from archive file.")
@click.option('--file', '-f', type=click.Path(exists=True), help='Compressed Engine file.')
@click.option('--dest', '-d', envvar='MARVIN_HOME', type=click.Path(exists=True), help='Path to extract')
def import_project(file, dest):
    extract_folder(file, dest)

@cli.command("engine-dryrun", help="Run engines in a standalone way.")
@click.option('--grpchost', '-gh', help='gRPC Host Address', default=None)
@click.option('--grpcport', '-gp', help='gRPC Port', default='50057')
@click.option(
    '--action',
    '-a',
    default='all',
    type=click.Choice(['all', 'acquisitor', 'tpreparator', 'trainer', 'evaluator', 'ppreparator', 'predictor', 'feedback']),
    help='Marvin engine action name')
@click.option('--profiling', '-p', default=False, is_flag=True, help='Deterministic profiling of user code.')
@click.pass_context
def dryrun(ctx, grpchost, grpcport, action, profiling):
    if not grpchost:
        grpchost = 'localhost'

    rc = RemoteCalls(grpchost, grpcport)
    rc.run_dryrun(action, profiling)

def grpc_port_forwarding(engine_name, grpchost):
    ports = [
        50051,
        50052,
        50053,
        50054,
        50055,
        50056,
    ]
    init_port_forwarding(engine_name, grpchost, 
                            ports_list=ports)

@cli.command("engine-grpcserver", help="Run gRPC of given actions.")
@click.option('--grpchost', '-gh', help='gRPC Host Address', default=None)
@click.option('--grpcport', '-gp', help='gRPC Port', default='50057')
@click.option(
    '--action',
    '-a',
    default='all',
    type=click.Choice(['all', 'acquisitor', 'tpreparator', 'trainer', 'evaluator', 'ppreparator', 'predictor', 'feedback']),
    help='Marvin engine action name')
@click.option('--max-workers', '-w', help='Max Workers', default=None)
@click.option('--max-rpc-workers', '-rw', help='Max gRPC Workers', default=None)
@click.pass_context
def grpc(ctx, grpchost, grpcport, action, max_workers, max_rpc_workers):
    if not grpchost:
        grpchost = 'localhost'

    rc = RemoteCalls(grpchost, grpcport)
    rc.run_grpc(action, max_workers, max_rpc_workers)
    grpc_port_forwarding(ctx.obj['engine_name'], ctx.obj['default_host'])
    rc.stop_grpc()
    logger.info("gRPC server terminated!")

@cli.command("engine-httpserver", help="Run executor HTTP server.")
@click.option('--grpchost', '-gh', help='gRPC Host Address', default=None)
@click.option('--grpcport', '-gp', help='gRPC Port', default='50057')
@click.option('--host', '-h', help='REST API Host', default='localhost')
@click.option('--port', '-p', help='REST API Port', default='8000')
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
@click.option('--no-docker', '-nd', default=False, is_flag=True, help='Don\'t run the engine-executor on a Docker container.')
@click.pass_context
def http(ctx, grpchost, grpcport, host, port, protocol, action, max_workers, 
            max_rpc_workers, executor_path, extra_executor_parameters, benchmark, no_docker):

    if not grpchost:
        grpchost = 'localhost'
    
    if not host:
        host = ctx.obj['default_host']

    rc = RemoteCalls(grpchost, grpcport)

    try:
        rc.run_grpc(action, max_workers, max_rpc_workers)
        time.sleep(3)
    except:
        print("Could not start grpc server!")
        sys.exit(1)

    bench_thread = None
    httpserver = None

    try:
        if not no_docker:
            create_executor_container(ctx.obj['engine_name'])
        else:
            if not executor_path:
                executor_path = get_executor_path_or_download(ctx.obj['executor_url'])

            check_engine(ctx.obj['engine_name'])
            engine_path = os.path.join(os.environ['MARVIN_HOME'], ctx.obj['engine_name'])

            command_list = ['java']
            command_list.append('-DmarvinConfig.engineHome={}'.format(engine_path))
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

            while True:
                try:
                    time.sleep(100)
                except KeyboardInterrupt:
                    break
                
    except Exception as e:
        logger.error("Could not start http server!")
        if not no_docker:
            shutdown_and_delete_container("marvin-executor-" +
                ctx.obj['engine_name'])
        print(e)
        rc.stop_grpc()
        if benchmark:
            bench_thread.terminate()
        sys.exit(1)
    
    if no_docker:
        logger.info("Terminating http and grpc servers...")
        rc.stop_grpc()
        httpserver.terminate() if httpserver else None
        logger.info("Http and grpc servers terminated!")
    else:
        try:
            while True:
                time.sleep(100)

        except KeyboardInterrupt:
            logger.info("Terminating http and grpc servers...")
            rc.stop_grpc()
            shutdown_and_delete_container("marvin-executor-" +
                                            ctx.obj['engine_name'])
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
@click.pass_context
def bumpversion(ctx, verbose, dry_run, part):
    check_engine(ctx.obj['engine_name'])
    path = os.path.join(os.environ['MARVIN_HOME'], ctx.obj['engine_name'])
    bump_version(path, part, verbose, dry_run)

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
@click.option('--grpchost', '-gh', help='gRPC Host Address', default=None)
@click.option('--grpcport', '-gp', help='gRPC Port', default='50057')
@click.option('--profiling', '-p', default=False, is_flag=True, help='Deterministic profiling of user code.')
@click.option('--delay', '-d', default=False, is_flag=True, help='Delay the benchmark for 5 seconds.')
@click.pass_context
def btest(ctx, grpchost, grpcport, profiling, delay):
    timestamp = generate_timestamp()
    b_thread = benchmark_thread(ctx.obj['engine_name'], timestamp)
    actions = ['acquisitor', 'tpreparator', 'trainer',
                'evaluator']

    if not grpchost:
        grpchost = 'localhost'

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

@cli.command("kube-deployment", help="Deploy Kubernetes production pod.")
@click.option('--namespace', '-p', default='default', help='Define Kubernetes namespace.')
@click.option('--nreplicas', '-nr', default=1, help='Define number of pod replicas.')
@click.option('--name-in-registry', '-nr', prompt='Image name in registry', help='Docker registry repository to pull the deploy image.', default='')
@click.option('--target-port', '-tp', help='Target port on cluster.', default="9736")
@click.pass_context
def kube_deploy(ctx, namespace, nreplicas, name_in_registry, target_port):
    engine_name = ctx.obj['engine_name']
    port = None
    if not click.confirm('Do you want to use the TFX serving method?', default=False):
        artifact_path = os.path.join(os.environ['MARVIN_DATA_PATH'], 
                                    '.artifacts', engine_name)
        engine_path = os.path.join(os.environ['MARVIN_HOME'], engine_name)
        deps_path = os.path.join(engine_path, 'docker', 'deploy', 'daemon', 'deps')
        executor_path = get_executor_path_or_download(ctx.obj['executor_url'])

        daemon = DaemonManagement(engine_name)
        check_engine(engine_name)
        daemon.clone_artifacts()

        shutil.move(artifact_path, os.path.join(deps_path, 'artifacts'))
        shutil.copy(executor_path, deps_path)
        create_deploy_image_and_push(engine_name, engine_path, name_in_registry)
        port = "8000"
    else:
        model_path = click.prompt('Model path', type=str)
        old_image_name = "tfserving:{}".format(engine_name)
        create_tfserving_container(engine_name, model_path)
        rename_image(old_image_name, name_in_registry)
        logger.info("Pushing deploy image to registry...")
        os.system("docker push {}".format(name_in_registry))
        logger.info("Pushing deploy image to registry... Done!")
        port = "8500"

    kubernetes_template = os.path.join(pathlib.Path(__file__).parent.absolute(), 
                                        'kubernetes_template')

    _extras_dir = {
        'engine_name': engine_name,
        'n_reps': nreplicas,
        'image_name': name_in_registry,
        'service_name': "{}-service".format(engine_name),
        'deployment_name': "{}-deployment".format(engine_name),
        'target_port': target_port,
        'container_port': port
    }

    cookiecutter(kubernetes_template, output_dir=os.getcwd(), 
                    extra_context=_extras_dir, no_input=True)




    


    
