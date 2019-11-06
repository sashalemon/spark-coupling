# Framework launch scripts for Slurm

Q: What is this?
A: Erlang scripts which vastly simplify launching coupled workflows with Spark.

Q: Should I use it?
A: That depends. If you use Slurm, almost certainly. Even if your job scheduler is not Slurm, the Slurm-specific pieces can probably be replaced with ones that work for you. The main reason not to use this is if you cannot run Erlang for some reason, or if you want to use completely different conventions for organizing your output files and/or handling job failures.

## Dependencies

* Erlang (tested with version 19.1)
    * [Kerl](https://github.com/kerl/kerl) is recommended for managing the install
* [Erlexec](http://saleyn.github.com/erlexec/)
* [Slurm](https://github.com/SchedMD/slurm) or a similar job scheduler

## Use

Before using these scripts, ensure the framework libraries (and any test data) are installed on your cluster.

1. Adjust script variables to fit your computing environment.
2. Submit (ie. via sbatch) the `setup` file.
