#!/usr/bin/env python
# coding=utf-8

"""TrainingPreparator engine action.

Use this module to add the project main code.
"""
from .._compatibility import six
from .._logging import get_logger
from sklearn.model_selection import StratifiedShuffleSplit

from marvin_python_toolbox.engine_base import EngineBaseDataHandler

__all__ = ['TrainingPreparator']


logger = get_logger('training_preparator')


class TrainingPreparator(EngineBaseDataHandler):

    def __init__(self, **kwargs):
        super(TrainingPreparator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):

        train_no_na = self.marvin_initial_dataset['train'][params["pred_cols"] + [params["dep_var"]]].dropna()

        print("Length: {}".format(len(train_no_na)))

        # Feature Engineering
        data_X = train_no_na[params["pred_cols"]]
        data_X.loc[:, 'Sex'] = data_X.loc[:, 'Sex'].map({'male': 1, 'female': 0})
        data_y = train_no_na[params["dep_var"]]

        # Prepare for Stratified Shuffle Split
        sss = StratifiedShuffleSplit(n_splits=5, test_size=.6, random_state=0)
        sss.get_n_splits(data_X, data_y)

        # Get Test Dataset
        test_no_na = self.marvin_initial_dataset['test'][params["pred_cols"]].dropna()

        print("Length: {}".format(len(test_no_na)))

        # Feature Engineering
        test_X = test_no_na[params["pred_cols"]]
        test_X.loc[:, 'Sex'] = test_X.loc[:, 'Sex'].map({'male': 1, 'female': 0})

        self.marvin_dataset = {
            'X_train': data_X,
            'y_train': data_y,
            'X_test': test_X,
            'sss': sss
        }

        print ("Preparation is Done!!!!")

