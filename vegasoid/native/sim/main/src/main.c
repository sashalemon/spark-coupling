#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <gsl/gsl_rng.h>
#include <gsl/gsl_randist.h>
#include <gsl/gsl_qrng.h>
#include <gsl/gsl_histogram2d.h>
#include <gsl/gsl_sf_laguerre.h>

#include <courier.h>
#include "data.h"

static const char* localhost = "127.0.0.1";
extern void splineval_init(int* nx, double *x, double *fcn_1d);
extern void spline_dump(int* nx);
extern void throw_darts(const int* ndarts, const int *nx, double *x, double *y, int* hits);
extern void classify(const int *nx, double *x, double *y, int* hits);

void report(size_t ndarts, double* x, double* y, int* hits, Point* rowvec) {
    int i;
}
void explore(const int ndarts, size_t nreps, size_t nx, size_t ny, 
        double xmin, double xmax, double ymin, double ymax) {
    double u, v;
    int i, j;
    const char* deps[] = {"Point"};
    const gsl_rng_type * T = gsl_rng_default;
    gsl_rng * r = gsl_rng_alloc (T);
    gsl_histogram2d *h = gsl_histogram2d_alloc(nx,ny);
    gsl_histogram2d_pdf *p = gsl_histogram2d_pdf_alloc(nx,ny);

    gsl_histogram2d_set_ranges_uniform(h, xmin, xmax, ymin, ymax);
    gsl_histogram2d_shift(h, 100.0);

    double *x = malloc(sizeof(double)*ndarts), 
           *y = malloc(sizeof(double)*ndarts);
    int *c = malloc(sizeof(int)*ndarts);
    Point *rowvec = malloc(sizeof(Point)*ndarts);
    int breakout = 0;
    for (j = 0; j < nreps; j++) {
        gsl_histogram2d_pdf_init(p,h);

        for (i = 0; i < ndarts; i++) {
            u = gsl_rng_uniform(r);
            v = gsl_rng_uniform(r);
            gsl_histogram2d_pdf_sample (p, u, v, &x[i], &y[i]);
        }
        classify(&ndarts,x,y,c);
        for (i = 0; i < ndarts; i++) {
            rowvec[i].x = x[i];
            rowvec[i].y = y[i];
            rowvec[i].category = c[i];
        }
        courier_write_table("Point", rowvec, sizeof(Point), ndarts, 0);
        char* res = courier_request_analysis(localhost, "Update", deps, 1);
        {
            char* token = strtok(res, " ");
            while (token != NULL) 
            { 
                size_t idx = strtod(token, NULL) - 1;
                token = strtok(NULL, " ");
                double w = strtod(token, NULL);
                h->bin[idx] = 1;//fmax(0,10 / w);
                token = strtok(NULL, " ");
            } 
            if (gsl_histogram2d_sum(h) <= nx*ny) {
                printf("Stopping due to exhausted histogram\n");
                breakout = 1;
            }
        }
        courier_free_data(res);
        if (breakout) {
            printf("Iteration %d\n",j);
            break;
        }
    }
    FILE *hf = fopen("hist.tab", "w");
    gsl_histogram2d_fprintf(hf,h,"%g","%g");
    fclose(hf);

    free(rowvec);
    free(c);
    free(x);
    free(y);
    gsl_rng_free (r);
    gsl_histogram2d_pdf_free(p);
    gsl_histogram2d_free(h);
} 
int main(int argc, char** argv) {
    int nx = 100;
    int i, j;
    double x[nx], fcn_1d[nx];
    const gsl_rng_type * T;
    gsl_rng * r;

    gsl_rng_env_setup();

    T = gsl_rng_default;
    r = gsl_rng_alloc (T);
    fcn_1d[0] = 0;
    x[0] = 0;
    for (i = 1; i < nx; i++)
    {
        x[i] = i + 1.0;
        double h = gsl_ran_levy_skew(r, 1, 1.0, 1.0);
        if (h < 1 && h > -1) { h = -5; }
        fcn_1d[i] = fmax(25,fmin(75,fcn_1d[i-1]+h));
    }
    splineval_init(&nx,x,fcn_1d);
    explore(1000, 1000, 64, 64, 0, 100, 0, 100);
    spline_dump(&nx);
    gsl_rng_free (r);
    return 0;
}
