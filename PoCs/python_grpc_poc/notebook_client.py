import grpc

import poc_pb2
import poc_pb2_grpc


def start_notebook(stub):
    params = poc_pb2.Params(ip='localhost', port='9999')
    logs = stub.StartNotebook(params)

    for line in logs:
        print(line)


def run():
    with open('server.crt', 'rb') as f:
        trusted_certs = f.read()

    credentials = grpc.ssl_channel_credentials(root_certificates=trusted_certs)

    with grpc.secure_channel('localhost:1337', credentials) as channel:
        stub = poc_pb2_grpc.NotebookStub(channel)
        print("-------------- StartNotebook --------------")
        start_notebook(stub)


if __name__ == '__main__':
    run()
