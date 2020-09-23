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
import subprocess
import tarfile
import wget
import glob
import pickle
import datetime
from .log import get_logger

logger = get_logger('misc')

def make_tarfile(output_filename, source_dir):
    with tarfile.open(output_filename, "w:gz") as tar:
        tar.add(source_dir, arcname=os.path.basename(source_dir))

def package_to_name(package):
    #remove marvin_ substring
    return package[len("marvin_"):]

def generate_engine_package(package):
    subprocess.run(['python', 'setup.py', 'sdist'])
    filename = package + "-" + get_version(package) + ".tar.gz"
    origin = os.path.join(os.getcwd(), "dist", filename)
    dest = os.path.join(os.getcwd(), "docker", "develop" ,"daemon", filename)
    os.rename(origin, dest)

def get_version(package):
    with open(os.path.join(os.getcwd(), package ,"VERSION"), 'rb') as f:
        version = f.read().decode('ascii').strip()
    return version

def package_folder(input, output):
    with tarfile.open(output, "w:gz") as tar:
        tar.add(input, arcname=os.path.basename(input))

def extract_folder(input, output):
    tf = tarfile.open(input)
    tf.extractall(output)

def call_logs(package, follow, buffer):
    container_name = 'marvin-cont-' + package_to_name(package)
    p_return = None
    if follow:
        p_return = subprocess.Popen(['docker', 'logs', '--follow', container_name], stdout=subprocess.PIPE)
    else:
        p_return = subprocess.Popen(['docker', 'logs', '--tail', str(buffer), container_name], stdout=subprocess.PIPE)

    return p_return

def create_or_return_tmp_dir():
    tmp_path = '/tmp/marvin'
    if not os.path.exists(tmp_path):
        os.makedirs(tmp_path)
    return tmp_path

def kill_persisted_process():
    base_path = create_or_return_tmp_dir()
    for obj_file in glob.glob('{0}/*.mproc'.format(base_path)):
        pid = int(
            obj_file[(len(base_path) + 1):(len('.mproc') * -1)]
        )
        try:
            os.kill(pid, 9)
            logger.info("PID {0} now killed!".format(pid))
        except ProcessLookupError:
            logger.info("PID {0} already killed!".format(pid))
        os.remove(obj_file)
        

def persist_process(obj):
    filepath = os.path.join(create_or_return_tmp_dir(), 
                            '{0}.mproc'.format(obj.pid))
    logger.info("Creating {0}...".format(filepath))
    with open(filepath, 'w'):
        pass


def get_executor_path_or_download(executor_url):
    #get filename from url
    _executor_name = executor_url.split('/').pop(-1)

    executor_path = os.path.join(os.environ['MARVIN_DATA_PATH'], _executor_name)

    if not os.path.exists(executor_path):
        logger.info("Downloading engine executor in {0}...".format(executor_path))
        wget.download(executor_url, out=executor_path)

    return executor_path

def generate_timestamp():
    return datetime.datetime.now().timestamp()