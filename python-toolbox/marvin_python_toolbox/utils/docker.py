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
from docker import DockerClient
from docker.errors import ImageNotFound, NotFound
from shutil import copyfile
from .misc import package_to_name, generate_engine_package, name_to_package
from .misc import get_chunk_and_untar, get_tar_data
from ..utils.log import get_logger

logger = get_logger('docker')

def _get_client(env=True, url=None):
    if env:
        return DockerClient.from_env()
    return DockerClient(base_url=url)

def rename_image(old_name, new_name):
    os.system("docker tag {} {}".format(old_name, new_name))

def _search_docker_image(name):
    client = _get_client()
    try:
        client.images.get(name)
        return True
    except ImageNotFound:
        return False

def _search_docker_container(name):
    client = _get_client()
    try:
        client.containers.get(name)
        return True
    except NotFound:
        return False

def search_docker_volume(name):
    client = _get_client()
    try:
        client.volumes.get(name)
        return True
    except NotFound:
        return False

def search_engine_container(engine_name):
    container_name = "marvin-cont-" + engine_name
    return _search_docker_container(container_name)

def search_engine_images(engine_name):
    image_name = "marvin-" + engine_name
    return _search_docker_image(image_name)

def create_engine_image(engine_name, engine_path):
    logger.info("Creating engine docker image ...")
    _package = name_to_package(engine_name)
    dockerfile_path = os.path.join(engine_path, "docker", 
                        "develop", "daemon")
    generate_engine_package(_package, engine_path)
    client = _get_client()
    client.images.build(
        path=dockerfile_path,
        tag="marvin-" + engine_name
    )
    logger.info("Creating engine docker image ... Done!")

def create_deploy_image_and_push(engine_name, engine_path, name_in_registry):
    logger.info("Creating engine deploy image ...")
    _package = name_to_package(engine_name)
    dockerfile_path = os.path.join(engine_path, "docker", 
                        "deploy", "daemon")
    generate_engine_package(_package, engine_path, 
                            dest=dockerfile_path)

    client = _get_client()
    client.images.build(
        path=dockerfile_path,
        tag=name_in_registry
    )
    logger.info("Creating engine deploy image... Done!")
    logger.info("Pushing deploy image to registry...")
    os.system("docker push {}".format(name_in_registry))
    logger.info("Pushing deploy image to registry... Done!")

def create_docker_volume(volume_name):
    client = _get_client()
    logger.info("Creating {} docker volume...".format(volume_name))
    client.volumes.create(volume_name)
    logger.info("Creating {} docker volume... Done!".format(volume_name))

def shutdown_and_delete_container(name):
    client = _get_client()
    try:
        _container = client.containers.get(name)
        _container.stop()
        logger.warning("{} container was stopped!".format(name))
        _container.remove()
        logger.info("{} container was removed!".format(name))
    except NotFound:
        logger.warning("Container not found!")

def delete_image_and_volume(name, vol):
    client = _get_client()
    try:
        client.images.remove(name)
        logger.warning("{} image was untagged, use prune to delete it permanently!".format(name))
        _vol = client.volumes.get(vol)
        _vol.remove()
        logger.warning("{} volume was deleted!".format(vol))
    except NotFound:
        logger.warning("Image or volume not found!")

def create_daemon_container(engine_name):
    logger.info("Creating engine docker container ...")

    client = _get_client()
    _engine_volume = "marvin-{}-vol".format(engine_name)
    _data_volume = "marvin-data"
    _log_volume = "marvin-log"

    client.containers.run(
        image="marvin-" + engine_name,
        name="marvin-cont-" + engine_name,
        volumes={
            _engine_volume: {
                'bind': '/home/marvin/engine',
                'mode': 'rw'
            },
            _data_volume: {
                'bind': '/home/marvin/data',
                'mode': 'rw'
            },
            _log_volume: {
                'bind': '/home/marvin/log',
                'mode': 'rw'
            }
        },
        ports={
            '22/tcp':2022,
            '50051/tcp': 50051,
            '50052/tcp': 50052,
            '50053/tcp': 50053,
            '50054/tcp': 50054,
            '50055/tcp': 50055,
            '50056/tcp': 50056,
            '50057/tcp': 50057,
            '8888/tcp': 8888
        },
        detach=True
    )

    logger.info("Creating engine docker container ... Done!")

def create_executor_container(engine_name):
    logger.info("Creating engine executor container ...")

    client = _get_client()
    _engine_volume = "marvin-{}-vol".format(engine_name)

    client.containers.run(
        image="marvin-executor",
        name="marvin-executor-" + engine_name,
        volumes={
            _engine_volume: {
                'bind': '/home/marvin/engine',
                'mode': 'rw'
            }
        },
        ports={
            '8000/tcp': 8000
        },
        detach=True
    )

    logger.info("Creating executor docker container ... Done!")

