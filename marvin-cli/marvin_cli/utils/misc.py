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
import subprocess
import tarfile
import wget
import glob
import pickle
import datetime
import time
import shutil
from cryptography.hazmat.primitives import serialization as crypto_serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.hazmat.backends import default_backend as crypto_default_backend
from .log import get_logger

logger = get_logger('misc')

def make_tarfile(output_filename, source_dir):
    with tarfile.open(output_filename, "w:gz") as tar:
        tar.add(source_dir, arcname=os.path.basename(source_dir))

def package_to_name(package):
    #remove marvin_ substring
    return package[len("marvin_"):]

def name_to_package(name):
    return "marvin_{}".format(name)

def generate_engine_package(package, path, dest=None):
    filename = package + "-" + get_version(package, path) + ".tar.gz"
    output = os.path.join('/tmp/marvin', filename)
    make_tarfile(output, path)

    if dest is not None:
        move_dest = os.path.join(dest, filename)
    else:
        move_dest = os.path.join(path, "docker", "develop" ,"daemon", filename)

    shutil.move(output, move_dest)

def get_version(package, path):
    with open(os.path.join(path, package ,"VERSION"), 'rb') as f:
        version = f.read().decode('ascii').strip()
    return version

def package_folder(input, output):
    with tarfile.open(output, "w:gz") as tar:
        tar.add(input, arcname=os.path.basename(input))

def extract_folder(input, output):
    tf = tarfile.open(input)
    tf.extractall(output)

def call_logs(engine):
    container_name = 'marvin-cont-' + engine
    p_return = subprocess.Popen(['docker', 'logs', '--follow', container_name], stdout=subprocess.PIPE)
    return p_return

def create_or_return_tmp_dir():
    tmp_path = '/tmp/marvin'
    if not os.path.exists(tmp_path):
        os.makedirs(tmp_path)
    return tmp_path

def write_tmp_info(key, info):
    _filepath = os.path.join(create_or_return_tmp_dir(), key)
    logger.info("Creating {0}...".format(key))
    with open(_filepath, 'w') as f:
        f.write(info)

def retrieve_tmp_info(key):
    _filepath = os.path.join(create_or_return_tmp_dir(), key)
    logger.info("Retriving {0}...".format(key))
    try:
        with open(_filepath, 'r') as f:
            info = f.read()
        return info
    except:
        return None

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

def create_tmp_marvin_folder():
    _dir = '/tmp/marvin'
    if not os.path.exists(_dir):
        os.makedirs(_dir)

def get_chunk_and_untar(bits, output_path):
    _dir = '/tmp/marvin'
    _tmp_path = os.path.join(_dir, 'tmp_data')
    #save tar in tmp file
    with open(_tmp_path, 'wb') as f:
        for chunk in bits:
            f.write(chunk)
        f.close()
    #extract files
    with tarfile.open(_tmp_path) as tf:
        tf.extractall(output_path)
        tf.close()
    #remove tmp_data
    os.remove(_tmp_path)

def get_tar_data(source, folder, compress):
    _dir = '/tmp/marvin'
    tmp_path = os.path.join(_dir, 'tmp_data')
    _tar_mode = "w:gz" if compress else "w"
    #save tar in tmp file
    with tarfile.open(tmp_path, _tar_mode) as tf:
        if folder:
            tf.add(source, arcname='.')
        else:
            tf.add(source, arcname=os.path.basename(source))
    
    #get bytes from file
    with open(tmp_path, 'rb') as bf:
        temp_bytes = bf.read()
        return (temp_bytes, tmp_path)

def generate_keys(engine_name):
    _key_path = os.path.join(os.environ['MARVIN_DATA_PATH'],
                            '.keys',
                            engine_name)

    os.makedirs(_key_path)

    pvk_path = os.path.join(_key_path, 'id_rsa')
    pubk_path = os.path.join(_key_path, 'id_rsa.pub')

    key = rsa.generate_private_key(
        backend=crypto_default_backend(),
        public_exponent=65537,
        key_size=2048
    )

    private_key = key.private_bytes(
        crypto_serialization.Encoding.PEM,
        crypto_serialization.PrivateFormat.PKCS8,
        crypto_serialization.NoEncryption()
    )

    public_key = key.public_key().public_bytes(
        crypto_serialization.Encoding.OpenSSH,
        crypto_serialization.PublicFormat.OpenSSH
    )

    open(pubk_path ,"w").write(public_key.decode("utf-8"))
    open(pvk_path ,"w").write(private_key.decode("utf-8"))
    os.chmod(pvk_path, 0o500)

    return pubk_path
    
def init_port_forwarding(engine_name, remote_host, ports_list, background=True):
    if remote_host != 'localhost' and remote_host != '127.0.0.1':
        pkey_path = os.path.join(os.environ['MARVIN_DATA_PATH'], '.keys', engine_name, 'id_rsa')
        command_list = ["ssh"]
        command_list.append("-o")
        command_list.append("StrictHostKeyChecking=no")
        command_list.append("-N")

        if background:
            command_list.append('-f')

        for remote_port in ports_list:
            command_list.append("-L")
            command_list.append("localhost:{0}:localhost:{0}".format(remote_port))

        command_list.append("-i")
        command_list.append("{0}".format(pkey_path))
        command_list.append("marvin@{0}".format(remote_host))
        command_list.append("-p")
        command_list.append("2022")
        if not background:
            logger.info("Press Ctrl+C to disable port forwarding")
            
        os.system(" ".join(command_list))