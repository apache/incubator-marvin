FROM python:3.7-buster

ADD python-daemon.tar.gz .
RUN /bin/bash -c "apt update && apt install libsasl2-dev graphviz -y" \
&& /bin/bash -c "cd python-daemon && pip install ." \
&& rm -rf python-daemon \
&& apt-get clean \
&& rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* /root/.cache/*