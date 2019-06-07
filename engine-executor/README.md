[![Build Status](https://travis-ci.org/apache/incubator-marvin.svg)](https://travis-ci.org/apache/incubator-marvin) [![codecov](https://codecov.io/gh/apache/incubator-marvin/branch/develop/graph/badge.svg)](https://codecov.io/gh/apache/incubator-marvin/branch/develop)

# Marvin Engine Executor (Server)

This is the component responsable for coordinate execution of the steps (actions) in a Marvin engine. The engine-executor
is able to communicate with engines through gRPC protocol. More details about the contract can be found 
on the protobuf file.

Last stable build v0.0.4 can be downloaded from [here](https://s3.amazonaws.com/marvin-engine-executor/marvin-engine-executor-assembly-0.0.4.jar).

### install requirements

- Java 8 or +

### how to build

From the root folder, run: 

```
make package
```

### how to run

```
java <OPTIONS> -jar path_to_jar_file.jar
```

#### the available options are:

- marvinConfig.engineHome (path to the folder where the params and metadata file are located)
- marvinConfig.ipAddress (the IP address to bind the server)
- marvinConfig.port (the port to bind the server)

Example of direct invocation with options:

```
java -DmarvinConfig.engineHome=/path -DmarvinConfig.ipAddress=0.0.0.0 -DmarvinConfig.port=8080 -jar marvin_engine_executor.jar
```

> Marvin is a project started at B2W Digital offices and released open source on September 2017.
> The project is donated to Apache Software Foundation on August 2018.
