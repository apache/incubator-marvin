#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

import numpy as np
from marvin_image_classification_engine.training import Trainer


@mock.patch('marvin_image_classification_engine.training.trainer.Sequential')
def test_build_model(mocked_model):

    ac = Trainer()
    ac.build_model(w=150, h=150, trainable=True)

    mocked_model.assert_called_once()


@mock.patch('marvin_image_classification_engine.training.trainer.Sequential.fit_generator')
@mock.patch('marvin_image_classification_engine.training.trainer.Sequential.compile')
@mock.patch('marvin_image_classification_engine.training.trainer.cv2.imread')
def test_execute(mocked_imread, mocked_compile, mocked_fit, mocked_params):

    mocked_params = {
        'LEARNING_RATE': 0.001,
        'MOMENTUM': 0.09,
        'STEPS': 2,
        'EPOCHS': 1,
        'VAL_STEPS': 2
    }

    test_data = {
        'train': ['t0'],
        'val': ['t1']
    }

    mocked_imread.return_value = np.array([[[0, 1, 2], [1,2, 3], [2,3, 4]], [[0, 1, 2], [1,2, 3], [2,3, 4]], [[0, 1, 2], [1,2, 3], [2,3, 4]]])

    ac = Trainer(dataset=test_data)
    ac.execute(params=mocked_params)

    mocked_compile.assert_called_once()
    mocked_fit.assert_called_once()
