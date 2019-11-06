# libcourier.jar

A JAR containing the bulk of the JVM libcourier implementation, provided for the convenience of users who don't need to fiddle with the source. It also has any profiling debugging dependencies stripped for efficient deployment. If you need more than a basic release, build from source.

## Instructions

You will need [SBT](https://www.scala-sbt.org/) to build this project.

With it, this library can be built by running `sbt assembly`.

Once SBT finishes, the library should be available in `target/scala-2.11/libcourier.jar`.

The output should also be in [Downloads](https://bitbucket.org/byucsl/libcourier/downloads/), so you don't need to build this yourself.
