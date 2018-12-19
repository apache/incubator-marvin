import scalapb.compiler.Version.{grpcJavaVersion, scalapbVersion}

name := "protoc-test"

version := "0.1"

scalaVersion := "2.12.6"

// compiles protobuf definitions into scala code
PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "scalapb-runtime"       % scalapbVersion % "protobuf",
  // for gRPC
  "io.grpc"              %  "grpc-netty"            % grpcJavaVersion,
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc"  % scalapbVersion,
  // for JSON conversion
  "com.thesamet.scalapb" %% "scalapb-json4s"        % "0.7.0",
  "io.netty" % "netty-handler" % "4.1.17.Final",
  "io.netty" % "netty-tcnative-boringssl-static" % "2.0.7.Final"
)