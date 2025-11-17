import sbt._

object Dependencies {

  private val gatlingVersion = "3.14.3"

  val hmrcMongoPlayVersion = "2.10.0"

  val test = Seq(
    "com.typesafe"          % "config"                    % "1.4.4"        % Test,
    "uk.gov.hmrc"          %% "performance-test-runner"   % "6.3.0"        % Test,
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-test-play-30" % hmrcMongoPlayVersion % Test
  )
}
