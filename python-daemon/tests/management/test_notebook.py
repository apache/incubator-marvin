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

try:
    import mock
except ImportError:
    import unittest.mock as mock

import os
from marvin_python_daemon.management.notebook import notebook, lab


mocked_config = {
    'base_path': '/tmp'
}

os.environ['MARVIN_DAEMON_PATH'] = '/tmp/marvin/marvin_python_daemon'
os.environ['MARVIN_ENGINE_PATH'] = '/tmp/marvin'


@mock.patch('marvin_python_daemon.management.notebook.sys')
@mock.patch('marvin_python_daemon.management.notebook.os.system')
def test_notebook(system_mocked, sys_mocked):
    config = mocked_config
    port = '8888'
    enable_security = False
    allow_root = False
    spark_conf = '/opt/spark/conf'
    system_mocked.return_value = 1

    notebook(config, enable_security, port)

    system_mocked.assert_called_once_with("SPARK_CONF_DIR={0} YARN_CONF_DIR={0} jupyter notebook --notebook-dir /tmp/notebooks --ip 0.0.0.0 --port 8888 --no-browser --config ".format(os.path.join(os.environ["SPARK_HOME"], "conf")) +
                                          os.environ["MARVIN_ENGINE_PATH"] + '/marvin_python_daemon/extras/notebook_extensions/jupyter_notebook_config.py --allow-root')


@mock.patch('marvin_python_daemon.management.notebook.sys')
@mock.patch('marvin_python_daemon.management.notebook.os.system')
def test_notebook_with_security(system_mocked, sys_mocked):
    config = mocked_config
    port = '8888'
    enable_security = True
    system_mocked.return_value = 1

    notebook(config, enable_security, port)

    system_mocked.assert_called_once_with("SPARK_CONF_DIR={0} YARN_CONF_DIR={0} jupyter notebook --notebook-dir /tmp/notebooks --ip 0.0.0.0 --port 8888 --no-browser --config ".format(os.path.join(os.environ["SPARK_HOME"], "conf")) +
                                          os.environ["MARVIN_ENGINE_PATH"] + '/marvin_python_daemon/extras/notebook_extensions/jupyter_notebook_config.py --NotebookApp.token= --allow-root')


@mock.patch('marvin_python_daemon.management.notebook.sys')
@mock.patch('marvin_python_daemon.management.notebook.os.system')
def test_jupyter_lab(system_mocked, sys_mocked):
    config = mocked_config
    port = '8888'
    enable_security = False
    spark_conf = '/opt/spark/conf'
    system_mocked.return_value = 1

    lab(config, enable_security, port)

    system_mocked.assert_called_once_with(
        "SPARK_CONF_DIR={0} YARN_CONF_DIR={0} jupyter-lab --notebook-dir /tmp/notebooks --ip 0.0.0.0 --port 8888 --no-browser --allow-root".format(os.path.join(os.environ["SPARK_HOME"], "conf")))


@mock.patch('marvin_python_daemon.management.notebook.sys')
@mock.patch('marvin_python_daemon.management.notebook.os.system')
def test_jupyter_lab_with_security(system_mocked, sys_mocked):
    config = mocked_config
    port = '8888'
    enable_security = True
    system_mocked.return_value = 1

    lab(config, enable_security, port)

    system_mocked.assert_called_once_with(
        "SPARK_CONF_DIR={0} YARN_CONF_DIR={0} jupyter-lab --notebook-dir /tmp/notebooks --ip 0.0.0.0 --port 8888 --no-browser --NotebookApp.token= --allow-root".format(os.path.join(os.environ["SPARK_HOME"], "conf")))
