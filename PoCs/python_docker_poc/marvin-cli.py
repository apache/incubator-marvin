from ishell.command import Command
from ishell.console import Console

import os
import docker


class DockerCommand(Command):
    def __init__(self, *args, **kwargs):
        tls_config = docker.tls.TLSConfig(                                     
            client_cert=('ca.pem', 'ca-key.pem')
        )
        self.docker = docker.DockerClient(base_url='localhost:2376', tls=tls_config)
        super(DockerCommand, self).__init__(*args, **kwargs)


class NotebookSDK(DockerCommand):
    def run(self, line):
        #import pdb; pdb.set_trace()
        containers = self.docker.containers.list(filters={
            'ancestor':'marvinaiplatform/marvin-automl:0.0.1',
            'status': 'running'
        })
        if containers:
            container = containers[0]
        else:
            container = self.docker.containers.run(
                "marvinaiplatform/marvin-automl:0.0.1", auto_remove=True,
                detach=True, ports={'8000/tcp': 8000, '9999/tcp': 9999}
            )
        cmds = [
            'source /usr/local/bin/virtualenvwrapper.sh',
            'workon marvin-engine-env',
            'cd /opt/marvin/engine/',
            'marvin notebook --allow-root -p 9999'
        ]
        logs = container.exec_run("bash -c '%s'" % ';'.join(cmds), stream=True)
        for line in logs.output:
            print(line.strip())


class DryRunSDK(DockerCommand):
    def run(self, line):
        containers = self.docker.containers.list(filters={
            'ancestor':'marvinaiplatform/marvin-automl:0.0.1',
            'status': 'running'
        })
        if containers:
            container = containers[0]
        else:
            container = self.docker.containers.run(
                "marvinaiplatform/marvin-automl:0.0.1", auto_remove=True,
                detach=True, ports={'8000/tcp': 8000, '9999/tcp': 9999}
            )
        cmds = [
            'source /usr/local/bin/virtualenvwrapper.sh',
            'workon marvin-engine-env',
            'cd /opt/marvin/engine/',
            'marvin engine-dryrun'
        ]
        logs = container.exec_run("bash -c '%s'" % ';'.join(cmds), stream=True)
        for line in logs.output:
            print(line.decode().strip())

def main():
    console = Console("marvin", ">")
    notebook = NotebookSDK("notebook")
    dryrun = DryRunSDK("dryrun")

    console.addChild(notebook)
    console.addChild(dryrun)
    console.loop()


if __name__ == '__main__':
    main()