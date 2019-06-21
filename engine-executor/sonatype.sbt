import xerial.sbt.Sonatype._

publishMavenStyle := true

sonatypeProfileName := "org.apache.marvin"

sonatypeProjectHosting := Some(GitHubHosting("incubator-marvin", "dev@marvin.apache.org", "marvin-engine-executor"))

licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
