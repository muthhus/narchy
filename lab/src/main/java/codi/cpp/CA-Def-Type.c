/* this file is included by ca.c , codi.c , GA.c */

/********* extern defines  **********************/

/* to choose which CA to compile */
#define CODI               1
#define LIFE               2
#define COMPLEX_CELLBODIES 3
#define WHICH_CA           CODI

/* to choose which APP to compile */
#define MAXACTIV           1
#define MAXAXON            2
#define WHICH_APP          MAXACTIV

#define SPACESIZEX 16
#define SPACESIZEY 16
/* Modified by Eiji, August 26, 1997 */
#define SPACESIZEZ 16
/*#define SPACESIZEZ 32*/
#define CELLSIZE   sizeof(TCACell)
/* #define NEURON_DENSITY  0.08 */
#define NEURON_DENSITY     0.7

#define CELLWIDTH   10 /* for display (in pixels) */
#define CELLHEIGHT  10 /* for display (in pixels) */

#define GROWTHPHASE  0
#define SIGNALPHASE  1
#define NUMBERPHASES 2

#define BLANK      0
#define NEURONSEED 1
#define NEURON     1
#define AXON       2
#define DEND       4
#define AXON_SIG   2
#define DEND_SIG   4

#define MAX_GROWTHSTEPS    100
#define MAX_SIGNALSTEPS    100
/* Modified by Eiji, August 25, 1997 */
/*#define CHROMOFILENAME     "Chromo3DFile"*/
#define CHROMOFILENAME     "CAM_TEST"
#define NR_GENERATIONS     10

#define RAND_MAX           65000

/* Introduced by Eiji, August 26, 1997 */
#define AX_DEN_RATIO       3  /* Ratio of axon growth ticks/dendrite growth ticks */
#define X_WINDOW           4  /* Size of the reserved area for one neuron in the x axis */
#define Y_WINDOW           4  /* Size of the reserved area for one neuron in the y axis */
#define Z_WINDOW           2  /* Size of the reserved area for one neuron in the z axis */

/* ******** Type Declarations ************* */


struct CellStruct {
          char Type;
          char Chromo;
          char Gate;
          char Activ;
          char IOBuf[6];  /* struct { char N, W, S, E, U, D ;} IOBuf ; */
          /* Introduced by Eiji, 08/26/97 */
          char Sign;  /* bit 0 == 0 indicates IOBuf[0] is a negative synapse; == 1 indicates a + synapse and so on */
        }; 

typedef struct CellStruct TCACell;

