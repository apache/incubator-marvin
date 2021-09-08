#!/usr/bin/env python
# coding=utf-8

# Copyright [2019] [Apache Software Foundation]
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


def marvin_code_export(model, **kwargs):

    import os
    import re
    import inspect
    import autopep8
    from marvin_python_daemon.common.log import get_logger
    from marvin_python_daemon.common.config import Config

    logger = get_logger('marvin.savehook')

    logger.info("Executing the marvin export hook script...")

    if model['type'] != 'notebook':
        return

    # import ipdb; ipdb.set_trace()

    cells = model['content']['cells']

    artifacts = {
        'marvin_initial_dataset': re.compile(r"(\bmarvin_initial_dataset\b)"),
        'marvin_dataset': re.compile(r"(\bmarvin_dataset\b)"),
        'marvin_model': re.compile(r"(\bmarvin_model\b)"),
        'marvin_metrics': re.compile(r"(\bmarvin_metrics\b)")
    }

    class_path = {
        "acquisitor": "data_handler{0}acquisitor_and_cleaner.py".format(os.path.sep),
        "tpreparator": "data_handler{0}training_preparator.py".format(os.path.sep),
        "trainer": "training{0}trainer.py".format(os.path.sep),
        "evaluator": "training{0}metrics_evaluator.py".format(os.path.sep),
        "ppreparator": "prediction{0}prediction_preparator.py".format(os.path.sep),
        "predictor": "prediction{0}predictor.py".format(os.path.sep),
        "feedback": "prediction{0}feedback.py".format(os.path.sep),
    }
    
    CLAZZES = {
        "acquisitor": "AcquisitorAndCleaner",
        "tpreparator": "TrainingPreparator",
        "trainer": "Trainer",
        "evaluator": "MetricsEvaluator",
        "ppreparator": "PredictionPreparator",
        "predictor": "Predictor",
        "feedback": "Feedback"
    }

    batch_exec_pattern = re.compile(
        "(def\s+execute\s*\(\s*self\s*,\s*params\s*,\s*\*\*kwargs\s*\)\s*:)")
    online_exec_pattern = re.compile(
        "(def\s+execute\s*\(\s*self\s*,\s*input_message\s*,\s*params\s*,\s*\*\*kwargs\s*\)\s*:)")

    for cell in cells:
        if cell['cell_type'] == 'code' and cell["metadata"].get("marvin_cell", False):
            source = cell["source"]
            new_source = autopep8.fix_code(
                source, options={'max_line_length': 160})

            marvin_action = cell["metadata"]["marvin_cell"]
            source_path_list = []
            site_package_clazz = getattr(__import__(Config.get("package")),
                                            CLAZZES[marvin_action])
            
            #append paths of instaled and source engine package code
            source_path_list.append(inspect.getsourcefile(site_package_clazz))
            source_path_list.append(os.path.join(os.getcwd(), 
                                    Config.get("package"), class_path[marvin_action]))

            fnew_source_lines = []
            for new_line in new_source.split("\n"):
                fnew_line = "        " + new_line + "\n" if new_line.strip() else "\n"

                if not new_line.startswith("import") and not new_line.startswith("from") and not new_line.startswith("print"):
                    for artifact in artifacts.keys():
                        fnew_line = re.sub(
                            artifacts[artifact], 'self.' + artifact, fnew_line)

                fnew_source_lines.append(fnew_line)

            if marvin_action == "predictor":
                fnew_source_lines.append("        return final_prediction\n")
                exec_pattern = online_exec_pattern

            elif marvin_action == "ppreparator":
                fnew_source_lines.append("        return input_message\n")
                exec_pattern = online_exec_pattern

            elif marvin_action == "feedback":
                fnew_source_lines.append(
                    "        return \"Thanks for the feedback!\"\n")
                exec_pattern = online_exec_pattern

            else:
                exec_pattern = batch_exec_pattern

            fnew_source = "".join(fnew_source_lines)
        
            for source_path in source_path_list:
                with open(source_path, 'r+') as fp:
                    lines = fp.readlines()
                    fp.seek(0)
                    for line in lines:
                        if re.findall(exec_pattern, line):
                            fp.write(line)
                            fp.write(fnew_source)
                            fp.truncate()

                            break
                        else:
                            fp.write(line)

                logger.info("File {0} updated!".format(source_path))

    logger.info("Finished the marvin export hook script...")


c.FileContentsManager.pre_save_hook = marvin_code_export
