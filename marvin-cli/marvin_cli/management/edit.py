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

@click.group("edit")
def cli():
    pass

@cli.command("edit-config", help="Edit configuration.")
@click.pass_context
def config(ctx):
    filepath = os.path.join(os.environ['MARVIN_DATA_PATH'], '.conf', 'cli_conf.json')
    os.system(ctx.obj['editor'] + ' ' + filepath)
