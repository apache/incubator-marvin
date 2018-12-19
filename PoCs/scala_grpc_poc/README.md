# marvin-repl

A scala version of the repl using scalaPB to help marvin to control docker containers.

It is a WIP (work-in-progress)

## How to use?

First, run for tests

```
sbt compile
```

to create the case class GreeterGrpc.

Second, run

```
sbt "runMain main.scala.org.marvin.repl.ReplServer"
```

to start the Server.

Third, run

```
sbt "runMain main.scala.org.marvin.repl.ReplClient"
```
