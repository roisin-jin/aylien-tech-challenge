lazy val akkaHttpVersion = "10.1.11"
lazy val akkaVersion    = "2.6.1"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.example",
      scalaVersion    := "2.13.1"
    )),
    name := "akka-http-paint-factory",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-caching"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",
      "org.scala-lang.modules" %% "scala-parallel-collections" % "0.2.0",

      "com.typesafe.slick" %% "slick"               % "3.3.2",
      "com.typesafe.slick" %% "slick-hikaricp"      % "3.3.2",
      "mysql"              % "mysql-connector-java" % "5.1.34",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.0.8"         % Test,
      "com.h2database" % "h2" % "1.4.200" % Test,
    )
  )
