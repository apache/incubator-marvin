/*
 * Copyright [2017] [B2W Digital]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
name := "marvin-engine-executor"

version := "0.0.5"

scalaVersion := "2.12.3"

organization := "org.apache.marvin"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.10",
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.10",
  "org.clapper"       %% "grizzled-slf4j" % "1.3.0",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.0.10" % Test,
  "org.scalatest"     %% "scalatest"     % "3.0.1" % Test,
  "org.scalamock"     %% "scalamock-scalatest-support" % "3.5.0" % Test,
  "com.typesafe.akka" %% "akka-cluster" % "2.5.9"
)

libraryDependencies ++= Seq (
  "io.grpc" % "grpc-netty" % com.trueaccord.scalapb.compiler.Version.grpcJavaVersion,
  "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % com.trueaccord.scalapb.compiler.Version.scalapbVersion
)

libraryDependencies += "org.apache.hadoop" % "hadoop-client" % "2.7.4"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.3"
libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.0"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.9.0"
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.0"
libraryDependencies += "com.github.java-json-tools" % "json-schema-validator" % "2.2.8"
libraryDependencies += "io.jvm.uuid" %% "scala-uuid" % "0.2.3"
libraryDependencies += "com.amazonaws" % "aws-java-sdk-s3" % "1.11.232"

dependencyOverrides ++= Set(
  "io.netty" %% "netty" % "3.7.0",
  "io.netty" %% "netty-handler-proxy" % "4.1.12",
  "com.google.guava" %% "guava" % "19.0"
)

libraryDependencies += "com.github.cb372" %% "scalacache-guava" % "0.22.0"

mainClass in (Compile, run) := Some("org.marvin.executor.EngineExecutorApp")
mainClass in assembly := Some("org.marvin.executor.EngineExecutorApp")

assemblyMergeStrategy in assembly := {
  case PathList("org", "apache", xs @_*) => MergeStrategy.first
  case PathList("io", "netty", xs @_*) => MergeStrategy.first
  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
  case x => {
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
  }
}

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

useGpg := true
