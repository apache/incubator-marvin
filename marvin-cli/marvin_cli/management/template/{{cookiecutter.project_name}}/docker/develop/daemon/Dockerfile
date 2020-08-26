############################################################
# DEVELOPMENT USE ONLY - USE THE RIGHT PROCEDURE TO DEPLOY #
############################################################

FROM marvin-daemon:python

MAINTAINER {{cookiecutter.maintainer_name}}

ENV SLEEP_MILLIS 0

##############################################################
# Define all environment variables to be used 
##############################################################

ENV ENGINE={{cookiecutter.project_name}}
ENV MARVIN_HOME=/opt/marvin
ENV MARVIN_LOG=$MARVIN_HOME/log
ENV MARVIN_DATA_PATH=/opt/marvin/data
ENV MARVIN_ENGINE_HOME=$MARVIN_HOME/engine
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
    mkdir -p $MARVIN_LOG

##############################################################
# Install Apache Spark
#
# Uncomment if you are using spark, note that is needed the 
# spark configuration files to the think works correctly.
##############################################################
#
# RUN wget -O /tmp/spark-2.1.1-bin-hadoop2.6.tgz \
#          https://d3kbcqa49mib13.cloudfront.net/spark-2.1.1-bin-hadoop2.6.tgz && \
#    tar -xf /tmp/spark-2.1.1-bin-hadoop2.6.tgz -C /opt/ && \
#    ln -s /opt/spark-2.1.1-bin-hadoop2.6 /opt/spark
##############################################################

RUN mkdir -p $SPARK_CONF_DIR

##############################################################
#        	INSTALL ENGINE FROM SOURCE          	     #
##############################################################

COPY {{cookiecutter.project_package}}-0.0.1.tar.gz .
RUN pip install {{cookiecutter.project_package}}-0.0.1.tar.gz

##############################################################
#        CUSTOM ENGINE INSTALLATION PROCEDURE ABOVE          #
##############################################################

RUN pip install --no-cache pandas \
&& pip install --no-cache matplotlib \
&& pip install --no-cache cython \
&& pip install --no-cache numpy \
&& pip install --no-cache scikit-learn \
&& pip install --no-cache pytest \
&& pip install --no-cache pytest-cov \
&& pip install --no-cache pytest-watch

##############################################################
# Uninstalling unnecessary software and cleaning cache
##############################################################

RUN rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* /root/.cache/*

##############################################################
# Starts the daemon
##############################################################

EXPOSE 50051-50057
EXPOSE 8888

CMD /bin/bash -c "cd $MARVIN_ENGINE_HOME && marvin-daemon"