name := "Courier"

//None of the pre-1.0 stuff is presentable
version := "1.0"

//Spark persists in using 2.11, so we can't upgrade to 2.12
scalaVersion := "2.11.11"

//Javolution provides native structs
libraryDependencies += "org.javolution" % "javolution-core-java" % "6.0.0" % "provided"

//JNR enables use of POSIX IPC
libraryDependencies += "com.github.jnr" % "jnr-constants" % "0.9.8"
libraryDependencies += "com.github.jnr" % "jnr-ffi" % "2.1.6"

//Serialization hack
libraryDependencies += "com.twitter" %% "chill" % "0.9.3"
libraryDependencies += "com.twitter" %% "chill-bijection" % "0.9.3"

//ARM==Automatic resource management
libraryDependencies += "com.jsuereth" %% "scala-arm" % "2.0"

//Spark dependency is obvious
val sparkVersion = "2.3.0"
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql",
  "org.apache.spark" %% "spark-core",
  "org.apache.spark" %% "spark-mllib"
).map(_ % sparkVersion % "provided")

//Microservices are embedded in Spark via Akka
val akkaVersion = "10.0.7"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http",
  "com.typesafe.akka" %% "akka-http-spray-json"
).map(_ % akkaVersion)

//Force SBT to be less cryptic and more useful 
scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

//Assembly settings for JAR packaging 
assemblyJarName := "libcourier.jar"
assemblyMergeStrategy in assembly := {
  case x if x.contains("bnd") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
