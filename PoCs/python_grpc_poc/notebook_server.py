from concurrent import futures
import time
import subprocess

import grpc

import poc_pb2
import poc_pb2_grpc


class NotebookServicer(poc_pb2_grpc.NotebookServicer):

    def StartNotebook(self, ip='localhost', port=8888):
        p = subprocess.Popen(
                ["jupyter", "notebook"],
                stdout=subprocess.PIPE, stderr=subprocess.PIPE
            )
        while True:
            time.sleep(0.2)
            line = p.stderr.readline().decode()
            if line:
                yield poc_pb2.Logs(line=line)


def serve():
    with open('server.key', 'rb') as f:
        private_key = f.read()
    with open('server.crt', 'rb') as f:
        certificate_chain = f.read()

    server_credentials = grpc.ssl_server_credentials(
      ((private_key, certificate_chain,),))

    server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
    poc_pb2_grpc.add_NotebookServicer_to_server(
        NotebookServicer(), server)
    server.add_secure_port('[::]:1337', server_credentials)
    server.start()
    try:
        while True:
            time.sleep(60 * 60 * 24)
    except KeyboardInterrupt:
        server.stop(0)


if __name__ == '__main__':
    serve()
