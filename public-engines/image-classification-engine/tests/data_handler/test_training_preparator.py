#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_image_classification_engine.data_handler import TrainingPreparator


@mock.patch('marvin_image_classification_engine.data_handler.training_preparator.cv2.imwrite')
@mock.patch('marvin_image_classification_engine.data_handler.training_preparator.cv2.resize')
@mock.patch('marvin_image_classification_engine.data_handler.training_preparator.logger.info')
def test_convert_images(mocked_info, mocked_resize, mocked_imwrite):

    ac = TrainingPreparator()
    ac.convert_images(image_path='test_path', fnames=['t2'], w=150, h=150)

    mocked_info.assert_called_with('0/1')
    mocked_resize.assert_called_once()
    mocked_imwrite.assert_called_once()


@mock.patch('marvin_image_classification_engine.data_handler.training_preparator.cv2.imwrite')
@mock.patch('marvin_image_classification_engine.data_handler.training_preparator.cv2.resize')
def test_execute(mocked_resize, mocked_imwrite, mocked_params):

    test_dataset = [['t1'], ['t2']]
    mocked_params = {
        'IMAGES': 'test_images',
        'W': 150,
        'H': 150
    }

    ac = TrainingPreparator(initial_dataset=test_dataset)
    ac.execute(params=mocked_params)

    mocked_resize.assert_called()
    mocked_imwrite.assert_called()
