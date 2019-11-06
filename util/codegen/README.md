# Codegen for libcourier.jar

Q: What is this?
A: An optional code generator for libcourier. 

Q: Should I use it?
A: Probably. If you aren't intimately familiar with Scala and Spark's architecture and JVM workarounds,
you almost certainly want to try this tool before modifying libcourier internals on the Spark side.
Even if you do decide to write your own library, and you're a Java, Scala, and Spark master,
a simple generated example can be a useful architectural reference.

## Dependencies

```toml
askama = "0.7.1"
clap = "2.32.0"
colored = "1.6.1"
```

## Use

The input should be a C header file containing your data structures (and nothing else, ideally). 

If you give structures of the form `typdef struct {...} <Name>`, you will have a table called `Name` accessible in Spark, whose columns are the fields of the associated struct.
