# Marvin CLI v0.0.5

![](https://github.com/apache/incubator-marvin-website/blob/master/site/assets/themes/apache/img/logo.png?raw=true)

# Quick Start

## Review

**Marvin** is an open-source Artificial Intelligence platform that focuses on helping data scientists deliver meaningful solutions to complex problems. Supported by a standardized large-scale, language-agnostic architecture, Marvin simplifies the process of exploration and modeling.

## Getting Started
* [Installing Marvin (Ubuntu)](https://marvin.apache.org/marvin-platform-book/ch2_toolbox_installation/ubuntu/)
* [Installing Marvin (MacOS)](https://marvin.apache.org/marvin-platform-book/ch2_toolbox_installation/mac/)
* [Installing Marvin (Other OS) Vagrant](https://marvin.apache.org/marvin-platform-book/ch2_toolbox_installation/vagrant/)
* [Creating a new engine](#creating-a-new-engine)
* [Working in an existing engine](#working-in-an-existing-engine)
* [Command line interface](#command-line-interface)
* [Running an example engine](#running-a-example-engine)


### Creating a new engine
1. To create a new engine
```
marvin project-generate
```
Respond to the prompt and wait for the engine environment preparation to complete. Don't forget to start dev box before if you are using vagrant.

2. Test the new engine
```
cd $MARVIN_HOME/engine
marvin test
```

3. For more information
```
marvin --help
```

### Command line interface
Usage: marvin [OPTIONS] COMMAND [ARGS]

Options:
```
  --help        #Show this command line interface and exit.
```

Commands:
```
  edit-config         Edit configuration.
  project-generate    Generate engine project.
  project-import      Import engine project from archive file.
  edit-metadata       Edit engine.metadata.
  engine-bumpversion  Bump, commit and tag engine version.
  engine-dryrun       Run engines in a standalone way.
  engine-grpcserver   Run gRPC of given actions.
  engine-httpserver   Run executor HTTP server.
  engine-logs         Show daemon execution.
  lab                 Run custom engine Jupyter Lab.
  notebook            Run custom engine Jupyter Notebook.
  project-export      Export engine project to a archive file.
  test                Run tests.
  test-tdd            Watch for changes to run tests automatically.
  test-tox            Run tests using Tox environment.
```

> Marvin is a project started at B2W Digital offices and released open source on September 2017.
> The project is donated to Apache Software Foundation on August 2018.
