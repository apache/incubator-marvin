#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

import pandas as pd
from sklearn.svm import SVC
from marvin_iris_species_engine.prediction import Predictor


class TestPredictor:
    def test_execute(mocked_params):

        data_petals_x = {
            'PetalLengthCm': [5, 6],
            'PetalWidthCm': [7, 8],
        }

        data_petals_y = {'Species': ['species1', 'species2']}

        train_x = pd.DataFrame(data=data_petals_x)
        train_y = pd.DataFrame(data=data_petals_y)
        test_x = pd.DataFrame(data=data_petals_x)
        test_y = pd.DataFrame(data=data_petals_y)

        data_source = {
            'train_X': train_x,
            'train_y': train_y,
            'test_X': test_x,
            'test_y': test_y
        }

        svm_mocked = SVC().fit(data_source['train_X'], data_source['train_y'])

        model_mocked = {
            'svm_petals': svm_mocked,
            'svm_sepals': svm_mocked
        }

        ac = Predictor(model=model_mocked)
        ac.execute(input_message=[5, 6], params=mocked_params)
