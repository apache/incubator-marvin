############################################################
# DEVELOPMENT USE ONLY - USE THE RIGHT PROCEDURE TO DEPLOY #
############################################################

FROM marvin-daemon:python

ENV SLEEP_MILLIS 0

#############################################################
#                       CREATE USER                         #
#############################################################

RUN useradd -m marvin

##############################################################
# Define all environment variables to be used 
##############################################################

ENV MARVIN_HOME=/home/marvin/
ENV MARVIN_LOG=/home/marvin/log
ENV MARVIN_DATA_PATH=/home/marvin/data
ENV MARVIN_ENGINE_HOME=$MARVIN_HOME/engine
ENV SPARK_HOME=/opt/spark
ENV MARVIN_ENGINE_NAME={{cookiecutter.project_name}}
ENV SPARK_CONF_DIR=$SPARK_HOME/conf
ENV HADOOP_CONF_DIR=$SPARK_CONF_DIR
ENV YARN_CONF_DIR=$SPARK_CONF_DIR

##############################################################
# Put here the artifact protocol                             #
ENV ARTIFACT=
##############################################################


ENV PYTHONUNBUFFERED=0

##############################################################
# Create all folders needed 
##############################################################

RUN mkdir -p $MARVIN_HOME && \
    mkdir -p $MARVIN_DATA_PATH && \
    mkdir -p $MARVIN_LOG && \
    mkdir -p $MARVIN_DATA_PATH/.artifacts/$MARVIN_ENGINE_NAME

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
#        	    INSTALL ENGINE FROM SOURCE          	     #
##############################################################

ADD {{cookiecutter.project_package}}-0.0.1.tar.gz $MARVIN_HOME
ADD deps/artifacts/$MARVIN_ENGINE_NAME/* $MARVIN_DATA_PATH/.artifacts/$MARVIN_ENGINE_NAME/
ADD deps/marvin-engine-executor-assembly-*.jar $MARVIN_DATA_PATH/executor.jar
ADD deps/start.sh $MARVIN_DATA_PATH/start.sh

RUN cd $MARVIN_HOME && \
mv {{cookiecutter.project_name}} engine && \
cd $MARVIN_ENGINE_HOME && \
pip3 install -e . --no-compile && \
apt update && \
apt install -y openjdk-8-jre && \
update-alternatives --config java

###############################################################
#        CUSTOM ENGINE INSTALLATION PROCEDURE ABOVE           #
###############################################################

RUN pip3 install --no-cache pandas \
&& pip3 install --no-cache matplotlib \
&& pip3 install --no-cache cython \
&& pip3 install --no-cache numpy \
&& pip3 install --no-cache scikit-learn \
&& pip3 install --no-cache pytest \
&& pip3 install --no-cache pytest-cov \
&& pip3 install --no-cache pytest-watch

##############################################################
# Uninstalling unnecessary software and cleaning cache
##############################################################

RUN rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* /root/.cache/*

##############################################################
# Starts the daemon
##############################################################

EXPOSE 50051-50056
EXPOSE 8000

ENTRYPOINT ["/bin/bash", "-c", "cd $MARVIN_DATA_PATH && bash start.sh"]