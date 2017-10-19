#!/usr/bin/env python
# coding=utf-8

"""AcquisitorAndCleaner engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBaseDataHandler

__all__ = ['AcquisitorAndCleaner']


logger = get_logger('acquisitor_and_cleaner')


class AcquisitorAndCleaner(EngineBaseDataHandler):

    def __init__(self, **kwargs):
        super(AcquisitorAndCleaner, self).__init__(**kwargs)

    def read_samples(self, filename):
        with open(filename, 'r') as fp:
            samples = [line.strip().split() for line in fp.readlines()]
        positive = [image for image, label in samples if int(label) == 1]
        negative = [image for image, label in samples if int(label) == -1]
        return {'positive': positive, 'negative': negative}

    def execute(self, **kwargs):
        train = self.read_samples(self.params['TRAIN'])
        val = self.read_samples(self.params['VALID'])
        self.initial_dataset = ((train, val))
