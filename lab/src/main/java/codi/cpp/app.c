/* the application */

#include <stdio.h>
#include <math.h>
#include "app.h"
#include "CA-Def-Type.c"

/********* defines for app.c **********************/

/******* external Globals from codi.c */

extern unsigned long               SpaceSizeX;
extern unsigned long               SpaceSizeY;
extern unsigned long               SpaceSizeZ;
extern unsigned long               NumberCells;
extern unsigned long               PlainSizeXY;
extern TCACell                    *CASpace;
extern TCACell                    *CASpaceTemp;

/********** local variables ***************/

static char                        Activity;
static int                         SignalStepCount;
static float                       StepFitness;
static int                         i, it, itt;

static int                  xt[] = {4,8,12};
static int                  yt[] = {5,10};

/********* functions **********************/

static void ResetApp(void)
{
  Activity = 0;
  SignalStepCount = 0;
  StepFitness = 0;
}

static float GetMaxFitness(void)
{
#if WHICH_APP == MAXACTIV
  return (MAX_SIGNALSTEPS * 64 ); 
#elif WHICH_APP  == MAXAXON
  return (MAX_SIGNALSTEPS * NumberCells);
#endif 
}

extern void appFeedIn(int clk)
{
  int p1, p2, A[3], B[3];
  int ix, iy, iz; /* Introduced by Eiji, Sep.2/97 */

  /* feed in some activity 
  if (CASpace->Type == NEURON) 
    CASpace->Activ++;
  if ((CASpace+NumberCells-1)->Type == NEURON) 
    (CASpace+NumberCells-1)->Activ++;
  if ((CASpace+SpaceSizeX-1)->Type == NEURON) 
    (CASpace+SpaceSizeX-1)->Activ++;
  if ((CASpace+NumberCells-SpaceSizeX)->Type == NEURON) 
    (CASpace+NumberCells-SpaceSizeX)->Activ++;
*/

/* Modified by Eiji, Sep.2/97 */
  CASpaceTemp = CASpace;
  iz = 0;
  iy = 4;
  ix = 4;
  if ((CASpaceTemp + iz*PlainSizeXY + iy*SpaceSizeX + ix)->Type == NEURON)
    (CASpaceTemp + iz*PlainSizeXY + iy*SpaceSizeX + ix)->Activ++;
  ix = 8;
  if ((CASpaceTemp + iz*PlainSizeXY + iy*SpaceSizeX + ix)->Type == NEURON)
    (CASpaceTemp + iz*PlainSizeXY + iy*SpaceSizeX + ix)->Activ++;
  ix = 12;
  if ((CASpaceTemp + iz*PlainSizeXY + iy*SpaceSizeX + ix)->Type == NEURON)
    (CASpaceTemp + iz*PlainSizeXY + iy*SpaceSizeX + ix)->Activ++;
  iy = 8;
  ix = 4;
  if ((CASpaceTemp + iz*PlainSizeXY + iy*SpaceSizeX + ix)->Type == NEURON)
    (CASpaceTemp + iz*PlainSizeXY + iy*SpaceSizeX + ix)->Activ++;
  ix = 8;
  if ((CASpaceTemp + iz*PlainSizeXY + iy*SpaceSizeX + ix)->Type == NEURON)
    (CASpaceTemp + iz*PlainSizeXY + iy*SpaceSizeX + ix)->Activ++;
  ix = 12;
  if ((CASpaceTemp + iz*PlainSizeXY + iy*SpaceSizeX + ix)->Type == NEURON)
    (CASpaceTemp + iz*PlainSizeXY + iy*SpaceSizeX + ix)->Activ++;

  /*
  for(iz=0; iz<SpaceSizeZ; iz++)
    for(iy=0; iy<SpaceSizeY; iy++)
	for(ix=0; ix<SpaceSizeX; ix++) {
	  if ((iz == 0) && (iy == 4) && (CASpaceTemp->Type == NEURON)) {
	    if ((ix == 4) || (ix == 8) || (ix == 12))
	      if (CASpaceTemp->Type == NEURON)
	        CASpaceTemp->Activ++;
	  }
	  if ((iz == 0) && (iy == 8) && (CASpaceTemp->Type == NEURON)) {
	    if ((ix == 4) || (ix == 8) || (ix == 12))
	      if (CASpaceTemp->Type == NEURON)
	        CASpaceTemp->Activ++;
	  }
	  CASpaceTemp++;
	}
  CASpaceTemp = CASpace;
  */
  /*
  for(it=0; it<3; it++) {
    CASpaceTemp = CASpace;
    CASpaceTemp += ((5*SPACESIZEX)+xt[it]);
    if (CASpaceTemp->Type == NEURON)
@@@ comment      if(A[it] == 1)  
      CASpaceTemp->Activ++;
  }

  for(it=0; it<3; it++) {
    CASpaceTemp = CASpace;
    CASpaceTemp += ((10*SPACESIZEX)+xt[it]);
    if (CASpaceTemp->Type == NEURON)
@@@ comment     if(B[it] == 1)  
      CASpaceTemp->Activ++;
  } 
  */ 
}


