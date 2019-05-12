## Changes log

### 0.0.5

   - Artifact reload bug fix #3 #5
   - Dependency Links Deprecation & Remove #14
   - Dependencies version fix for MacOS env #17
   - General contribution file update #10
   - CI update
     - Move Travis CI out from sub-directory #1 #5
     - Travis CI retry #6
     - Travis CI sbt batch mode #7 #8
     - Pytest-Cov Version Requirement Update #9
   - Readme update
     - Tutorial book link update #2 #4 #11
     - Install links update to Apache Website #12
   - License Update
     - Move License out for Github #13

### 0.0.4

   - Docs.yaml file update for metrics api
   - Some adjustments in toolbox and template makefiles #104
   - Removing some commands by install mode (dev and prod) #104
   - Moving autocomplete and notebook extension from toolbox setup to engine template setup. Close #107
   - Separating tests dependencies and creating a new make command. close #100
   - Metrics as json and Keras serializser to Closes #86 and Closes #98
   - Saving and loading metrics artifacts as json files to Fix #98
   - Adding a symlink to the data path on engine generate. close #93
   - Marvin is now installable with pip. fix #84
   - ASCII encode error fix for accented words in predict message
   - Add Jupyter Lab command. Fix #85
   - Cli parameter conflit fix
   - New param to force reload #80
   - Improving test coverage
   - New python binary parameter to be used in the creation of virtual env
   - Fix tornado 4.5.3 and pip 9.0.1

### 0.0.3

	- Python 3 support general compatibility refactoring (#68)
    - Add marvin_ prefix in artefacts getters and setters to avoid user code conflicts   
    - Fixing #66 bug related to override the params default values
    - Refact artifacts setter and getter in engine templates
    - Making marvin.ini from toolbox be found by default
    - Making "params" as an execute method parameter to be possible to overriden default values in runtime
    - Enabling to inform extra parameters for executor's jvm customization. Fix #65
    - Improve spark conf parameter usage in cli's commands to use SPARK_CONF_DIR and SPARK_HOME envs.
    - Not use json dumps if response type is string. Fixed #67
    - Adding gitter tag to README file.
    - Remove deploy to pipy from build
    - Install twine in distribution task
    - Add --process-dependency-links in pip install command
    - General bug fixes

### 0.0.2

    - change executor vm parameter from modelProtocol to protocol
    - Generic Dockerfile template and make commands to be used to build, run and push containers    
    - fix spark conf dir parameter bug
    - create distribute task to simplify the pypi package distribution.

### 0.0.1

 - initial version
