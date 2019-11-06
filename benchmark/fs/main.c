#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <mpi.h>
#include "../base/base.h"

#define NFMT "/tmp/scratch/%s.csv/file-%d.part"
void write_timesteps(Timestep* steps, size_t nsteps, int rank) {
    char fname[512];
    snprintf(fname, 512, NFMT, "timestep", rank);
    FILE* file = fopen(fname, "w");
    int i;
    //fprintf(stderr, "id,rank,natoms,energy\n");
    for(i = 0; i < nsteps; i++) {
        fprintf(file, "%ld,%d,%d,%f\n",
                steps[i].id,
                steps[i].rank,
                steps[i].natoms,
                steps[i].energy);
    }
    fclose(file);
}
void write_atoms(Timestep* steps, Atom* atoms, size_t nsteps, int rank) {
    char fname[512];
    snprintf(fname, 512, NFMT, "atom", rank);
    FILE* file = fopen(fname, "w");
    int i, j;
    //fprintf(stderr, "timestep,rank,symbol,num,x,y,z,force_x,force_y,force_z\n");
    size_t atoms_per_step, atom_idx;
    atom_idx = 0;
    for(i = 0; i < nsteps; i++) {
        atoms_per_step = steps[i].natoms;
        for(j = 0; j < atoms_per_step; j++) {
            fprintf(file, "%ld,%d,%d,%d,%f,%f,%f,%f,%f,%f\n",
                    atoms[atom_idx].timestep,
                    atoms[atom_idx].rank,
                    atoms[atom_idx].symbol,
                    atoms[atom_idx].num,
                    atoms[atom_idx].x,
                    atoms[atom_idx].y,
                    atoms[atom_idx].z,
                    atoms[atom_idx].force_x,
                    atoms[atom_idx].force_y,
                    atoms[atom_idx].force_z);
            atom_idx++;
        }
    }
    fclose(file);
}

void write_data(Timestep* steps, Atom* atoms, size_t nsteps, int rank)
{
    write_timesteps(steps, nsteps, rank);
    write_atoms(steps, atoms, nsteps, rank);
}

int main(int argc, char** argv) 
{
    const char* deps[] = {};
    return run(argc, argv, write_data, "fs", "FSCount", deps, sizeof(deps)/sizeof(char*));
}
