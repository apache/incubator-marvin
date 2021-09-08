############################################################
# DEVELOPMENT USE ONLY - USE THE RIGHT PROCEDURE TO DEPLOY #
############################################################

FROM marvin-daemon:python

LABEL MAINTAINER {{cookiecutter.maintainer_name}}

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
ENV PYTHONUNBUFFERED=0

##############################################################
# Create all folders needed 
##############################################################

RUN mkdir -p $MARVIN_HOME && \
    mkdir -p $MARVIN_DATA_PATH && \
    mkdir -p $MARVIN_LOG && \
    mkdir -p $MARVIN_HOME/.ssh

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

###############################################################
#                    CONFIGURE SSH SERVER                     #
###############################################################

ADD id_rsa.pub $MARVIN_HOME/.ssh/
RUN apt update && \
apt install -y net-tools openssh-server sudo && \
sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/g' /etc/ssh/sshd_config && \
sed -i 's/PermitRootLogin without-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
sed -i 's/PrintModt yes/PrintModt no/' /etc/ssh/sshd_config && \
cat $MARVIN_HOME/.ssh/id_rsa.pub >> $MARVIN_HOME/.ssh/authorized_keys && \
service ssh start && \
chown -R marvin:marvin /home/marvin && \
chmod -R 700 $MARVIN_HOME/.ssh && \
chmod -R 600 $MARVIN_HOME/.ssh/authorized_keys && \
echo 'marvin:Marvin' | chpasswd && \
addgroup marvin staff && \ 
addgroup marvin sudo && \
true

##############################################################
#        	    INSTALL ENGINE FROM SOURCE          	     #
##############################################################

ADD {{cookiecutter.project_package}}-0.0.1.tar.gz $MARVIN_HOME

RUN cd $MARVIN_HOME && \
mv {{cookiecutter.project_name}} engine && \
cd $MARVIN_ENGINE_HOME && \
pip3 install -e . --no-compile

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

EXPOSE 50051-50057
EXPOSE 22
EXPOSE 8888

CMD /bin/bash -c "/usr/sbin/sshd && cd $MARVIN_ENGINE_HOME && nohup marvin-daemon"