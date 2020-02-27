# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
# 
#     http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

ARG BASE_TAG=latest
FROM marvin-base:${BASE_TAG}

##############################################################
# Define all environment variables to be used 
##############################################################
ENV WORKON_HOME=$MARVIN_HOME/.virtualenvs

##############################################################
# Install python dependencies
##############################################################

RUN apt-get update -y && \
    apt-get install -y python && \
    apt-get install -y python2.7-dev && \
    apt-get install -y python-pip && \
    apt-get install -y ipython && \
    # ??? apt-get install -y python-tk && \ ??? #
    pip install --upgrade pip==9.0.1 && \
    apt install -y libzmq3-dev libcurl4-openssl-dev libssl-dev && \
    pip install jupyter && \
    apt-get clean

##############################################################
# Copy and Install the marvin engine inside virtualenv
##############################################################

# adds the package containing the user-generated engine
ADD python-toolbox.tgz $MARVIN_ENGINE_HOME

# adds the freshly built engine server jar
ADD marvin-engine-executor-assembly.jar $MARVIN_DATA_PATH 

##############################################################
# Starts the jupyter http server
##############################################################

EXPOSE 8888

USER marvin

RUN /bin/bash -c "cd $MARVIN_ENGINE_HOME/python-toolbox && pip install . --user"

CMD /bin/bash -c "marvin --help"