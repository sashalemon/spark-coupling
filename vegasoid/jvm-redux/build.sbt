name := "Spark Pruner"

version := "1.0"

//SystemML and Spark pull in enough stuff to break each other
//This removes the worst offenders
excludeDependencies ++= Seq("javax.jms", "com.sun.jmx", "com.sun.jdmk")

scalaVersion := "2.12.8"

//Javolution provides native structs
libraryDependencies += "org.javolution" % "javolution-core-java" % "6.0.0"

//Spark
val sparkVersion = "2.4.3"
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-sql",
  "org.apache.spark" %% "spark-core",
  "org.apache.spark" %% "spark-mllib"
).map(_ % sparkVersion % "provided")

scalacOptions in ThisBuild ++= Seq("-unchecked", "-deprecation")

assemblyJarName := "spark-pruner.jar"
assemblyMergeStrategy in assembly := {
  case x if x.contains("bnd") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
assemblyExcludedJars in assembly := { 
    val cp = (fullClasspath in assembly).value
    cp filter {_.data.getName == "SystemML.jar"}
}
