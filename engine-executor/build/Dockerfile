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

##############################################################
# Base docker image for Marvin
##############################################################
FROM ubuntu:16.04

ENV SLEEP_MILLIS 0

USER root

##############################################################
# Creates the marvin user
##############################################################
RUN groupadd marvin && \
    useradd marvin -mg marvin

##############################################################
# Define all environment variables to be used 
##############################################################

ENV MARVIN_HOME=/home/marvin
ENV MARVIN_DATA_PATH=/home/marvin/marvin-data
ENV MARVIN_ENGINE_HOME=$MARVIN_HOME/engine
ENV MARVIN_ENGINE_ENV=marvin-engine-env


##############################################################
# Create all folders needed 
##############################################################

RUN mkdir -p $MARVIN_HOME && \
    mkdir -p $MARVIN_DATA_PATH && \
    mkdir -p $MARVIN_ENGINE_HOME && \
    mkdir -p /var/log/marvin/engines && \
    mkdir -p /var/run/marvin/engines


##############################################################
# Install the system dependencies for default installation 
##############################################################

RUN apt-get update -y && \
    apt-get install -y build-essential && \
    apt-get install -y maven git cmake software-properties-common curl libstdc++6 && \
    apt-get install -y wget && \
    apt-get install -y libffi-dev && \
    apt-get install -y libssl-dev && \
    apt-get install -y libxml2-dev && \
    apt-get install -y libxslt1-dev && \
    apt-get install -y libpng12-dev && \
    apt-get install -y libfreetype6-dev && \
    apt-get install -y libsasl2-dev && \
    apt-get install -y graphviz && \
    apt-get clean

### Installs Open JDK 8
RUN add-apt-repository ppa:openjdk-r/ppa && \
    apt-get update && \
    apt-get install -y openjdk-8-jdk

## TODO - Think in a good way to make Spark an option as soon we implement docker supporting it
##############################################################
# Install Apache Spark
#
# Uncomment if you are using spark, note that is needed the 
# spark configuration files to the think works correctly.
##############################################################

# RUN curl https://d3kbcqa49mib13.cloudfront.net/spark-2.1.1-bin-hadoop2.6.tgz -o /tmp/spark-2.1.1-bin-hadoop2.6.tgz && \
#    tar -xf /tmp/spark-2.1.1-bin-hadoop2.6.tgz -C /opt/ && \
#    ln -s /opt/spark-2.1.1-bin-hadoop2.6 /opt/spark

# Add the b2w datalake config for Spark
# ADD spark-conf.tar $SPARK_CONF_DIR
# RUN mkdir -p $SPARK_CONF_DIR