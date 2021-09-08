import os
import sys
import datetime
import html
import shutil
import itertools
from enum import Enum
from six.moves import builtins

from tfx import types
from tfx.components.base import base_node
from tfx.orchestration import data_types
from tfx.orchestration import metadata
from tfx.orchestration import pipeline
from tfx.orchestration.experimental.interactive import execution_result
from tfx.orchestration.experimental.interactive import notebook_formatters
from tfx.orchestration.experimental.interactive import standard_visualizations
from tfx.orchestration.experimental.interactive import visualizations
from tfx.orchestration.launcher import in_process_component_launcher
from tfx.orchestration.beam.beam_dag_runner import BeamDagRunner
from tfx.orchestration.data_types import PipelineInfo
from ml_metadata.proto import metadata_store_pb2
from ml_metadata import metadata_store

from tfx.types.standard_artifacts import Examples
from tfx.types.standard_artifacts import ExampleStatistics
from tfx.types.standard_artifacts import Schema
from tfx.types.standard_artifacts import ExampleAnomalies
from tfx.types.standard_artifacts import TransformGraph
from tfx.types.standard_artifacts import TransformCache
from tfx.types.standard_artifacts import Model
from tfx.types.standard_artifacts import ModelRun
from tfx.types.standard_artifacts import ModelEvaluation
from tfx.types.standard_artifacts import ModelBlessing
from tfx.types.standard_artifacts import PushedModel

from ..common.log import get_logger

logger = get_logger('utils.mtfx')

def check_ipython():
    if getattr(builtins, '__IPYTHON__', None):
        # __IPYTHON__ variable is set by IPython
        return True
    else:
        return False

class TfxExecutor:
    def __init__(self, 
                enable_cache,
                beam_pipeline_args,
                pipeline_name,
                pipeline_root,
                metadata_connection_config):

        self.enable_cache = enable_cache
        self.beam_pipeline_args = beam_pipeline_args
        self.pipeline_name = pipeline_name
        self.pipeline_root = pipeline_root
        self.metadata_connection_config = metadata_connection_config

    def execute(self, component, params=None):
        run_id = datetime.datetime.now().isoformat()
        pipeline_info = data_types.PipelineInfo(
            pipeline_name=self.pipeline_name,
            pipeline_root=self.pipeline_root,
            run_id=run_id)

        driver_args = data_types.DriverArgs(
            enable_cache=self.enable_cache, interactive_resolution=True)

        metadata_connection = metadata.Metadata(self.metadata_connection_config)
        beam_pipeline_args = self.beam_pipeline_args

        for name, output in component.outputs.items():
            for artifact in output.get():
                artifact.pipeline_name = self.pipeline_name
                artifact.producer_component = component.id
                artifact.name = name

        additional_pipeline_args = {}

        launcher = in_process_component_launcher.InProcessComponentLauncher.create(
                        component, pipeline_info, driver_args, metadata_connection,
                        beam_pipeline_args, additional_pipeline_args)
        
        execution_id = launcher.launch().execution_id

        result = execution_result.ExecutionResult(
                    component=component, execution_id=execution_id)

        return result

    def show(self, item):
        if check_ipython():
            from IPython.core.display import display
            from IPython.core.display import HTML
            if isinstance(item, types.Channel):
                channel = item
                artifacts = channel.get()
                for artifact in artifacts:
                    artifact_heading = 'Artifact at %s' % html.escape(artifact.uri)
                    display(HTML('<b>%s</b><br/><br/>' % artifact_heading))
                    visualization = visualizations.get_registry().get_visualization(
                        artifact.type_name)
                    if visualization:
                        visualization.display(artifact)
                    else:
                        display(item)
        else:
            logger.warning("Skipping interactive calls outside IPython environment!")

ARTIFACT_LIST = [
        'Examples',
        'ExampleStatistics',
        'Schema',
        'ExampleAnomalies',
        'TransformGraph',
        'TransformCache',
        'Model',
        'ModelRun',
        'ModelEvaluation',
        'ModelBlessing',
        'PushedModel'
    ]

ARTIFACT_TYPE_DICT = {
    'Examples': Examples,
    'ExampleStatistics': ExampleStatistics,
    'Schema': Schema,
    'ExampleAnomalies': ExampleAnomalies,
    'TransformGraph': TransformGraph,
    'TransformCache': TransformCache,
    'Model': Model,
    'ModelRun': ModelRun,
    'ModelEvaluation': ModelEvaluation,
    'ModelBlessing': ModelBlessing,
    'PushedModel': PushedModel
}

def get_channel(type_name, artifact, md_store):
        _converted_artifact = types.Artifact(
            md_store.get_artifact_type(type_name))

        _converted_artifact.set_mlmd_artifact(
            artifact)
                
        return _converted_artifact

