#
#                  Politecnico di Milano
#
#        Studente: Caravano Andrea
#
# Ultima modifica: 08/12/2023
#
#     Descrizione: Dockerfile per container Docker con catena di sviluppo e debug C/C++ e OpenMPI
#
#                  Variante Debian SID
#
#                  grazie per l'idea base a https://github.com/mfisherman/docker/tree/main/openmpi
#                  Variante personale modificata con strumenti di debug e integrazioni per CLion con Docker

FROM debian:sid

RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
    && apt-get dist-upgrade -y \
    && apt-get -y install --no-install-recommends gdb hotspot valgrind build-essential cmake libopenmpi-dev openmpi-bin valgrind-mpi \
    && apt clean
# kcachegrind e massif-visualizer tralasciate (no GUI)
# Variazione: aggiunte estensioni Valgrind per MPI

#### TEST GCC-MPICC ####
RUN gcc --version && mpicc --version

#### TEST OPENMPI INSTALLATION ####
RUN mkdir /tmp/mpi-test
WORKDIR /tmp/mpi-test
COPY test .
RUN sh test.sh
RUN rm -rf /tmp/mpi-test

#### CLEAN UP ####
WORKDIR /
RUN rm -rf /tmp/*

#### ADD DEFAULT USER ####
ARG USER=mpi
ENV USER ${USER}
RUN adduser --disabled-password ${USER}

ENV USER_HOME /home/${USER}
RUN chown -R ${USER}:${USER} ${USER_HOME}

#### CREATE WORKING DIRECTORY FOR USER ####
ARG WORKDIR=/project
ENV WORKDIR ${WORKDIR}
RUN mkdir ${WORKDIR}
RUN chown -R ${USER}:${USER} ${WORKDIR}

WORKDIR ${WORKDIR}
USER ${USER}
