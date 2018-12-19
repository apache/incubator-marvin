.PHONY: help stubs client server gen_key

help:
	@echo "    stubs"
	@echo "        Generate gRPC code."
	@echo "    client"
	@echo "        Start client."
	@echo "    server"
	@echo "        Start server."
	@echo "    gen_key"
	@echo "        Generate credential."

stubs:
	python run_codegen.py

client:
	python notebook_client.py

server:
	python notebook_server.py

gen_key:
	openssl req -newkey rsa:2048 -nodes -keyout server.key -x509 -days 365 -out server.crt