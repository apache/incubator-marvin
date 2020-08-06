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

from __future__ import print_function

import os
import subprocess
import sys

from ..common.log import get_logger

logger = get_logger('management.notebook')


def notebook(config, enable_security, port):
    notebookdir = os.path.join(config['base_path'], 'notebooks')
    command = [
        "SPARK_CONF_DIR={0} YARN_CONF_DIR={0}".format(
            os.path.join(os.environ["SPARK_HOME"], "conf")),
        'jupyter', 'notebook',
        '--notebook-dir', notebookdir,
        '--ip', '0.0.0.0',
        '--port', port,
        '--no-browser',
        '--config', os.path.join(os.environ["MARVIN_DAEMON_PATH"],
                                 'extras', 'notebook_extensions', 'jupyter_notebook_config.py')
    ]

    command.append("--NotebookApp.token=") if not enable_security else None
    command.append("--allow-root")

    return_code = os.system(' '.join(command))
    logger.info("Notebook call returned {0}".format(str(return_code)))


def lab(config, enable_security, port):
    notebookdir = os.path.join(config['base_path'], 'notebooks')
    command = [
        "SPARK_CONF_DIR={0} YARN_CONF_DIR={0}".format(
            os.path.join(os.environ["SPARK_HOME"], "conf")),
        'jupyter-lab',
        '--notebook-dir', notebookdir,
        '--ip', '0.0.0.0',
        '--port', port,
        '--no-browser'
    ]

    command.append("--NotebookApp.token=") if not enable_security else None
    command.append("--allow-root")

    return_code = os.system(' '.join(command))
    logger.info("Lab call returned {0}".format(str(return_code)))
