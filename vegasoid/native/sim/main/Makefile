FC= ifort
CC= icc
BUILD_DIR=build
LIBS = ../deps_build/bspline-fortran/libbspline-fortran.a -L../deps_build/libcourier/ -lcourier -lgsl -lgslcblas -lm
MODS= -I../deps_build/bspline-fortran/
OPTIMIZE= -fast
INCLUDES= -I../deps_build/libcourier/
CFLAGS= -c -std=c11 $(OPTIMIZE) $(INCLUDES)
FCFLAGS= -c -std15 $(OPTIMIZE) $(MODS) -module ${BUILD_DIR}
LD= ifort -nofor_main
OBJS= $(addprefix ${BUILD_DIR}/, lib.f.o main.c.o)
DBGOBJS= $(addprefix ${BUILD_DIR}/, test.c.o)

.PHONY: all

all: sim dbg

sim: $(OBJS)
	$(LD) $^ $(LIBS) -o $@

dbg: $(DBGOBJS)
	$(LD) $^ $(LIBS) -o $@

${BUILD_DIR}/%.c.o: src/%.c ${BUILD_DIR}
	$(CC) $(CFLAGS) $< -o $@

${BUILD_DIR}/%.f.o: src/%.f90 ${BUILD_DIR}
	$(FC) $(FCFLAGS) $< -o $@

${BUILD_DIR}:
	mkdir -p ${BUILD_DIR}

clean:
	rm -rf ${BUILD_DIR}
