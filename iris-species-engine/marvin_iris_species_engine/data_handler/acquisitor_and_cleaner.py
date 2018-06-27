#!/usr/bin/env python
# coding=utf-8

"""AcquisitorAndCleaner engine action.

Use this module to add the project main code.
"""

import pandas as pd
from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBaseDataHandler
from marvin_python_toolbox.common.data import MarvinData

__all__ = ['AcquisitorAndCleaner']


logger = get_logger('acquisitor_and_cleaner')


class AcquisitorAndCleaner(EngineBaseDataHandler):

    def __init__(self, **kwargs):
        super(AcquisitorAndCleaner, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        file_path = MarvinData.download_file(url=params["data_url"])

        iris = pd.read_csv(file_path)
        iris.drop('Id', axis=1, inplace=True)
        self.marvin_initial_dataset = iris