class TfxArtifacts:
    def __init__(self, context):
        self.metadata_connection_config = context.metadata_connection_config
        self.pipeline_info = PipelineInfo(context.pipeline_name, context.pipeline_root)

    def get_artifact_by_name(self, name):
        metadata_c = metadata.Metadata(self.metadata_connection_config)
        metadata_c.__enter__()
        mlmd_context = metadata_c.get_pipeline_context(self.pipeline_info)
        outputs = metadata_c.get_published_artifacts_by_type_within_context(
            ARTIFACT_LIST, mlmd_context.id)

        md_store = metadata_store.MetadataStore(
            config=self.metadata_connection_config)

        _transformed_examples_tag = 'TransformedExamples'

        _artifact_iterator = None
        _type_name = None

        if name == _transformed_examples_tag:
            _type_name = 'Examples'
            _artifact_iterator = outputs[_type_name]
        else:
            _artifact_iterator = outputs[name]
            _type_name = name

        _converted_artifacts = []

        for _artifact in _artifact_iterator:
            if name == _transformed_examples_tag:
                if "Transform" in _artifact.uri:
                    _converted_artifacts.append(
                        get_channel('Examples', _artifact, md_store))
                else:
                    continue
            elif name == 'Examples':
                if "Transform" not in _artifact.uri:
                    _converted_artifacts.append(
                            get_channel(name, _artifact, md_store))
                else:
                    continue
            else:
                _converted_artifacts.append(
                            get_channel(name, _artifact, md_store))

        output = types.Channel(
            type=ARTIFACT_TYPE_DICT[_type_name],
            artifacts=_converted_artifacts
        )

        return output

    def get_examples(self):
        return self.get_artifact_by_name('Examples')

    def get_statistics(self):
        return self.get_artifact_by_name('ExampleStatistics')

    def get_schema(self):
        return self.get_artifact_by_name('Schema')
    
    def get_example_anomalies(self):
        return self.get_artifact_by_name('ExampleAnomalies')

    def get_transformed_examples(self):
        return self.get_artifact_by_name('TransformedExamples')

    def get_transform_graph(self):
        return self.get_artifact_by_name('TransformGraph')

    def get_transform_cache(self):
        return self.get_artifact_by_name('TransformCache')

    def get_model(self):
        return self.get_artifact_by_name('Model')

    def get_model_run(self):
        return self.get_artifact_by_name('ModelRun')

    def get_model_evaluation(self):
        return self.get_artifact_by_name('ModelEvaluation')

    def get_model_blessing(self):
        return self.get_artifact_by_name('ModelBlessing')

    def get_pushed_model(self):
        return self.get_artifact_by_name('PushedModel')

class MarvinTfxContext:

    _BASE_TMP_PATH = '/tmp/marvin/mtfx'

    def __init__(self, metadata_config=None, beam_pipeline_args=None):
        self.pipeline_name = "mtfx_{0}".format(
            os.environ['MARVIN_ENGINE_NAME'])

        self.pipeline_root = os.path.join(os.environ['MARVIN_DATA_PATH'],
                                                '.artifacts', 'mtfx', self.pipeline_name)

        if check_ipython():
            self.pipeline_root = os.path.join(self._BASE_TMP_PATH, self.pipeline_name)

        self.beam_pipeline_args = beam_pipeline_args
        
        if not os.path.exists(self.pipeline_root):
            os.makedirs(self.pipeline_root)

        self.metadata_connection_config = metadata_config

        if not metadata_config:
            metadata_sqlite_path = os.path.join(self.pipeline_root, "{}.sqlite".format(self.pipeline_name))

            logger.warning("metadata_config not provided, using default sqlite metadata path")
        
            self.metadata_connection_config = metadata.sqlite_metadata_connection_config(
                metadata_sqlite_path
            )

        self.executor = TfxExecutor(
                        enable_cache=True,
                        beam_pipeline_args=beam_pipeline_args,
                        pipeline_name=self.pipeline_name,
                        pipeline_root=self.pipeline_root,
                        metadata_connection_config=self.metadata_connection_config)

        self.outputs = None

    def run_beam_pipeline(self, pipeline_list):
        pipeline_object = pipeline.Pipeline(
                            pipeline_name=self.pipeline_name,
                            pipeline_root=self.pipeline_root,
                            components=pipeline_list,
                            metadata_connection_config=self.metadata_connection_config,
                            enable_cache=True,
                            beam_pipeline_args=self.beam_pipeline_args)
        BeamDagRunner().run(pipeline_object)

    def run(self, component, params=None):
        result = self.executor.execute(component, params)
        return result