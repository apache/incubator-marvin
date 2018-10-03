#!/usr/bin/env python
# coding=utf-8

"""AcquisitorAndCleaner engine action.

Use this module to add the project main code.
"""

import pandas as pd

from .._compatibility import six
from .._logging import get_logger
from marvin_python_toolbox.common.data import MarvinData
import pandas as pd

from marvin_python_toolbox.engine_base import EngineBaseDataHandler

__all__ = ['AcquisitorAndCleaner']


logger = get_logger('acquisitor_and_cleaner')


class AcquisitorAndCleaner(EngineBaseDataHandler):

    def __init__(self, **kwargs):
        super(AcquisitorAndCleaner, self).__init__(**kwargs)

    def execute(self, params, **kwargs):

        train_df = pd.read_csv(MarvinData.download_file("https://s3.amazonaws.com/marvin-engines-data/titanic/train.csv"))
        test_df = pd.read_csv(MarvinData.download_file("https://s3.amazonaws.com/marvin-engines-data/titanic/test.csv"))

        print ("{} samples to train with {} features...".format(train_df.shape[0], train_df.shape[1]))
        print ("{} samples to test...".format(test_df.shape[0]))

        self.marvin_initial_dataset = {
            'train': train_df,
            'test': test_df
        }

