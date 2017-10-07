[![Build Status](https://travis-ci.org/marvin-ai/marvin-engine-executor.svg)](https://travis-ci.org/marvin-ai/marvin--engine-executor) [![codecov](https://codecov.io/gh/marvin-ai/marvin-engine-executor/branch/master/graph/badge.svg)](https://codecov.io/gh/marvin-ai/marvin-engine-executor)

# Marvin Engine Executor (Server)

This is the server able to coordinate execution of the steps in a Marvin engine. The engine-executor
is able to communicate with engines through gRPC protocol. More details about the contract can be found 
on the protobuf file.

The latest stable build can be downloaded from: //TODO

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

### using engine-executor from marvin-toolbox

If you are running marvin within the development vagrant, or you have the marvin toolbox installed, 
the engine-executor will be available through a command line. To start it run the following command
from the toolbox CLI: 

```
marvin engine-httpserver
```

The following options will be available:
```
Options:
  -a, --action [all|acquisitor|tpreparator|trainer|evaluator|ppreparator|predictor]
                                  Marvin engine action name
  -id, --initial-dataset PATH     Initial dataset file path
  -d, --dataset PATH              Dataset file path
  -m, --model PATH                Engine model file path
  -me, --metrics PATH             Engine Metrics file path
  -pf, --params-file PATH         Marvin engine params file path
  -c, --spark-conf PATH           Spark configuration folder path to be used
                                  in this session
  -h, --http_host TEXT            Engine executor http bind host
  -p, --http_port INTEGER         Engine executor http port
  -e, --executor-path PATH        Marvin engine executor jar path
```

- action - specify which pipeline actions will be performed on the server.
- id - the PATH to load initial dataset if you want to reuse an existent file.
- d - the PATH to load prepared dataset if you want to reuse an existent file.
- m - the PATH to load the model if you want to reuse an existent model.
- me - the PATH to load the metrics dataset if you want to reuse an existent file.
- pf - the folder containing the file engine.params and engine.metadata.
- c - the folder to the spark configuration.
- h - the IP address to bind the server.
- p - the port to bind the server.
- e - the PATH to the engine executor jar if you want to use a custom jar.

> Marvin is a project started at B2W Digital offices and released open source on September 2017.
