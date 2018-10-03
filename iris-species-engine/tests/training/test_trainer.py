#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

import pandas as pd
from marvin_iris_species_engine.training import Trainer


@mock.patch('marvin_iris_species_engine.training.trainer.svm.SVC')
@mock.patch('marvin_iris_species_engine.training.trainer.LogisticRegression')
@mock.patch('marvin_iris_species_engine.training.trainer.DecisionTreeClassifier')
@mock.patch('marvin_iris_species_engine.training.trainer.KNeighborsClassifier')
def test_execute(knn_mocked, dt_mocked, lr_mocked, svc_mocked, mocked_params):

    data_petals_x = {
        'PetalLengthCm': [5, 6],
        'PetalWidthCm': [7, 8],
        }

    data_sepals_x = {
        'SepalLengthCm': [1, 2],
        'SepalWidthCm': [3, 4],
        }

    data_joined_x = {
        'SepalLengthCm': [1, 2],
        'SepalWidthCm': [3, 4],
        'PetalLengthCm': [5, 6],
        'PetalWidthCm': [7, 8],
    }

    data_petals_y = {'Species': ['species1']}
    data_sepals_y = {'Species': ['species1']}
    data_joined_y = {'Species': ['species1']}

    train_x_p = pd.DataFrame(data=data_petals_x)
    train_y_p = pd.DataFrame(data=data_petals_y)
    test_x_p = pd.DataFrame(data=data_petals_x)
    test_y_p = pd.DataFrame(data=data_petals_y)

    train_x_s = pd.DataFrame(data=data_sepals_x)
    train_y_s = pd.DataFrame(data=data_sepals_y)
    test_x_s = pd.DataFrame(data=data_sepals_x)
    test_y_s = pd.DataFrame(data=data_sepals_y)

    train_X = pd.DataFrame(data=data_joined_x)
    train_y = pd.DataFrame(data=data_joined_y)
    test_X = pd.DataFrame(data=data_joined_x)
    test_y = pd.DataFrame(data=data_joined_y)

    data_source = {
        'petals': {'train_X': train_x_p, 'train_y': train_y_p, 'test_X': test_x_p, 'test_y': test_y_p},
        'sepals': {'train_X': train_x_s, 'train_y': train_y_s, 'test_X': test_x_s, 'test_y': test_y_s},
        'joined': {'train_X': train_X, 'train_y': train_y, 'test_X': test_X, 'test_y': test_y}
        }

    ac = Trainer(dataset=data_source)
    ac.execute(params=mocked_params)

    knn_mocked.assert_called()
    dt_mocked.assert_called()
    lr_mocked.assert_called()
    svc_mocked.assert_called()