def create_tfserving_container(engine_name, model_path, serve=False):
    logger.info("Creating temporary container ...")

    _container_name = "tfserving-{}".format(engine_name)
    _dest_path = "/models/{}".format(engine_name)

    client = _get_client()
    container = client.containers.run(
        image="tensorflow/serving:1.11.1",
        name=_container_name,
        detach=True
    )
    logger.info("Creating temporary container ... Done!")

    logger.info("Writing model on image ...")
    os.system("docker cp {0} {1}:/models/{2}".format(model_path, _container_name, engine_name))

    os.system(
        "docker commit --change \'ENV MODEL_NAME {0}\' {1} tfserving:{0}"
        .format(engine_name, _container_name)
    )
    logger.info("Writing model on image ... Done!")

    logger.info("Cleaning...")
    shutdown_and_delete_container(_container_name)
    logger.info("Cleaning... Done!")

    if serve:
        logger.info("Serving...")
        client.containers.run(
            image="tensorflow/serving:{}".format(engine_name),
            name=_container_name,
            ports={
                '8500/tcp': 8500
            },
            detach=True
        )
        logger.info("Serving... Done!")


def get_stats(engine_name):
    client = _get_client()
    if search_engine_container(engine_name):
        container_name = "marvin-cont-" + engine_name
        container = client.containers.get(container_name)
        return container.stats(stream=False)
    else:
        logger.error("Engine container was not found!")
        return None

class DaemonManagement:

    data_path = '/home/marvin/data'
    log_path = '/home/marvin/log'

    def __init__(self, engine_name):
        self.engine_name = engine_name
        self.engine_path = '/home/marvin/engine'

    @staticmethod
    def get_container(engine_name):
        _engine_container = "marvin-cont-{}".format(engine_name)
        client = _get_client()
        return client.containers.get(_engine_container)

    def delete_from_daemon(self, source, folder):
        if folder:
            source = os.path.join(source, '*')
        
        _container = self.get_container(self.engine_name)
        _response = _container.exec_run("sh -c 'rm -rf {}'".format(source))

        if _response.exit_code == 0:
            logger.info("Files were deleted sucessfully!")
        else:
            logger.error("Error deleting files.")

    def list_dir_from_daemon(self, source):
        _container = self.get_container(self.engine_name)

        _response = _container.exec_run("ls {}".format(source))
        if _response.exit_code == 0:
            print(_response.output.decode())
        else:
            logger.error("Error listing files.")

    def copy_from_daemon(self, source, dest):
        _container = self.get_container(self.engine_name)
        bits, stats = _container.get_archive(source)
        logger.info("Copying file from daemon container. Size:{} bytes...".format(stats['size']))
        get_chunk_and_untar(bits, dest)
        logger.info("Copying file from daemon container. Size:{} bytes... Done!".format(stats['size']))

    def copy_to_daemon(self, source, dest, folder=True):
        _container = self.get_container(self.engine_name)
        data, filepath  = get_tar_data(source, folder)

        _response = _container.put_archive(dest, data)

        if _response:
            logger.info("File copy was sucessfull!")
        else:
            logger.error("Error in file copy.")
        #delete tmp file
        os.remove(filepath)

    def push_data(self, path):
        self.copy_to_daemon(path, self.data_path, compress, folder=False)

    def delete_data(self, file):
        _file_path = os.path.join(self.data_path, file)
        self.delete_from_daemon(_file_path, folder=False)

    def list_data_files(self):
        self.list_dir_from_daemon(self.data_path)

    def clone_engine(self):
        _dest = os.environ['MARVIN_HOME']
        _orig_folder_name = os.path.join(_dest, 'engine')
        _mod_folder_name = os.path.join(_dest, self.engine_name)
        self.copy_from_daemon(self.engine_path, _dest)
        os.rename(_orig_folder_name, _mod_folder_name)

    def push_engine(self):
        _source = os.environ['MARVIN_HOME']
        _mod_folder_name = os.path.join(_source, self.engine_name)
        _engine_folder = os.path.join(_source, 'engine')
        os.rename(_mod_folder_name, _engine_folder)
        self.delete_from_daemon(self.engine_path, folder=True)
        self.copy_to_daemon(_engine_folder, self.engine_path)

    def clone_artifacts(self):
        _dest = os.path.join(os.environ['MARVIN_DATA_PATH'], '.artifacts', self.engine_name)
        _source = os.path.join(self.data_path, '.artifacts', self.engine_name)
        self.copy_from_daemon(_source, _dest)
    
    def clone_logs(self):
        _dest = os.path.join(os.environ['MARVIN_DATA_PATH'], '.log', self.engine_name)
        self.copy_from_daemon(self.log_path, _dest)