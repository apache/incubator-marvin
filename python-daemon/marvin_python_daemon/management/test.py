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

import sys
import os
import os.path
import subprocess
import shutil
import tempfile
import errno

from ..common.log import get_logger

logger = get_logger('management.test')


def _copy(src, dest, ignore=('.git', '.pyc', '__pycache__')):
    try:
        shutil.copytree(src, dest, ignore=shutil.ignore_patterns(*ignore))
    except OSError as e:
        if e.errno == errno.ENOTDIR:
            shutil.copy(src, dest)
        else:
            print('Directory not copied. Error: %s' % e)


def test(config, cov, no_capture, pdb, args):
    os.environ['TESTING'] = 'true'

    if args:
        args = args.split(' ')
    else:
        args = ['tests']

    if no_capture:
        args += ['--capture=no']

    if pdb:
        args += ['--pdb']

    cov_args = []
    if cov:
        cov_args += ['--cov', os.path.relpath(config['marvin_package'],
                                              start=config['base_path']),
                     '--cov-report', 'html',
                     '--cov-report', 'xml',
                     '--cov-report', 'term-missing',
                     ]

    command = ['python', '-m', 'pytest'] + cov_args + args
    print(' '.join(command))
    env = os.environ.copy()
    subprocess.call(command, cwd=config['base_path'], env=env)


def tox(config, args):
    os.environ['TESTING'] = 'true'

    if args:
        args = ['-a'] + args.split(' ')
    else:
        args = []
    # Copy the project to a tmp dir
    tmp_dir = tempfile.mkdtemp()
    tox_dir = os.path.join(tmp_dir, config['marvin_package'])
    _copy(config['base_path'], tox_dir)
    command = ['python', 'setup.py', 'test'] + args
    env = os.environ.copy()
    subprocess.call(command, cwd=tox_dir, env=env)
    shutil.rmtree(tmp_dir)


def tdd(config, cov, no_capture, pdb, partial, args):
    os.environ['TESTING'] = 'true'

    if args:
        args = args.split(' ')
    else:
        args = [os.path.relpath(
            os.path.join(config['base_path'], 'tests'))]

    if no_capture:
        args += ['--capture=no']

    if pdb:
        args += ['--pdb']

    if partial:
        args += ['--testmon']

    cov_args = []
    if cov:
        cov_args += ['--cov', os.path.relpath(config['marvin_package'],
                                              start=config['base_path']),
                     '--cov-report', 'html',
                     '--cov-report', 'xml',
                     '--cov-report', 'term-missing',
                     ]

    command = ['ptw', '-p', '--'] + cov_args + args
    print(' '.join(command))
    env = os.environ.copy()
    ptw_process = subprocess.Popen(command, cwd=config['base_path'], env=env)
    return ptw_process


def pep8(config):
    command = ['pep8', config['marvin_package']]
    exitcode = subprocess.call(command, cwd=config['base_path'])
    if exitcode == 0:
        logger.info('Congratulations! Everything looks in PEP8 standard.')
    else:
        logger.info('Error in PEP8 call')
