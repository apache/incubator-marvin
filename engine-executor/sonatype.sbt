import xerial.sbt.Sonatype._

publishMavenStyle := true

sonatypeProfileName := "com.github.marvin-ai"

sonatypeProjectHosting := Some(GitHubHosting("marvin-ai", "daniel.takabayashi@b2wdigital.com", "marvin-engine-executor"))

licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
