FROM ubuntu:20.04

ADD python-daemon.tar.gz .
RUN apt update && DEBIAN_FRONTEND="noninteractive" apt install libsasl2-dev graphviz python3 python3-pip -y
RUN /bin/bash -c "cd python-daemon && pip3 install ." \
&& rm -rf python-daemon \
&& apt-get clean \
&& rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* /root/.cache/*
