#!/usr/bin/env python
# coding=utf-8

"""AcquisitorAndCleaner engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger
from marvin_python_toolbox.engine_base import EngineBaseDataHandler
from marvin_python_toolbox.common.data import MarvinData
import pandas as pd


__all__ = ['AcquisitorAndCleaner']


logger = get_logger('acquisitor_and_cleaner')


class AcquisitorAndCleaner(EngineBaseDataHandler):

    def __init__(self, **kwargs):
        super(AcquisitorAndCleaner, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        data_file = MarvinData.download_file("https://s3.amazonaws.com/marvin-engines-data/spam.csv")
        data = pd.read_csv(data_file, encoding='latin-1')
        data = data.drop(["Unnamed: 2", "Unnamed: 3", "Unnamed: 4"], axis=1)
        data = data.rename(columns={"v1": "label", "v2": "text"})
        data['label_num'] = data.label.map({'ham': 0, 'spam': 1})

        self.marvin_initial_dataset = data

