#pragma once
#include <stddef.h>
#include <sys/time.h>
typedef struct {
    long id;
    int rank;
    int natoms;
    double energy;
} Timestep;

typedef struct {
    long timestep;
    int rank;
    char symbol;
    int num;
    double x;
    double y;
    double z;
    double force_x;
    double force_y;
    double force_z;
} Atom;

//typedef struct {
//    char only;
//} Tiny;
//Tiny* make_tiny(size_t nsteps) {
//    Tiny* data = malloc(nsteps*sizeof(Tiny));
//    int i;
//    for(i = 0; i < nsteps; i++) {
//        data[i].only = 0;
//    }
//    return data;
//}

typedef void (*writer)(Timestep*, Atom*, size_t, int);
int run(int argc, char** argv, writer w, const char* method, const char* analysis_name, const char** analysis_deps, size_t ndeps);

double when();
