# spark-pruner.jar

Be warned that this may not build correctly without some tweaking of SystemML code or a few hours spent fiddling with library versions, as some compatibility hacks were used to enable a combination of SystemML, Spark, and Scala versions not officially supported by SystemML at publication time.

You will need [SBT](https://www.scala-sbt.org/) to build this project.

Ensure the libcourier.jar is in the `lib` subdirectory.
Then, this app can be built by copying your custom data schema into `src/main/scala/schema.scala` and running `sbt assembly`.

Once SBT finishes, the library should be available in `target/scala-SCALA_VERSION/spark-pruner.jar`.
