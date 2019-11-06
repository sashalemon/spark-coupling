#include <mpi.h>
#include <assert.h>
#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <Random123/philox.h>
#include "base.h"
extern void courier_write_table(const char* name, void* rows, size_t row_size, size_t nrows, int append);
extern char* courier_request_analysis(const char* master, const char* analysis, const char** data_deps, size_t ndeps);
extern void courier_free_data(char* data);
//static size_t progress = 0;
/* Return the current time in seconds, using a double precision number. */
double when()
{
    struct timeval tp;
    gettimeofday(&tp, NULL);
    return ((double) tp.tv_sec + (double) tp.tv_usec * 1e-6);
}

Timestep* make_timesteps(size_t nsteps, int iproc, philox4x32_key_t *k, philox4x32_ctr_t *c) {
    int i;
    Timestep* steps = (Timestep*)malloc(nsteps*sizeof(Timestep));
    for(i = 0; i < nsteps; i++) {
        c->v[1] = i;
        philox4x32_ctr_t r = philox4x32(*c, *k);
        steps[i].id = i;
        steps[i].rank = iproc;
        steps[i].natoms = r.v[0] % 4 + 4;
        steps[i].energy = i * 0.15;
    }
    return steps;
}

Atom* make_atoms(Timestep* steps, size_t nsteps, size_t* out_totalAtoms, int iproc) {
    size_t i, j;
    char symbols[] = {'C', 'O', 'O'};
    size_t totalAtoms = 0;
    size_t atoms_per_step, atom_idx;
    for(i = 0; i < nsteps; i++) { totalAtoms += steps[i].natoms; }
    //printf("Atoms[%d]: %ld\n", iproc, totalAtoms);
    Atom* atoms = (Atom*)malloc(totalAtoms*sizeof(Atom));
    atom_idx = 0;
    for(i = 0; i < nsteps; i++) {
        atoms_per_step = steps[i].natoms;
        //fprintf(stderr, "Atoms[%d]: %ld\n", i, atoms_per_step);
        for(j = 0; j < atoms_per_step; j++) {
            atoms[atom_idx].timestep = i;
            atoms[atom_idx].rank = iproc;
            atoms[atom_idx].symbol = symbols[j];
            atoms[atom_idx].num = j;
            atoms[atom_idx].x = i * 0.01 + j * 0.01;
            atoms[atom_idx].y = i * 0.02 + j * 0.01;
            atoms[atom_idx].z = i * 0.03 + j * 0.01;
            atoms[atom_idx].force_x = i * 0.01;
            atoms[atom_idx].force_y = i * 0.02;
            atoms[atom_idx].force_z = i * 0.03;
            atom_idx++;
        }
    }
    (*out_totalAtoms) = totalAtoms;
    return atoms;
}

void printHeader()
{ 
    fprintf(stderr, "Method\tNrows\tNatoms\tBytes\tTotal\tTransfer\tAnalysis\n");
}
void printStep(const char* method, size_t nsteps, size_t natoms, double tx, double analysis)
{
    double total = tx + analysis;
    size_t data_size = nsteps*sizeof(Timestep) + natoms*sizeof(Atom);
    fprintf(stderr, "%s\t%ld\t%ld\t%ld\t%f\t%f\t%f\n", method, nsteps, natoms, data_size, total, tx, analysis);
}
double getAnalysis(const char* name, const char** deps, size_t ndeps, long long expected)
{
    double start = when();
    const char* DRIVER = getenv("DRIVER");
    char* res = courier_request_analysis(DRIVER, name, deps, ndeps);
    double end = when();
    long long actual = atoll(res);
    //printf("#Result: %ld\t%s:(%ld)\t%d\n", expected, res, actual, actual == expected);
    assert(actual == 0); //FIXME: no expected anymore, as Spark now does a diff
    courier_free_data(res);
    return end - start;
}

int run_actual(size_t nsteps, writer w, const char* method, 
        const char* analysis_name, const char** analysis_deps, 
        size_t ndeps, int iproc, philox4x32_key_t *k, philox4x32_ctr_t *c)
{
    int nproc;
    MPI_Comm_size(MPI_COMM_WORLD, &nproc);
    int master = iproc == 0;
    //if (master) fprintf(stderr, "Run #%ld\n", progress++);
    c->v[0] = nsteps;
    size_t natoms;

    Timestep* steps = make_timesteps(nsteps, iproc, k, c);
    Atom* atoms = make_atoms(steps, nsteps, &natoms, iproc);
    //printf("Atoms: %ld\n", natoms);

    int i;
    double start, end;
    for (i = 0; i < 10; i++) {
        if (master) { start = when(); }
        w(steps, atoms, nsteps, iproc);
        MPI_Barrier(MPI_COMM_WORLD);
        if (master) { 
            end = when(); 
            printStep(method, nsteps, natoms, end - start, 
                    getAnalysis(analysis_name, analysis_deps, ndeps, nproc*nsteps));
        }
        MPI_Barrier(MPI_COMM_WORLD);
    }

    free(atoms);
    free(steps);
}

int run(int argc, char** argv, writer w, const char* method, const char* analysis_name, const char** analysis_deps, size_t ndeps)
{
    MPI_Init(&argc, &argv);

    int iproc;
    MPI_Comm_rank(MPI_COMM_WORLD, &iproc);
    if (iproc == 0) { printHeader(); }

    philox4x32_ctr_t c={{}};
    philox4x32_ukey_t uk={{}};
    uk.v[0] = iproc;
    philox4x32_key_t k = philox4x32keyinit(uk);

    size_t i = 1;
    
    //run_actual(i, w, method, analysis_name, analysis_deps, ndeps, iproc, &k, &c);
    for (i = 1/*65536;1048576*/; i < 1e7; i+=ceil(i*0.25)) run_actual(i, w, method, analysis_name, analysis_deps, ndeps, iproc, &k, &c);
    for (i = 1e7; i < 1e8; i+=ceil(i*0.25)) run_actual(i, w, method, analysis_name, analysis_deps, ndeps, iproc, &k, &c);
    MPI_Finalize();
    return 0;
}
