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

import json
import os
import sys
import time
import os.path
import subprocess
import multiprocessing
from ..common.profiling import profiling
from ..common.data import MarvinData
from ..common.log import get_logger
from ..common.config import Config, load_conf_from_file

logger = get_logger('management.engine')

CLAZZES = {
    "acquisitor": "AcquisitorAndCleaner",
    "tpreparator": "TrainingPreparator",
    "trainer": "Trainer",
    "evaluator": "MetricsEvaluator",
    "ppreparator": "PredictionPreparator",
    "predictor": "Predictor",
    "feedback": "Feedback"
}

ARTIFACTS = {
    "AcquisitorAndCleaner": [],
    "TrainingPreparator": ["initialdataset"],
    "Trainer": ["dataset"],
    "MetricsEvaluator": ["model"],
    "PredictionPreparator": ["model", "metrics"],
    "Predictor": ["model", "metrics"],
    "Feedback": []
}

def dryrun(config, action, profiling):

    # setting spark configuration directory
    os.environ["SPARK_CONF_DIR"] = os.path.join(
        os.environ["SPARK_HOME"], "conf")
    os.environ["YARN_CONF_DIR"] = os.environ["SPARK_CONF_DIR"]

    params = read_file('engine.params')
    messages_file = read_file('engine.messages')
    feedback_file = read_file('feedback.messages')

    if action == 'all':
        pipeline = ['acquisitor', 'tpreparator', 'trainer',
                    'evaluator', 'ppreparator', 'predictor', 'feedback']
    else:
        pipeline = [action]

    _dryrun = MarvinDryRun(config=config, messages=[
                           messages_file, feedback_file])

    initial_start_time = time.time()

    for step in pipeline:
        _dryrun.execute(clazz=CLAZZES[step],
                        params=params, profiling_enabled=profiling)

    logger.info("Total Time : {:.2f}s".format(
        time.time() - initial_start_time))


class MarvinDryRun(object):
    def __init__(self, config, messages):
        self.predictor_messages = messages[0]
        self.feedback_messages = messages[1]
        self.pmessages = []
        self.package_name = config['marvin_package']
        self.kwargs = None

    def execute(self, clazz, params, profiling_enabled=False):
        self.print_start_step(clazz)

        _Step = dynamic_import("{}.{}".format(self.package_name, clazz))

        if not self.kwargs:
            self.kwargs = generate_kwargs(self.package_name, _Step, params)

        step = _Step(**self.kwargs)

        def call_online_actions(step, msg, msg_idx):
            if profiling_enabled:
                with profiling(output_path=".profiling", uid=clazz) as prof:
                    result = step.execute(input_message=msg, params=params)

                prof.disable
                logger.info(
                    "\nProfile images created in {}\n".format(prof.image_path))

            else:
                result = step.execute(input_message=msg, params=params)

            return result

        if clazz == 'PredictionPreparator':
            for idx, msg in enumerate(self.predictor_messages):
                self.pmessages.append(call_online_actions(step, msg, idx))

        elif clazz == 'Feedback':
            for idx, msg in enumerate(self.feedback_messages):
                self.pmessages.append(call_online_actions(step, msg, idx))

        elif clazz == 'Predictor':

            self.execute("PredictionPreparator", params)

            self.pmessages = self.messages if not self.pmessages else self.pmessages

            for idx, msg in enumerate(self.pmessages):
                call_online_actions(step, msg, idx)

        else:
            if profiling_enabled:
                with profiling(output_path=".profiling", uid=clazz) as prof:
                    step.execute(params=params)

                prof.disable

                logger.info(
                    "\nProfile images created in {}\n".format(prof.image_path))

            else:
                step.execute(params=params)

        self.print_finish_step()

    def print_finish_step(self):
        logger.info("STEP TAKES {:.4f} (seconds) ".format(
            (time.time() - self.start_time)))

    def print_start_step(self, name):
        logger.info("MARVIN DRYRUN - STEP [{}]".format(name))
        self.start_time = time.time()


def dynamic_import(clazz):
    components = clazz.split('.')
    mod = __import__(components[0])
    for comp in components[1:]:
        mod = getattr(mod, comp)
    return mod


def read_file(filename):
    fname = os.path.join("", filename)
    if os.path.exists(fname):

        logger.info("Engine file {} loaded!".format(filename))

        with open(fname, 'r') as fp:
            return json.load(fp)
    else:
        logger.info("Engine file {} doesn't exists...".format(filename))
        return {}


def generate_kwargs(package_name, clazz, params=None, initial_dataset='initialdataset', dataset='dataset', model='model', metrics='metrics'):
    kwargs = {}

    kwargs["persistence_mode"] = 'local'
    kwargs["default_root_path"] = os.path.join(
        os.getenv('MARVIN_DATA_PATH'), '.artifacts')
    kwargs["is_remote_calling"] = True

    _artifact_folder = package_name.replace(
            'marvin_', '').replace('_engine', '')
    _artifacts_to_load = ARTIFACTS[clazz.__name__]

    if params:
        kwargs["params"] = params
    if dataset in _artifacts_to_load:
        kwargs["dataset"] = clazz.retrieve_obj(os.path.join(kwargs["default_root_path"],
                                                _artifact_folder, dataset))
    if initial_dataset in _artifacts_to_load:
        kwargs["initial_dataset"] = clazz.retrieve_obj(os.path.join(kwargs["default_root_path"],
                                                _artifact_folder, initial_dataset))
    if model in _artifacts_to_load:
        kwargs["model"] = clazz.retrieve_obj(os.path.join(kwargs["default_root_path"],
                                                _artifact_folder, model))
    if metrics in _artifacts_to_load:
        kwargs["metrics"] = clazz.retrieve_obj(os.path.join(kwargs["default_root_path"],
                                                _artifact_folder, metrics))

    return kwargs


class MarvinEngineServer(object):
    @classmethod
    def create(self, config, action, port, workers, rpc_workers, params, pipeline):
        package_name = config['marvin_package']

        def create_object(act):
            clazz = CLAZZES[act]
            _Action = dynamic_import("{}.{}".format(package_name, clazz))
            kwargs = generate_kwargs(package_name, _Action, params)
            return _Action(**kwargs)

        root_obj = create_object(action)
        previous_object = root_obj

        if pipeline:
            for step in list(reversed(pipeline)):
                previous_object._previous_step = create_object(step)
                previous_object = previous_object._previous_step

        server = root_obj._prepare_remote_server(
            port=port, workers=workers, rpc_workers=rpc_workers)

        logger.info(
            "Starting GRPC server [{}] for {} Action".format(port, action))
        server.start()

        return server


def engine_server(config, action, max_workers, max_rpc_workers):

    logger.info("Starting server ...")

    # setting spark configuration directory
    os.environ["SPARK_CONF_DIR"] = os.path.join(
        os.environ["SPARK_HOME"], "conf")
    os.environ["YARN_CONF_DIR"] = os.environ["SPARK_CONF_DIR"]

    params = read_file('engine.params')
    metadata = read_file('engine.metadata')

    default_actions = {action['name']
        : action for action in metadata['actions']}

    if action == 'all':
        action = default_actions
    else:
        action = {action: default_actions[action]}

    servers = []
    for action_name in action.keys():
        # initializing server configuration
        engine_server = MarvinEngineServer.create(
            config=config,
            action=action_name,
            port=action[action_name]["port"],
            workers=max_workers,
            rpc_workers=max_rpc_workers,
            params=params,
            pipeline=action[action_name]["pipeline"]
        )

        servers.append(engine_server)

    return servers
