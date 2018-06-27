#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_image_classification_engine.prediction import PredictionPreparator


@mock.patch('marvin_image_classification_engine.prediction.prediction_preparator.cv2.imread')
@mock.patch('marvin_image_classification_engine.prediction.prediction_preparator.cv2.resize')
def test_execute(mocked_imread, mocked_resize, mocked_params):

    test_message = {
        'message': 'test'
    }

    ac = PredictionPreparator(params=mocked_params)
    ac.execute(input_message=test_message, params=mocked_params)

    mocked_imread.assert_called_once()
    mocked_resize.assert_called_once()
