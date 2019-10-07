############################################################
FROM python:2-alpine3.10
# To use python3 comment the line above and uncomment 
# the line bellow.
#FROM python:3-alpine3.10
############################################################

MAINTAINER dev@marvin.apache.org

ENV SLEEP_MILLIS 0

USER root

##############################################################
# Define all environment variables to be used 
##############################################################

ENV MARVIN_HOME=/opt/marvin
ENV MARVIN_DATA_PATH=/marvin-data
ENV MARVIN_ENGINE_HOME=$MARVIN_HOME/engine
ENV MARVIN_ENGINE_ENV=marvin-engine-env
ENV WORKON_HOME=$MARVIN_HOME/.virtualenvs
ENV SPARK_HOME=/opt/spark
ENV SPARK_CONF_DIR=$SPARK_HOME/conf
ENV HADOOP_CONF_DIR=$SPARK_CONF_DIR
ENV YARN_CONF_DIR=$SPARK_CONF_DIR



##############################################################
# Create all folders needed 
##############################################################

RUN mkdir -p $MARVIN_HOME && \
    mkdir -p $MARVIN_DATA_PATH && \
    mkdir -p $MARVIN_ENGINE_HOME && \
    mkdir -p /var/log/marvin/engines && \
    mkdir -p /var/run/marvin/engines && \
##############################################################
# Install the system dependencies for default installation 
##############################################################
    apk add --no-cache g++ openssl-dev openjdk11-jre-headless bash && \
    apk add --no-cache --virtual .build-deps make \
    git \
    wget \
    libsass-dev \
    openblas-dev \
    libffi-dev \
    libxml2-dev \
    libxslt-dev \
    libpng-dev \
    freetype-dev \
    cyrus-sasl-dev
##############################################################
# Install Apache Spark
#
# Uncomment if you are using spark, note that is needed the 
# spark configuration files to the think works correctly.
##############################################################
#
# RUN wget -O /tmp/spark-2.1.1-bin-hadoop2.6.tgz https://d3kbcqa49mib13.cloudfront.net/spark-2.1.1-bin-hadoop2.6.tgz && \
#    tar -xf /tmp/spark-2.1.1-bin-hadoop2.6.tgz -C /opt/ && \
#    ln -s /opt/spark-2.1.1-bin-hadoop2.6 /opt/spark
##############################################################

RUN mkdir -p $SPARK_CONF_DIR

##############################################################
#        <CUSTOM ENGINE INSTALLATION PROCEDURE HERE>         #
##############################################################


##############################################################
# Copy and Install the marvin engine
##############################################################

RUN /bin/bash -c "pip install virtualenvwrapper && \
    cd $MARVIN_ENGINE_HOME && \
    source /usr/local/bin/virtualenvwrapper.sh && \
    mkvirtualenv $MARVIN_ENGINE_ENV"

ADD build/engine.tar $MARVIN_ENGINE_HOME

ADD build/marvin-engine-executor-assembly.jar $MARVIN_DATA_PATH 

RUN /bin/bash -c "source /usr/local/bin/virtualenvwrapper.sh && \
    workon $MARVIN_ENGINE_ENV && \
    cd $MARVIN_ENGINE_HOME && \
    pip install --no-cache numpy && \
    pip install --no-cache scipy && \
    pip install --no-cache pandas && \
    pip install --no-cache matplotlib && \
    pip install --no-cache cython && \
    pip install --no-cache scikit-learn && \
    pip install --no-cache Fabric && \
    pip install --no-cache marvin-python-toolbox && \
    pip install . "
##############################################################
# Uninstalling unnecessary software and cleaning cache
##############################################################
RUN rm -rf /root/.cache && \
    apk del .build-deps

##############################################################
# Starts the engine http server
##############################################################

EXPOSE 8000

CMD /bin/bash -c "source /usr/local/bin/virtualenvwrapper.sh && \
    workon $MARVIN_ENGINE_ENV && \
    cd $MARVIN_ENGINE_HOME && \
    marvin engine-httpserver -h 0.0.0.0 -p 8000"