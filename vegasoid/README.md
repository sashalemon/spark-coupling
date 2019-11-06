# Adaptive Histograms

This performs adaptive histogram sampling that effectively resembles the VEGAS algorithm, but is intended to highlight the flexible utility gained by coupling Spark to High-Performance simulations. Histogram adaptation is simply a minimal representative of stochastic search workflows, and this shows how Spark+SystemML could provide a means of rapidly prototyping methods for optimally pruning traversing the search space of such workflows. For details, read section 4.4 of the published thesis.

## Quality

It should be noted that the naming of certain files, design of scripts, and other conventions in this section is not necessarily meaningful in any way. 
This code was originally written as almost throw-away material to showcase things about the core thesis, and is included for completeness, under the [CRAPL](http://matt.might.net/articles/crapl/) (in addition to the BSD 3-Clause if you really want to use it).

## Usage

To actually run this may require fiddling with the versioning of Spark, Scala, SystemML, and any other JVM dependencies. 
The thesis work needed to verify whether the *core libraries* handled bleeding edge upgrades, and while the *core libraries* do, this demo involves far more than just the core of the thesis work.
For publication, a combination of versions was used which was not officially compatible at the time. 
Much hassle can be avoided by tweaking the SBT build to use an officially supported mix of the major libraries.
If the bleeding edge is required, modifying and building large JAR libraries will likely be necessary.

To build the native, mock-simulation half of this demo, the [Bspline-Fortran](https://github.com/jacobwilliams/bspline-fortran) library is required.
