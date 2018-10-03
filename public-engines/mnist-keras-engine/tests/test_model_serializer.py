#!/usr/bin/env python
# coding=utf-8

try:
    import mock

except ImportError:
    import unittest.mock as mock

from marvin_mnist_keras_engine.model_serializer import ModelSerializer


@mock.patch('marvin_mnist_keras_engine.model_serializer.super')
@mock.patch('marvin_mnist_keras_engine.model_serializer.load_model')
def test__serializer_load(mocked_load, mocked_super):

    ac = ModelSerializer()
    ac._serializer_load(object_file_path="model")
    mocked_load.assert_called_once_with("model")

    ac._serializer_load(object_file_path="not_model")
    mocked_super.assert_called_once()


@mock.patch('marvin_mnist_keras_engine.model_serializer.super')
def test__serializer_dump(mocked_super):

    mocked_obj = mock.MagicMock()

    ac = ModelSerializer()
    ac._serializer_dump(obj=mocked_obj, object_file_path="model")
    mocked_obj.save.assert_called_once_with("model")

    ac._serializer_dump(obj=mocked_obj, object_file_path="not_model")
    mocked_super.assert_called_once()
