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

from abc import ABCMeta
from ..common.log import get_logger

from .engine_base_action import EngineBaseBatchAction


__all__ = ['EngineBaseDataHandler']
logger = get_logger('engine_base_data_handler')


class EngineBaseDataHandler(EngineBaseBatchAction):
    __metaclass__ = ABCMeta

    _initialdataset = None
    _dataset = None

    def __init__(self, **kwargs):
        self._initialdataset = self._get_arg(
            kwargs=kwargs, arg='initial_dataset')
        self._dataset = self._get_arg(kwargs=kwargs, arg='dataset')
        super(EngineBaseDataHandler, self).__init__(**kwargs)

    @property
    def marvin_initial_dataset(self):
        logger.info("initial_dataset loaded.")
        return self._load_obj(object_reference='_initialdataset')

    @marvin_initial_dataset.setter
    def marvin_initial_dataset(self, initial_dataset):
        logger.info("initial_dataset saved.")
        self._save_obj(object_reference='_initialdataset',
                       obj=initial_dataset)

    @property
    def marvin_dataset(self):
        logger.info("dataset loaded.")
        return self._load_obj(object_reference='_dataset')

    @marvin_dataset.setter
    def marvin_dataset(self, dataset):
        logger.info("dataset saved.")
        self._save_obj(object_reference='_dataset', obj=dataset)
