#!/bin/bash

SCRIPTPATH="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 ; pwd -P )"

kubectl apply -f $SCRIPTPATH/{{cookiecutter.engine_name}}_deployment.yaml
kubectl apply -f $SCRIPTPATH/{{cookiecutter.engine_name}}_service.yaml