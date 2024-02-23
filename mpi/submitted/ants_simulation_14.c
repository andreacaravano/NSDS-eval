#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <mpi.h>
#include <math.h>
#include <float.h>

/*
 * Group number: 14
 *
 * Group members
 *  - Andrea Caravano
 *  - Biagio Cancelliere
 *  - Marco Pozzi
 */

const float min = 0;
const float max = 1000;
const float len = max - min;
const int num_ants = 8 * 1000 * 1000;
const int num_food_sources = 10;
const int num_iterations = 500;

float random_position() {
    return (float) rand() / (float) (RAND_MAX / (max - min)) + min;
}

/*
 * Process 0 invokes this function to initialize food sources.
 */
void init_food_sources(float *food_sources) {
    for (int i = 0; i < num_food_sources; i++) {
        food_sources[i] = random_position();
    }
}

/*
 * Process 0 invokes this function to initialize the position of ants.
 */
void init_ants(float *ants) {
    for (int i = 0; i < num_ants; i++) {
        ants[i] = random_position();
    }
}

int main() {
    MPI_Init(NULL, NULL);

    int rank;
    int num_procs;
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &num_procs);

    srand(rank);

    // Allocate space in each process for food sources and ants
    const int num_ants_per_proc = num_ants / num_procs;

    // Ants array is created only for process 0 (following initialization)
    float *food_sources = (float *) malloc(sizeof(float) * num_food_sources);
    float *ants = NULL;
    if (rank == 0) {
        ants = (float *) malloc(sizeof(float) * num_ants);
    }

    // Process 0 initializes food sources and ants
    if (rank == 0) {
        init_food_sources(food_sources);
        init_ants(ants);
    }

    // Process 0 distributed food sources and ants
    MPI_Bcast(food_sources, num_food_sources, MPI_FLOAT, 0, MPI_COMM_WORLD);

    // Process array (for each process, a certain amount of ants, supposing num_ants / num_procs has rest 0)
    float *ants_proc = (float *) malloc(sizeof(float) * num_ants_per_proc);

    // Scatter position of num_ants_per_proc ants between processes (included 0)
    MPI_Scatter(ants, num_ants_per_proc, MPI_FLOAT, ants_proc, num_ants_per_proc, MPI_FLOAT, 0, MPI_COMM_WORLD);

    // Iterative simulation
    float center = 0;

    // Initial center computation (executed only at the beginning)
    float avg_ants_position_per_proc = 0.0;
    // Every process calculates its average
    for (int i = 0; i < num_ants_per_proc; i++) {
        avg_ants_position_per_proc += ants_proc[i];
    }
    avg_ants_position_per_proc /= (float) num_ants_per_proc;

    float *avg_buffer = NULL;
    if (rank == 0) {
        avg_buffer = (float *) malloc(num_procs * sizeof(float));
    }

    // Average of all processes is gathered in a buffer by process 0
    MPI_Gather(&avg_ants_position_per_proc, 1, MPI_FLOAT, avg_buffer, 1, MPI_FLOAT, 0, MPI_COMM_WORLD);

    // Compute global average (= center)
    if (rank == 0) {
        for (int i = 0; i < num_procs; i++) {
            center += avg_buffer[i];
        }
        center /= (float) num_procs;
    }

    // Broadcast initial center position
    MPI_Bcast(&center, 1, MPI_FLOAT, 0, MPI_COMM_WORLD);

    for (int iter = 0; iter < num_iterations; iter++) {
        // Initialize d1 and d2 sign multipliers
        float sgn_d1 = +1;
        float sgn_d2 = +1;

        for (int i = 0; i < num_ants_per_proc; i++) {
            // Compute d1 based on distance from food sources
            float d1 = FLT_MAX;
            int pos_found = 0;
            for (int j = 0; j < num_food_sources; j++) {
                float dist = fabsf(ants_proc[i] - food_sources[j]);
                if (dist < d1) {
                    d1 = dist;
                    pos_found = j;
                }
            }
            if ((ants_proc[i] - food_sources[pos_found]) < 0) {
                sgn_d1 = +1;
            } else sgn_d1 = -1;

            // Compute d2 based on distance from center
            float d2 = fabsf(ants_proc[i] - center);
            if ((ants_proc[i] - center) < 0) {
                sgn_d2 = +1;
            } else sgn_d2 = -1;

            // F1 and F2 calculation
            float F1 = sgn_d1 * d1 * 0.01;
            float F2 = sgn_d2 * d2 * 0.012;

            // New ant position
            ants_proc[i] = ants_proc[i] + F1 + F2;
        }

        // Center computation (the same as before the loop)
        center = 0;

        float avg_ants_position_per_proc = 0.0;
        // Every process calculates its average
        for (int i = 0; i < num_ants_per_proc; i++) {
            avg_ants_position_per_proc += ants_proc[i];
        }
        avg_ants_position_per_proc /= (float) num_ants_per_proc;

        float *avg_buffer = NULL;
        if (rank == 0) {
            avg_buffer = (float *) malloc(num_procs * sizeof(float));
        }

        // Average of all processes is gathered in a buffer by process 0
        MPI_Gather(&avg_ants_position_per_proc, 1, MPI_FLOAT, avg_buffer, 1, MPI_FLOAT, 0, MPI_COMM_WORLD);

        // Computpe global average (= center)
        if (rank == 0) {
            for (int i = 0; i < num_procs; i++) {
                center += avg_buffer[i];
            }
            center /= (float) num_procs;
        }

        // Broadcast initial center position
        MPI_Bcast(&center, 1, MPI_FLOAT, 0, MPI_COMM_WORLD);

        if (rank == 0) {
            printf("Iteration: %d - Average position: %f\n", iter, center);
        }

        MPI_Barrier(MPI_COMM_WORLD);
    }

    // Free memory
    if (rank == 0) {
        free(ants);
        free(avg_buffer);
    }
    free(food_sources);
    free(ants_proc);

    MPI_Barrier(MPI_COMM_WORLD);

    MPI_Finalize();
    return 0;
}
