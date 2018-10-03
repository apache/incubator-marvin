#!/usr/bin/env python
# coding=utf-8

"""AcquisitorAndCleaner engine action.

Use this module to add the project main code.
"""
import os
import random
from random import shuffle
from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBaseDataHandler
from marvin_python_toolbox.common.data import MarvinData

__all__ = ['AcquisitorAndCleaner']


logger = get_logger('acquisitor_and_cleaner')

random.seed(123)


class AcquisitorAndCleaner(EngineBaseDataHandler):

    def __init__(self, **kwargs):
        super(AcquisitorAndCleaner, self).__init__(**kwargs)

    def read_samples(self, filename):
        with open(filename, 'r') as fp:
            samples = [line.strip().split() for line in fp.readlines()]
            shuffle(samples)
        return samples

    def execute(self, params, **kwargs):
        data = os.path.join(MarvinData.data_path, os.path.basename(params['DATA']))
        if not os.path.exists(data):
            print("Downloading...")
            data = MarvinData.download_file(url=params["DATA"])
            print("Extracting...")
            os.system('tar xvf {} --directory {}'.format(data, MarvinData.data_path))
            print("Done.")
        train = self.read_samples(os.path.join(MarvinData.data_path, params['TRAIN']))
        val = self.read_samples(os.path.join(MarvinData.data_path, params['VALID']))
        self.marvin_initial_dataset = ((train, val))
