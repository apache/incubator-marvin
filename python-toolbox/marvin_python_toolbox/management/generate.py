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
import click
import pathlib
from cookiecutter.main import cookiecutter
from ..utils.log import get_logger

logger = get_logger('management.generate')

@click.group("generate")
def cli():
    pass

AIRFLOW_TEMPLATE = os.path.join(pathlib.Path(__file__).parent.absolute(), 'airflow_template')

@cli.command("generate-dag", help="Generate Airflow DAG.")
@click.option('--name', '-n', prompt='DAG name', default='marvin-dag', help='DAG name')
@click.option('--host', '-h', prompt='Host', default='http://localhost:8000',
                help='Hostname with port.')
@click.option('--owner', '-o', prompt='Owner', default='Marvin',
                help='Owner of the DAG.')
@click.option('--interval-days', '-i', prompt='Interval days', default='1',
                help='DAG executiom interval in days.')
@click.option('--timeout', '-t', prompt='Timeout', default='60',
                help='Timeout in minutes.')
@click.option('--retries', '-r', prompt='Retries', default='3',
                help='# of task each retries.')
@click.option('--path', '-p', help='Output path to file.', 
                default=os.getcwd(), type=click.Path(exists=True))
def generate_dag(name, host, owner, interval_days, timeout, retries, path):
    _dest = path
    _extras_dir = {
        'dag_name': name,
        'host': host,
        'owner': owner,
        'interval_days': interval_days,
        'timeout': timeout,
        'retries': retries
    }
    cookiecutter(AIRFLOW_TEMPLATE, output_dir=_dest, extra_context=_extras_dir, no_input=True)