#!/usr/bin/env python
# coding=utf-8

# Copyright [2019] [Apache Software Foundation]
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
import click
import time
import os
import json
import pandas as pd
from google.cloud import bigquery
import hashlib

from .._logging import get_logger

from .._compatibility import six


logger = get_logger('management.bigquery')


@click.group('bigquery')
def cli():
    pass

def read_file(filename):
    fname = os.path.join("", filename)
    if os.path.exists(fname):

        print("Engine file {} loaded!".format(filename))

        with open(fname, 'r') as fp:
            return json.load(fp)
    else:
        print("Engine file {} doesn't exists...".format(filename))
        return {}

@cli.command(
    'bigquery-dataimport',
    help='Import data samples from a BigQuery database.')
@click.option('--dryrun', '-d', is_flag=True, help='If it must run just to estimate costs')
@click.option('--max_billed', '-m', default=10, help='Max bytes to be billed in the queries')
@click.option('--metadata-file', '-mf', default='engine.metadata', help='Marvin engine metadata file path', type=click.Path(exists=True))
@click.pass_context
def bigquery_dataimport_cli(ctx, dryrun, max_billed, metadata_file):
    bigquery_dataimport(ctx, metadata_file, dryrun, max_billed)

def bigquery_dataimport(ctx, metadata_file, dryrun, max_billed):

    initial_start_time = time.time()

    metadata = read_file(metadata_file)

    if metadata:
        print(chr(27) + "[2J")
        
        data_path = os.environ['MARVIN_DATA_PATH']
        path_csvs = data_path + '/bigquery-' + metadata['bigquery_project']
        
        if not dryrun:
            os.mkdir(path_csvs)
        
        for query, file in zip(metadata['bigquery_queries'], metadata['bigquery_csvfiles']):
            
            print("project: {} query: {} file: {}".format(metadata['bigquery_project'], query, file))
            
            bdi = BigQueryImporter(
                        project=metadata['bigquery_project'],
                        sql=query,
                        file=file,
                        # transform max_billed in bytes
                        max_billed=max_billed * 1073741824,
                        path_csv = path_csvs
                        
                    )
            if dryrun:
                bdi.dryrun()
            else:
                bdi.query()
                
        print("Total Time : {:.2f}s".format(time.time() - initial_start_time))

        print("\n")


def read_config(filename):
    fname = os.path.join("", filename)
    if os.path.exists(fname):
        with open(fname, 'r') as fp:
            return json.load(fp)[0]
    else:
        print("Configuration file {} doesn't exists...".format(filename))
        return {}


class BigQueryImporter():
    def __init__(self, project, sql, file, max_billed, path_csv):
        self.project = project
        self.sql = sql
        self.file = file
        self.max_billed = max_billed
        self.path_csv = path_csv

    def query(self):
        job_config = bigquery.QueryJobConfig()
        job_config.use_query_cache = False
        job_config.maximum_bytes_billed=self.max_billed
        client = bigquery.Client(project=self.project)
        query_job = client.query(self.sql,
                                 job_config=job_config)
        dataframe = query_job.to_dataframe()
        dataframe.to_csv(self.path_csv + '/' + self.file, index=False)
        
    def dryrun(self):
        job_config = bigquery.QueryJobConfig()
        job_config.use_query_cache = False
        job_config.dry_run = True
        job_config.maximum_bytes_billed=self.max_billed
        client = bigquery.Client(project=self.project)
        query_job = client.query(self.sql,
                                 job_config=job_config)
        
        assert query_job.state == "DONE"
        assert query_job.dry_run
        
        print("The query: {}\nWill process {} Gb.\n".format(self.sql, query_job.total_bytes_processed / 1073741824))
