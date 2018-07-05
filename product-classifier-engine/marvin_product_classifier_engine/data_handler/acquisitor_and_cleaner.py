#!/usr/bin/env python
# coding=utf-8

"""AcquisitorAndCleaner engine action.

Use this module to add the project main code.
"""

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

        initial_dataset = pd.read_csv(MarvinData.download_file("https://s3.amazonaws.com/automl-example/produtos.csv"), delimiter=";", encoding='utf-8')
        initial_dataset["text"] = initial_dataset["nome"] + " " + initial_dataset["descricao"]
        initial_dataset.drop(["descricao", "nome"], axis=1, inplace=True)
        initial_dataset.dropna(inplace=True)

        self.marvin_initial_dataset = initial_dataset