static char appIO(int clk)
{

  int i, p1, p2, AgeBflag;
  


/* feed in some activity */
   appFeedIn(clk);
  /* max activity change in a neuron that has to be 
   * in the middle of the CA-space */

/* if output cell is high */

  /*i = ((SPACESIZEX*SPACESIZEY*(SPACESIZEZ-1))+(8*SPACESIZEX)+7);*.  

  /*i = 8*PlainSizeXY+8*SpaceSizeX+(SpaceSizeX-1);*/  /*(15,8,8)=(x,y,z) output on side */
  /*i = 8*PlainSizeXY+8*SpaceSizeX+(SpaceSizeX-2);*/ /*(15,8,8)=(x,y,z) output on side */
  /*i = 15*PlainSizeXY+7*SpaceSizeX+SpaceSizeX/2;*/  /*(8,8,15)=(x,y,z) output on side */
    i = 7*PlainSizeXY+7*SpaceSizeX+SpaceSizeX/2;  /*(8,8,15)=(x,y,z) output on side */
  /* Added by Eiji, Sep.23/97 */
  CASpaceTemp = CASpace; 

    if(((CASpaceTemp+i)->Type)==DEND) {
      if (((CASpaceTemp+i)->Activ)&&(clk>40)&&(clk<=70)) StepFitness =
	StepFitness+5;
      if (((!((CASpaceTemp+i)->Activ))&&(clk<41))||((!((CASpaceTemp+i)->Activ))&&(clk>70)))StepFitness ++;
      if(clk == 41) fprintf(stderr, " ");
      if(clk == 71) fprintf(stderr, " ");
      if ((CASpaceTemp+i)->Activ) fprintf(stderr, "1");
      else fprintf(stderr, "0");  
    }
return 0;
}

extern void RunApp(float *AFitness)
{
  int i, p1, p2;

  /* Added by Eiji, Sep.23/97 */
  CASpaceTemp = CASpace;
  
  ResetApp();
  ResetCA_IO();
  StepCA(MAX_GROWTHSTEPS, GROWTHPHASE);

  /* skip if output cell not a dendrite  */

    /*i = ((SPACESIZEX*SPACESIZEY*(SPACESIZEZ-1))+(8*SPACESIZEX)+7);*/  

    /*i = 8*PlainSizeXY+8*SpaceSizeX+(SpaceSizeX-1);*/
    /*i = 8*PlainSizeXY+8*SpaceSizeX+(SpaceSizeX-2);*/
    /*i = 15*PlainSizeXY+7*SpaceSizeX+SpaceSizeX/2;*/  /*(8,8,15)=(x,y,z) output on side */
    i = 7*PlainSizeXY+7*SpaceSizeX+SpaceSizeX/2;  /*(8,8,15)=(x,y,z) output on side */

    if(((CASpaceTemp+i)->Type)!=DEND) goto skipit;

    RefreshWindowDebug(1);
    ResetCA_IO();
    for(SignalStepCount = 0;SignalStepCount < MAX_SIGNALSTEPS; SignalStepCount++) {
     StepCA(1, SIGNALPHASE);
     RefreshWindowDebug(0);
     appIO(SignalStepCount); /* calculate StepFitness */
    }
 skipit: 
    RefreshWindowDebug(1);
    ResetCA_IO();
    *AFitness = StepFitness/220.0; /* norm */

}

