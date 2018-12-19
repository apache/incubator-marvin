.PHONY: help stubs client server gen_key

help:
	@echo "    docker-notebook"
	@echo "        Start notebook inside a container"
	@echo "    docker-server"
	@echo "        Start dockerd with TLS."
	@echo "    generate-keys"
	@echo "        Generate keys to use Docker Deamon with TLS *on localhost*"

docker-server:
	sudo service docker stop
	sudo dockerd --tlsverify --tlscacert=ca.pem --tlscert=server-cert.pem --tlskey=server-key.pem -H=0.0.0.0:2376


docker-notebook:
	docker --tlsverify --tlscacert=ca.pem --tlscert=cert.pem --tlskey=key.pem -H=localhost:2376 run --name ubuntu_bash -p 8888:8888 --rm -d -t ubuntu
	docker --tlsverify --tlscacert=ca.pem --tlscert=cert.pem --tlskey=key.pem -H=localhost:2376 exec ubuntu_bash sh -c "apt-get update; apt-get -y upgrade; apt-get install -y python-dev python-pip"
	docker --tlsverify --tlscacert=ca.pem --tlscert=cert.pem --tlskey=key.pem -H=localhost:2376 exec ubuntu_bash sh -c "pip install notebook"
	docker --tlsverify --tlscacert=ca.pem --tlscert=cert.pem --tlskey=key.pem -H=localhost:2376 exec ubuntu_bash sh -c "jupyter-notebook --ip=0.0.0.0 --allow-root"


generate-keys:
	openssl genrsa -aes256 -out ca-key.pem 4096
	openssl req -new -x509 -days 365 -key ca-key.pem -sha256 -out ca.pem -subj "/CN=localhost"
	openssl genrsa -out server-key.pem 4096
	openssl req -subj "/CN=localhost" -sha256 -new -key server-key.pem -out server.csr
	echo subjectAltName = DNS:localhost,IP:10.10.10.20,IP:127.0.0.1 >> extfile.cnf
	echo extendedKeyUsage = serverAuth >> extfile.cnf
	openssl x509 -req -days 365 -sha256 -in server.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out server-cert.pem -extfile extfile.cnf
	openssl genrsa -out key.pem 4096
	openssl req -subj '/CN=client' -new -key key.pem -out client.csr
	echo extendedKeyUsage = clientAuth >> extfile.cnf
	openssl x509 -req -days 365 -sha256 -in client.csr -CA ca.pem -CAkey ca-key.pem -CAcreateserial -out cert.pem -extfile extfile.cnf
	rm -v client.csr server.csr
	chmod -v 0400 ca-key.pem key.pem server-key.pem
	chmod -v 0444 ca.pem server-cert.pem cert.pem