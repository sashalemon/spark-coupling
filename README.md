# Master Thesis Monorepository

This repository contains the main libraries, utilities, benchmark, and data used in the publication of [A Shared-Memory Coupled Architecture to Leverage Big Data Frameworks in Prototyping and In-Situ Analytics for Data Intensive Scientific Workflows](https://scholarsarchive.byu.edu/etd/7545/).

## Some Dependencies

Not all requirements are listed here, but to build code as written in this project will require at least:
* sbt
* cargo
* bmake
* icc
* ifort

The bmake-using Makefiles can be modified to not use BSD-Make syntax. 
Bugs in certain versions of GNU Fortran compiler may break the code for section 4.4, but in general it should be possible to substitute other compilers for the Intel versions. 

## Licensing

BSD 3-Clause
