#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

import numpy as np
from marvin_image_classification_engine.training import MetricsEvaluator


@mock.patch('marvin_image_classification_engine.training.metrics_evaluator.sk_metrics.accuracy_score')
@mock.patch('marvin_image_classification_engine.training.metrics_evaluator.cv2.imread')
def test_execute(mocked_imread, mocked_score, mocked_params):

    test_data = {
        'train': ['t0'],
        'val': ['t1']
    }

    mocked_params = {
        'TEST_STEPS': 20
    }

    mocked_imread.return_value = np.array([[[0, 1, 2], [1,2, 3], [2,3, 4]], [[0, 1, 2], [1,2, 3], [2,3, 4]], [[0, 1, 2], [1,2, 3], [2,3, 4]]])

    mocked_model = mock.MagicMock()

    ac = MetricsEvaluator(model=mocked_model, dataset=test_data)
    ac.execute(params=mocked_params)

    mocked_imread.assert_called_once()
    mocked_score.assert_called_once()
