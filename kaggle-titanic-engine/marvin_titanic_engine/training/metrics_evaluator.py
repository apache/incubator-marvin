#!/usr/bin/env python
# coding=utf-8

"""MetricsEvaluator engine action.

Use this module to add the project main code.
"""

from .._compatibility import six 
from .._logging import get_logger
from sklearn import metrics
from six import iteritems
import numpy as np

from marvin_python_toolbox.engine_base import EngineBaseTraining

__all__ = ['MetricsEvaluator']


logger = get_logger('metrics_evaluator')


class MetricsEvaluator(EngineBaseTraining):

    def __init__(self, **kwargs):
        super(MetricsEvaluator, self).__init__(**kwargs)

    def execute(self, params, **kwargs):

        all_metrics = {}

        _model = self.marvin_model
        for model_type, fitted_model in iteritems(_model):

            y_predicted = fitted_model.predict(self.marvin_dataset['X_train'])

            all_metrics[model_type] = {}
            all_metrics[model_type]["report"] = metrics.classification_report(y_predicted, self.marvin_dataset['y_train'])
            all_metrics[model_type]["confusion_matrix"] = metrics.confusion_matrix(y_predicted, self.marvin_dataset['y_train']).tolist()

            # Print the classification report of `y_test` and `predicted`
            print("Classification Report:\n")
            print(all_metrics[model_type]["report"])

            # Print the confusion matrix
            print("Confusion Matrix:\n")
            print(all_metrics[model_type]["confusion_matrix"])
            print("\n\n")

        importances = _model["rf"].best_estimator_.feature_importances_
        indices = np.argsort(importances)[::-1]

        # Print the feature ranking
        print("Feature ranking:")

        all_metrics["feature_ranking"] = []
        for f in range(self.marvin_dataset['X_train'].shape[1]):
            all_metrics["feature_ranking"].append((f + 1, params["pred_cols"][indices[f]], importances[indices[f]]))
            print("%d. feature %s (%f)" % all_metrics["feature_ranking"][f])

        self.marvin_metrics = all_metrics
