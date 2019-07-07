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
# Create all folders needed 
##############################################################


##############################################################
# Install the system dependencies for default R installation 
##############################################################

RUN apt install -y apt-transport-https &&\
    apt update && \
    apt install -y python2.7-dev && \
    apt install -y python-pip && \
    apt install -y ipython && \
    pip install --upgrade pip==9.0.1 && \
    apt install -y libzmq3-dev libcurl4-openssl-dev libssl-dev && \
    pip install jupyter
    

RUN apt install -y r-base && \
    su -c "R -e \"install.packages(c('repr', 'IRdisplay', 'IRkernel'), type = 'source', repos='http://cran.rstudio.com/')\"" && \
    su -c "R -e \"IRkernel::installspec(user = FALSE)\""


##############################################################
# Copy and Install the marvin engine inside virtualenv
##############################################################

### TODO - Uncomment this block once we have the common lib for R tested and we can run engines in R

# adds the package containing the user-generated engine
#ADD generated-engine $MARVIN_ENGINE_HOME

# adds the freshly built engine server jar
#ADD marvin-engine-executor-assembly.jar $MARVIN_DATA_PATH 

##############################################################
# Starts the jupyter http server
##############################################################

EXPOSE 8888

USER marvin

CMD /bin/bash -c "jupyter notebook --ip=0.0.0.0 --notebook-dir=$MARVIN_HOME"