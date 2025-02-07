import sbt._

object Dependencies {
  val AkkaVersion = "2.6.20"
  lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % AkkaVersion
}
