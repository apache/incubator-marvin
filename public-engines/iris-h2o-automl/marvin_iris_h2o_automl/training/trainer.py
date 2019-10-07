#!/usr/bin/env python
# coding=utf-8

"""Trainer engine action.

Use this module to add the project main code.
"""

from .._compatibility import six
from .._logging import get_logger

from marvin_python_toolbox.engine_base import EngineBaseTraining
from ..model_serializer import ModelSerializer

__all__ = ['Trainer']


logger = get_logger('trainer')


class Trainer(ModelSerializer, EngineBaseTraining):

    def __init__(self, **kwargs):
        super(Trainer, self).__init__(**kwargs)

    def execute(self, params, **kwargs):
        import h2o
        from h2o.automl import H2OAutoML

        h2o.init()

        train_X_frame = h2o.H2OFrame.from_python(self.marvin_dataset['train_X'])
        test_X_frame = h2o.H2OFrame.from_python(self.marvin_dataset['test_X'])

        x = train_X_frame.columns
        y = 'Species'
        x.remove(y)

        automl = H2OAutoML(max_models=20, seed=1)
        automl.train(x=x,
                     y=y,
                     training_frame=train_X_frame)

        self.marvin_model = automl

