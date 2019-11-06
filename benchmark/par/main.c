#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <mpi.h>
#include "../base/base.h"

void courier_write_table(const char* name, void* rows, size_t row_size, size_t nrows, int append);

void write_data(Timestep* steps, Atom* atoms, size_t nsteps, int _rank)
{
    size_t totalAtoms = 0;
    int i;
    for(i = 0; i < nsteps; i++) { totalAtoms += steps[i].natoms; }
    courier_write_table("Timestep", (void*)steps, sizeof(Timestep), nsteps, 0);
    courier_write_table("Atom", (void*)atoms, sizeof(Atom), totalAtoms, 0);
}

int main(int argc, char** argv) 
{
    const char* deps[] = {"Timestep", "Atom"};
    return run(argc, argv, write_data, "ring", "Count", deps, sizeof(deps)/sizeof(char*));
}
