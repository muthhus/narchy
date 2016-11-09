/* This file contains the functions for CoDi CA implementation.
 * It is used by ca.c, which controles the X-stuff.
*/

#include <stdio.h>
#include <math.h>
#include "CA-Def-Type.c"
#include "codi.h"

/********* defines  **********************/

/********* Variable Declarations **********/

unsigned long               SpaceSizeX = SPACESIZEX;
unsigned long               SpaceSizeY = SPACESIZEY;
unsigned long               SpaceSizeZ = SPACESIZEZ;
unsigned long               NumberCells;
unsigned long               PlainSizeXY;
TCACell                    *CASpace;
TCACell                    *CASpaceTemp;
TCACell                    *CASpaceStart;

/* Introduced by Eiji, 08/26/1997 */
int                         g_counter = 0;  /* to verify which growth signal should be propagated */

/********** local variables ***************/

static int                  i, ii, is, it ;
static int                  ix, iy, iz, iaux;
static int                  InputSum;

static int                  xt[] = {4,8,12};
static int                  yt[] = {5,10};


/********* external Globals  *****/


/********* Init ***************************/

static char Alloc3DSpace(void)
{
  NumberCells = SpaceSizeX*SpaceSizeY*SpaceSizeZ;  
  PlainSizeXY = SpaceSizeY*SpaceSizeX;
  CASpace = (TCACell*) 
            calloc(NumberCells, CELLSIZE) ;
  if (CASpace) return 0; else return 1;
}


extern void InitCASpace(void)
{
  int flag;
  int kk;

CASpaceTemp = CASpace;
  for(i=0;i<NumberCells;i++){
/*    CASpaceTemp->Chromo = (char) random()&255; */

    CASpaceTemp->Chromo = 0;
    for(kk=0; kk<6; kk++) {
      if (0.194 > (fmod(random(),RAND_MAX)/RAND_MAX)) /* 1/6 + 1/36 */
      CASpaceTemp->Chromo |= (1 << kk);
    }

    CASpaceTemp->Type   = BLANK;
    CASpaceTemp->Gate   = 0;
    CASpaceTemp->Activ  = 0;
    CASpaceTemp->Sign   = 0;
    for(ii=0; ii<6; ii++) 
      CASpaceTemp->IOBuf[ii] = 0; 
    CASpaceTemp++;
  }

  CASpaceTemp = CASpace;
  /* Modification by Eiji, Sep.1/97 */
  for (iz=0; iz<SpaceSizeZ; iz++)
    for (iy=0; iy<SpaceSizeY; iy++)
      for (ix=0;ix<SpaceSizeX; ix++) {
  /*for(i=0;i<NumberCells;i++){*/
    CASpaceTemp->Chromo &= 63;
    /* Modified by Eiji, Sep.1/97 */
    /* Neurons have to be positioned in a 2x2x4 grid in the space */
    if (NEURON_DENSITY > (fmod(random(),RAND_MAX)/RAND_MAX))
      if ((ix%X_WINDOW == 0) && (iy%Y_WINDOW == 0) && (iz%Z_WINDOW == 0))
	CASpaceTemp->Chromo |= (NEURONSEED << 6);
    /*
    if (NEURON_DENSITY > (fmod(random(),RAND_MAX)/RAND_MAX))       
      CASpaceTemp->Chromo |= (NEURONSEED << 6); 
    */
    /*
    if ((ix == 0) && (iy == 4))
      if ((iz == 2) || (iz == 4) || (iz == 6) || (iz == 10) || (iz == 12) || (iz == 14))
        CASpaceTemp->Chromo |= (NEURONSEED << 6); 
    */
    if ((iz == 0) && (iy == 4)) {
	if ((ix == 4) || (ix == 8) || (ix == 12))
	    CASpaceTemp->Chromo |= (NEURONSEED << 6); 
    }
    if ((iz == 0) && (iy == 8)) {
	if ((ix == 4) || (ix == 8) || (ix == 12))
	    CASpaceTemp->Chromo |= (NEURONSEED << 6); 
    }

    CASpaceTemp++;
  } 
/*
  for(it=0; it<3; it++) {
    CASpaceTemp = CASpace;
    CASpaceTemp += ((5*SPACESIZEX)+xt[it]);
    CASpaceTemp->Chromo |= (NEURONSEED << 6);
  }

  for(it=0; it<3; it++) {
    CASpaceTemp = CASpace;
    CASpaceTemp += ((10*SPACESIZEX)+xt[it]);
    CASpaceTemp->Chromo |= (NEURONSEED << 6);
  }
*/

}

extern void ResetCA_IO(void)
{
  CASpaceTemp = CASpace;
  for(i=0;i<NumberCells;i++){
    CASpaceTemp->Activ  = 0;
    for(ii=0; ii<6; ii++) 
      CASpaceTemp->IOBuf[ii] = 0; 
    CASpaceTemp++;
  }
}

extern char InitCA(TCACell **ACASpace)
{
  /* !!!!!! Pointer + 1 = Pointer++ 
   * : increase by a sizeof(PointerToType) */
  if(Alloc3DSpace()) return 1;
  *ACASpace = CASpace;
  /* read Chromosome */
  InitCASpace();
  return 0;
}

extern void FreeCASpace(void)
{
  if (CASpace) free(CASpace);
}

/********* CASteps and Rules ************/



static void Kicking(void) /* For the Neighborhood interaction */
/* not the most efficient way, but you don't loose the overview */
/* an other way is to work with a CAM-8 like offset register */
{
  /* von Neumann Neighborhood 
   * Names for the buffer correspond to the I-Buf,
   * so where the Information (signal) came from;
   * In the north buffer (+y=2)of a cell is either what came
   * from the norht (after kick) or the what will 
   * go to the south (before kick).
   * IOBuf[0..5] = east(+x), west(-x), north(+y), south(-y),
   * top(+z), bottom(-z) 
   * actulize the 3 direction one at a time, without wrap-around
   * Ptr +1 = Prt++ !!! 
   */
  /* For the positive directions */
  CASpaceTemp = CASpace;
  CASpaceStart = CASpace;

  for(iz=0;iz<SpaceSizeZ;iz++)
    for(iy=0;iy<SpaceSizeY;iy++)
      for(ix=0;ix<SpaceSizeX;ix++) {
        CASpaceTemp->IOBuf[4] = 0;
	CASpaceTemp->IOBuf[2] = 0;
	CASpaceTemp->IOBuf[0] = 0;
	for(i = 0;i < sizeof(char)*8;i++){

	  /* top (+z) */
	  if(iz!=SpaceSizeZ-1)
	    CASpaceTemp->IOBuf[4] |= 
	      (CASpaceTemp+PlainSizeXY)->IOBuf[4] & (1 << i); /*[ix][iy][iz+1]*/
	  else 
      	    CASpaceTemp->IOBuf[4] = 0;
	    /*CASpaceTemp->IOBuf[4] |= (CASpaceStart+(iy*SpaceSizeX)+ix)->IOBuf[4] & (1 << i);*/
            /*[ix][iy][0]*/ 

	  /* north (+y) */
	  if(iy!=SpaceSizeY-1)
	    CASpaceTemp->IOBuf[2] |= 
	      (CASpaceTemp+SpaceSizeX)->IOBuf[2] & (1 << i); /*[ix][iy+1][iz]*/
	  else 
	    CASpaceTemp->IOBuf[2] = 0;
	    /*CASpaceTemp->IOBuf[2] |= (CASpaceStart+(iz*PlainSizeXY)+ix)->IOBuf[2] & (1 << i);*/
            /*[ix][0][iz]*/ 

	  /* east (+x) */
	  if(ix!=SpaceSizeX-1)
	    CASpaceTemp->IOBuf[0] |= 
	      (CASpaceTemp+1)->IOBuf[0] & (1 << i); /*[ix+1][iy][iz]*/
	  else 
	    CASpaceTemp->IOBuf[0] = 0;
	    /*CASpaceTemp->IOBuf[0] |= (CASpaceStart+(iz*PlainSizeXY)+(iy*SpaceSizeX))->IOBuf[0] & (1 << i);*/
            /*[0][iy][iz]*/   

	  }
	CASpaceTemp++;
      }
  /* For the negative directions */
  /* CASpaceTemp pionts to the last cell now */
  /*  PrintCellInfo(CASpaceTemp-CASpace);*/
  for(iz=0;iz<SpaceSizeZ;iz++)
    for(iy=0;iy<SpaceSizeY;iy++)
      for(ix=0;ix<SpaceSizeX;ix++) {
        CASpaceTemp--;
	CASpaceTemp->IOBuf[5] = 0;
	CASpaceTemp->IOBuf[3] = 0;
	CASpaceTemp->IOBuf[1] = 0;
        for(i = 0;i < sizeof(char)*8;i++){

	/* bottom (-z) */
	  /*	if(iz!=SpaceSizeZ-1)   */
	  if(iz!=0)
            CASpaceTemp->IOBuf[5] |= (CASpaceStart+PlainSizeXY*(iz-1)+(iy*SpaceSizeX)+ix)->IOBuf[5] & (1 << i);
	    /*CASpaceTemp->IOBuf[5] |= (CASpaceTemp-PlainSizeXY)->IOBuf[5] & (1 << i);*/ /*[ix][iy][iz-1]*/
          else 
	    CASpaceTemp->IOBuf[5] = 0;

	  /*  [ix][iy][maxZ]*/  

        /* south (-y) */
	  /*      if(iy!=SpaceSizeY-1)   */
          if(iy!=0)
            CASpaceTemp->IOBuf[3] |= (CASpaceStart+iz*PlainSizeXY+(iy-1)*SpaceSizeX+ix)->IOBuf[3] & (1 << i); /*[ix][iy-1][iz]*/
	    /*CASpaceTemp->IOBuf[3] |= (CASpaceTemp-SpaceSizeX)->IOBuf[3] & (1 << i);*/ /*[ix][iy-1][iz]*/
          else 
	    CASpaceTemp->IOBuf[3] = 0;
	    /*CASpaceTemp->IOBuf[3] |= CASpaceStart+iz*PlainSizeXY+(SpaceSizeY-1)*SpaceSizeX+ix)->IOBuf[3] & (1 << i); */
	        /*  [ix][maxY][iz]*/
		

        /* west (-x) */
	  /*     if(ix!=SpaceSizeX-1)   */
          if(ix!=0)
	    CASpaceTemp->IOBuf[1] |= (CASpaceStart+iz*PlainSizeXY+(iy*SpaceSizeX)+(ix-1))->IOBuf[1] & (1 << i); /*[ix-1][iy][iz]*/
	    /*CASpaceTemp->IOBuf[1] |= (CASpaceTemp-1)->IOBuf[1] & (1 << i);*/ /*[ix-1][iy][iz]*/
          else
	    CASpaceTemp->IOBuf[1] = 0;
	    /*CASpaceTemp->IOBuf[1] |=   (CASpaceStart+iz*PlainSizeXY+iy*SpaceSizeX+SpaceSizeX-1)->IOBuf[1] & (1 << i); */

/*  [maxX][iy][iz]*/

	}
/*        CASpaceTemp--;*/
      }
}

static void WrappedKickingGrowing(void)
{
  /* Kicking with wrap-around...
   * Names for the buffer correspond to the I-Buf,
   * where the Information (signal) came from;
   * In the north buffer (+y=2) of a cell is either what came
   * from the north (after kick) or the sign that will 
   * go to the south (before kick).
   * IOBuf[0..5] = east(+x), west(-x), north(+y), south(-y),
   * top(+z), bottom(-z) 
   */

   /* Temporary variables for the wrap-around */
  char TempX, TempY[SpaceSizeX], TempZ;

  /* For the positive directions */
  CASpaceTemp = CASpace;
  CASpaceStart = CASpace;

  for(iz=0;iz<SpaceSizeZ;iz++)
    for(iy=0;iy<SpaceSizeY;iy++)
      for(ix=0;ix<SpaceSizeX;ix++) {
	/*  
        CASpaceTemp->IOBuf[4] = 0;
	CASpaceTemp->IOBuf[2] = 0;
	CASpaceTemp->IOBuf[0] = 0;
	*/

	for (i = 0; i < sizeof(char)*8; i++) {
	  /* top (+z) */
	  if (iz == 0)
	    TempZ = CASpaceTemp->IOBuf[4];
	  if (iz!=SpaceSizeZ-1)
	    CASpaceTemp->IOBuf[4] |= (CASpaceStart+(iz+1)*PlainSizeXY+iy*SpaceSizeX+ix)->IOBuf[4] & (1 << i); /*[ix][iy][iz+1]*/
	  else 
      	    CASpaceTemp->IOBuf[4] = 0;  

	  /* north (+y) */
	  if (iy == 0)
	    TempY[ix] = CASpaceTemp->IOBuf[2];
	  if (iy!=SpaceSizeY-1)
	    CASpaceTemp->IOBuf[2] |= (CASpaceStart+iz*PlainSizeXY+(iy+1)*SpaceSizeX+ix)->IOBuf[2] & (1 << i);  /*[ix][iy+1][iz]*/
	  else 
	    CASpaceTemp->IOBuf[2] |= TempY[ix] & (1 << i);  /* Wrap-around, [ix][0][iz]*/ 

	  /* east (+x) */
	  if (ix == 0)
	    TempX = CASpaceTemp->IOBuf[0];
	  if (ix!=SpaceSizeX-1)
	    CASpaceTemp->IOBuf[0] |= (CASpaceStart+iz*PlainSizeXY+iy*SpaceSizeX+ix+1)->IOBuf[0] & (1 << i);  /*[ix+1][iy][iz]*/
	  else 
	    CASpaceTemp->IOBuf[0] |= TempX & (1 << i);  /* Wrap-around, [0][iy][iz]*/   
	}
	CASpaceTemp++;
      }
  /* For the negative directions */
  /* CASpaceTemp points to the last cell now */
  for(iz=SpaceSizeZ-1;iz>=0;iz--)
    for(iy=SpaceSizeY-1;iy>=0;iy--)
      for(ix=SpaceSizeX-1;ix>=0;ix--) {
        CASpaceTemp--;
	/*
	CASpaceTemp->IOBuf[5] = 0;
	CASpaceTemp->IOBuf[3] = 0;
	CASpaceTemp->IOBuf[1] = 0;
	*/
	for (i = 0; i < sizeof(char)*8; i++) {
	/* bottom (-z) */
	  if (iz == SpaceSizeZ-1)   
	    TempZ = CASpaceTemp->IOBuf[5];
	  if(iz!=0)
            CASpaceTemp->IOBuf[5] |= (CASpaceStart+PlainSizeXY*(iz-1)+(iy*SpaceSizeX)+ix)->IOBuf[5] & (1 << i);
	    /*[ix][iy][iz-1]*/
          else 
	    CASpaceTemp->IOBuf[5] = 0;  

        /* south (-y) */
	  if(iy == SpaceSizeY-1)   
	    TempY[ix] = CASpaceTemp->IOBuf[3]; 
          if(iy!=0)
            CASpaceTemp->IOBuf[3] |= (CASpaceStart+iz*PlainSizeXY+(iy-1)*SpaceSizeX+ix)->IOBuf[3] & (1 << i); 
            /*[ix][iy-1][iz]*/
          else 
	    CASpaceTemp->IOBuf[3] |= TempY[ix] & (1 << i) ;  /* Wrap-around, [ix][maxY][iz]*/
		

        /* west (-x) */
	  if(ix == SpaceSizeX-1)   
	    TempX = CASpaceTemp->IOBuf[1];
          if(ix!=0)
	    CASpaceTemp->IOBuf[1] |= (CASpaceStart+iz*PlainSizeXY+(iy*SpaceSizeX)+(ix-1))->IOBuf[1] & (1 << i); 
	    /*[ix-1][iy][iz]*/
          else
	    CASpaceTemp->IOBuf[1] |= TempX & (1 << i);  /* Wrap-around, [maxX][iy][iz]*/
	}
      }
    CASpaceTemp = CASpace;
}

static void WrappedKickingSignaling(void)
{
  /* Kicking with wrap-around...
   * Names for the buffer correspond to the I-Buf,
   * where the Information (signal) came from;
   * In the north buffer (+y=2) of a cell is either what came
   * from the north (after kick) or the sign that will 
   * go to the south (before kick).
   * IOBuf[0..5] = east(+x), west(-x), north(+y), south(-y),
   * top(+z), bottom(-z) 
   */

   /* Temporary variables for the wrap-around */
  char TempX, TempY[SpaceSizeX], TempZ;

  /* For the positive directions */
  CASpaceTemp = CASpace;
  CASpaceStart = CASpace;

  for(iz=0;iz<SpaceSizeZ;iz++)
    for(iy=0;iy<SpaceSizeY;iy++)
      for(ix=0;ix<SpaceSizeX;ix++) {
	/*  
        CASpaceTemp->IOBuf[4] = 0;
	CASpaceTemp->IOBuf[2] = 0;
	CASpaceTemp->IOBuf[0] = 0;
	*/
	  /* top (+z) */
	  if (iz == 0)
	    TempZ = CASpaceTemp->IOBuf[4];
	  if (iz!=SpaceSizeZ-1)
	    CASpaceTemp->IOBuf[4] = (CASpaceStart+(iz+1)*PlainSizeXY+iy*SpaceSizeX+ix)->IOBuf[4]; /*[ix][iy][iz+1]*/
	  else 
      	    CASpaceTemp->IOBuf[4] = 0;  

	  /* north (+y) */
	  if (iy == 0)
	    TempY[ix] = CASpaceTemp->IOBuf[2];
	  if (iy!=SpaceSizeY-1)
	    CASpaceTemp->IOBuf[2] = (CASpaceStart+iz*PlainSizeXY+(iy+1)*SpaceSizeX+ix)->IOBuf[2];  /*[ix][iy+1][iz]*/
	  else 
	    CASpaceTemp->IOBuf[2] = TempY[ix];  /* Wrap-around, [ix][0][iz]*/ 

	  /* east (+x) */
	  if (ix == 0)
	    TempX = CASpaceTemp->IOBuf[0];
	  if (ix!=SpaceSizeX-1)
	    CASpaceTemp->IOBuf[0] = (CASpaceStart+iz*PlainSizeXY+iy*SpaceSizeX+ix+1)->IOBuf[0];  /*[ix+1][iy][iz]*/
	  else 
	    CASpaceTemp->IOBuf[0] = TempX;  /* Wrap-around, [0][iy][iz]*/   
	CASpaceTemp++;
      }
  /* For the negative directions */
  /* CASpaceTemp points to the last cell now */
  for(iz=SpaceSizeZ-1;iz>=0;iz--)
    for(iy=SpaceSizeY-1;iy>=0;iy--)
      for(ix=SpaceSizeX-1;ix>=0;ix--) {
        CASpaceTemp--;
	/*
	CASpaceTemp->IOBuf[5] = 0;
	CASpaceTemp->IOBuf[3] = 0;
	CASpaceTemp->IOBuf[1] = 0;
	*/
	/* bottom (-z) */
	  if (iz == SpaceSizeZ-1)   
	    TempZ = CASpaceTemp->IOBuf[5];
	  if(iz!=0)
            CASpaceTemp->IOBuf[5] = (CASpaceStart+PlainSizeXY*(iz-1)+(iy*SpaceSizeX)+ix)->IOBuf[5];
	    /*[ix][iy][iz-1]*/
          else 
	    CASpaceTemp->IOBuf[5] = 0;  

        /* south (-y) */
	  if(iy == SpaceSizeY-1)   
	    TempY[ix] = CASpaceTemp->IOBuf[3]; 
          if(iy!=0)
            CASpaceTemp->IOBuf[3] = (CASpaceStart+iz*PlainSizeXY+(iy-1)*SpaceSizeX+ix)->IOBuf[3]; 
            /*[ix][iy-1][iz]*/
          else 
	    CASpaceTemp->IOBuf[3] = TempY[ix];  /* Wrap-around, [ix][maxY][iz]*/
		

        /* west (-x) */
	  if(ix == SpaceSizeX-1)   
	    TempX = CASpaceTemp->IOBuf[1];
          if(ix!=0)
	    CASpaceTemp->IOBuf[1] = (CASpaceStart+iz*PlainSizeXY+(iy*SpaceSizeX)+(ix-1))->IOBuf[1]; 
	    /*[ix-1][iy][iz]*/
          else
	    CASpaceTemp->IOBuf[1] = TempX;  /* Wrap-around, [maxX][iy][iz]*/
      }
    CASpaceTemp = CASpace;
}

static char GrowthStep(int ANumberSteps)
{ 
  char CAChanged = 0;

  for(is=0; is<ANumberSteps; is++){
    CASpaceTemp = CASpace;
     for(iz=0;iz<SpaceSizeZ;iz++)
       for(iy=0;iy<SpaceSizeY;iy++)
         for(ix=0;ix<SpaceSizeX;ix++) {
           /*** now the Rules ***/
	   /* the Chromo is a bitmask for the 6 directions[Bit0..5]:
            * east(+x), west(-x), north(+y), south(-y),
            * top(+z), bottom(-z) 
           /* As the direction are to be seen as input directios,
            * the Gate and Chromo are Masks tha invert the directions
            * when applied to output selection */
           /* look at the input */
           switch(CASpaceTemp->Type) {
	     case BLANK :                    /* BLANK */
               /* see if it is a NeuronSeed (in bit ..7,8 of Chromo) */
               if (((CASpaceTemp->Chromo >> 6) == NEURONSEED) && (ix%X_WINDOW == 0) && (iy%Y_WINDOW == 0) && (iz%Z_WINDOW == 0)) {
                 CASpaceTemp->Type = NEURON;
                 CAChanged = 1;
                 /* and inform the neighbors immediately */
                 if(SpaceSizeZ == 1)
		     CASpaceTemp->Gate = (char) CASpaceTemp->Chromo % 4;
		 else
		     CASpaceTemp->Gate = (char) CASpaceTemp->Chromo % 6;
		 /* Modification by Eiji, 08/26/97 */
		 if (g_counter < AX_DEN_RATIO) {
		   for(i=0; i<6; i++) 
                     CASpaceTemp->IOBuf[i] = 0;
                   CASpaceTemp->IOBuf[CASpaceTemp->Gate] = AXON_SIG;
		   /*
                   CASpaceTemp->IOBuf\
		     [((CASpaceTemp->Gate%2)*-2)+1+CASpaceTemp->Gate] 
		      = AXON_SIG;
		   */
		 }
		 else {
		   for(i=0; i<6; i++) 
                     CASpaceTemp->IOBuf[i] = DEND_SIG;
                   CASpaceTemp->IOBuf[CASpaceTemp->Gate] = 0;
		   /*
                   CASpaceTemp->IOBuf\
		     [((CASpaceTemp->Gate%2)*-2)+1+CASpaceTemp->Gate] 
		      = 0;
		    */
		 }
		 /*
                 for(i=0; i<6; i++) 
                   CASpaceTemp->IOBuf[i] = DEND_SIG;
                 CASpaceTemp->IOBuf[CASpaceTemp->Gate] = AXON_SIG;
                 CASpaceTemp->IOBuf\
		   [((CASpaceTemp->Gate%2)*-2)+1+CASpaceTemp->Gate] 
		    = AXON_SIG;
		 */
/*               PrintCellInfo(iz*SpaceSizeY*SpaceSizeX+iy*SpaceSizeX+ix);*/
		 break;
	       }
               /* Test for no signal */
               InputSum =  
                   CASpaceTemp->IOBuf[0]
	         + CASpaceTemp->IOBuf[1]
	         + CASpaceTemp->IOBuf[2]
                 + CASpaceTemp->IOBuf[3]
	         + CASpaceTemp->IOBuf[4]
	         + CASpaceTemp->IOBuf[5];
               if(InputSum == 0) break;
               /* Test for AXON_SIG's */
	       InputSum =  
		   (CASpaceTemp->IOBuf[0] & AXON_SIG)
		   + (CASpaceTemp->IOBuf[1] & AXON_SIG)
		   + (CASpaceTemp->IOBuf[2] & AXON_SIG)
		   + (CASpaceTemp->IOBuf[3] & AXON_SIG)
		   + (CASpaceTemp->IOBuf[4] & AXON_SIG)
		   + (CASpaceTemp->IOBuf[5] & AXON_SIG);
	       if(InputSum == AXON_SIG) {/* exactly one AXON_SIG */
		   CASpaceTemp->Type = AXON;
		   CAChanged = 1;
		   for(i=0; i<6; i++) 
		       /* Modified by Eiji, 08/26/97, probably a typo, but it doesnt change much */
		       /*if (CASpaceTemp->IOBuf[i] == AXON)*/
		       if (CASpaceTemp->IOBuf[i] == AXON_SIG)
			   CASpaceTemp->Gate = i;
		   if (g_counter < AX_DEN_RATIO) {  /* Introduced by Eiji, 08/27/97 */
		     for(i=0; i<6; i++) 
                       if ((CASpaceTemp->Chromo >> i) & 1)
			   CASpaceTemp->IOBuf[i] = AXON_SIG;
		       else CASpaceTemp->IOBuf[i] = 0;
		   }
		   else {
		     for(i=0; i<6; i++)
		       CASpaceTemp->IOBuf[i] = 0;
		   }
		   break;
	       }
	       if(InputSum > AXON_SIG) {/* more than one AXON_SIG */
		   for(i=0; i<6; i++) 
		       CASpaceTemp->IOBuf[i] = 0;
		   break;
	       }
	       /* Test for DEMD_SIG's */
	       InputSum =  
		   (CASpaceTemp->IOBuf[0] & DEND_SIG)
		   + (CASpaceTemp->IOBuf[1] & DEND_SIG)
		   + (CASpaceTemp->IOBuf[2] & DEND_SIG)
		   + (CASpaceTemp->IOBuf[3] & DEND_SIG)
		   + (CASpaceTemp->IOBuf[4] & DEND_SIG)
		   + (CASpaceTemp->IOBuf[5] & DEND_SIG);
	       if(InputSum == DEND_SIG) {/* exactly one DEND_SIG */
		   CAChanged = 1;
                   CASpaceTemp->Type = DEND;
		   for(i=0; i<6; i++) 
		       if (CASpaceTemp->IOBuf[i])
			   CASpaceTemp->Gate = ((i%2)*-2)+1+i;
		   if (g_counter == AX_DEN_RATIO) {  /* Introduced by Eiji, 08/26/97 */
		     for(i=0; i<6; i++) 
		       if ((CASpaceTemp->Chromo >> i) & 1)
			   CASpaceTemp->IOBuf[i] = DEND_SIG;
		       else CASpaceTemp->IOBuf[i] = 0;
		   }
		   else {
		     for(i=0; i<6; i++)
		       CASpaceTemp->IOBuf[i] = 0;
		   }
                   break;
	       }
               /* default(more than one DEND_SIG and no AXON_SIG) */
               for(i=0; i<6; i++) 
 	         CASpaceTemp->IOBuf[i] = 0;
               break;
             case NEURON :                       /* NEURON */
	       /* Modified by Eiji, August 26, 1997 */
	       /* Implements the axon growth AX_DEN_RATIO clocks - dendrite growth 1 clock ratio */
	       if (g_counter < AX_DEN_RATIO) {  /* AXON growith signal is propagated */
	         for(i=0; i<6; i++) 
                   CASpaceTemp->IOBuf[i] = 0;
                 CASpaceTemp->IOBuf[CASpaceTemp->Gate] = AXON_SIG;
		 /*
                 CASpaceTemp->IOBuf\
                   [((CASpaceTemp->Gate%2)*-2)+1+CASpaceTemp->Gate] 
                   = AXON_SIG;
		 */
	       }
	       else {  /* DENDRITE growth signal is propagated */
	         for (i=0; i<6; i++)
		   CASpaceTemp->IOBuf[i] = DEND_SIG;
		 CASpaceTemp->IOBuf[CASpaceTemp->Gate] = 0;
		 /*
		 CASpaceTemp->IOBuf\
		     [((CASpaceTemp->Gate%2)*-2)+1+CASpaceTemp->Gate] 
		     = 0;
	         */
	       }
	       break;
	       /*
	       for(i=0; i<6; i++) 
                 CASpaceTemp->IOBuf[i] = DEND_SIG;
               CASpaceTemp->IOBuf[CASpaceTemp->Gate] = AXON_SIG;
               CASpaceTemp->IOBuf\
                 [((CASpaceTemp->Gate%2)*-2)+1+CASpaceTemp->Gate] 
                 = AXON_SIG;
               break;
	       */
             case AXON :                         /* AXON */
	       /* Modified by Eiji, 08/27/97 */
	       if (g_counter < AX_DEN_RATIO) {
	         for(i=0; i<6; i++) 
		   if ((CASpaceTemp->Chromo >> i) & 1)
                     CASpaceTemp->IOBuf[i] = AXON_SIG;
                   else CASpaceTemp->IOBuf[i] = 0;
	       }
	       else 
	         for(i=0; i<6; i++)
		   CASpaceTemp->IOBuf[i] = 0;
               break;
	       /*
               for(i=0; i<6; i++) 
                 if ((CASpaceTemp->Chromo >> i) & 1)
                   CASpaceTemp->IOBuf[i] = AXON_SIG;
                 else CASpaceTemp->IOBuf[i] = 0;
               break;
	       */
             case DEND :                         /* DENDRITE */
	       /* Modified by Eiji, 08/27/97 */
	       if (g_counter == AX_DEN_RATIO) {
	         for(i=0; i<6; i++) 
		   if ((CASpaceTemp->Chromo >> i) & 1)
                     CASpaceTemp->IOBuf[i] = DEND_SIG;
		   else CASpaceTemp->IOBuf[i] = 0;
	       }
	       else  
	         for(i=0; i<6; i++)
		   CASpaceTemp->IOBuf[i] = 0;
               break;
	       /*
               for(i=0; i<6; i++) 
                 if ((CASpaceTemp->Chromo >> i) & 1)
                   CASpaceTemp->IOBuf[i] = DEND_SIG;
                 else CASpaceTemp->IOBuf[i] = 0;
               break;
	       */
           }
           CASpaceTemp++;
	 }
    /* Introduced by Eiji, 08/27/97 */
    if (g_counter == AX_DEN_RATIO)
      g_counter = 0;
    else
      g_counter++;

    WrappedKickingGrowing(); 
    if (!CAChanged) return 1;
    else CAChanged = 0;
  }
  return 0;
}

static char SignalStep(int ANumberSteps)
{
  for(is=0; is<ANumberSteps; is++){
    CASpaceTemp = CASpace;
    /* feed in some activity */
     appFeedIn(is);
     for(iz=0;iz<SpaceSizeZ;iz++)
       for(iy=0;iy<SpaceSizeY;iy++)
         for(ix=0;ix<SpaceSizeX;ix++) {
           /*** now the Rules ***/
           switch(CASpaceTemp->Type) {
	     case BLANK :                    /* BLANK */
               for(i=0; i<6; i++) 
 	         CASpaceTemp->IOBuf[i] = 0;
               break;
             case NEURON :                   /* NEURON */
	       /* Modified by Eiji, 08/27/97 */
	       InputSum = 0;                 /* add default gain */
	       for(i=0; i<6; i++) {
	         if (i != (CASpaceTemp->Gate)) {
		   if ((CASpaceTemp->Sign >> i) & 1)
		     InputSum += CASpaceTemp->IOBuf[i];
		   else
		     InputSum -= CASpaceTemp->IOBuf[i];
		 }
	       }
               /*
               InputSum =  0                 
                 + CASpaceTemp->IOBuf[0]
	         + CASpaceTemp->IOBuf[1]
	         + CASpaceTemp->IOBuf[2]
                 + CASpaceTemp->IOBuf[3]
	         + CASpaceTemp->IOBuf[4]
	         + CASpaceTemp->IOBuf[5] 
                 - CASpaceTemp->IOBuf[CASpaceTemp->Gate];
	       */
	       /*
               InputSum =  0              
                 + CASpaceTemp->IOBuf[0]
	         + CASpaceTemp->IOBuf[1]
	         + CASpaceTemp->IOBuf[2]
                 + CASpaceTemp->IOBuf[3]
	         + CASpaceTemp->IOBuf[4]
	         + CASpaceTemp->IOBuf[5] 
                 - CASpaceTemp->IOBuf[CASpaceTemp->Gate]
                 - CASpaceTemp->IOBuf\
                   [((CASpaceTemp->Gate%2)*-2)+1+CASpaceTemp->Gate];
	       */
               for(i=0; i<6; i++) 
                 CASpaceTemp->IOBuf[i] = 0;
               CASpaceTemp->Activ += InputSum;
        /*       if (CASpaceTemp->Activ > 15 || CASpaceTemp->Activ < -16) { */
	/*       */
	       /* Modified by Eiji, 08/27/97 */
	       if (CASpaceTemp->Activ > 2) {  /* Neuron fires if activation level is higher than 2 then resets */
	         CASpaceTemp->IOBuf[CASpaceTemp->Gate] = 1;
		 CASpaceTemp->Activ = 0;
	       }
	       if (CASpaceTemp->Activ < -8)  /* Neuron resets if activation level is less than -8 */
	         CASpaceTemp->Activ = 0;
	       /*
	       if (CASpaceTemp->Activ > 2 || CASpaceTemp->Activ < -16) {
                 CASpaceTemp->IOBuf[CASpaceTemp->Gate] = 1;
                 CASpaceTemp->IOBuf\
                   [((CASpaceTemp->Gate%2)*-2)+1+CASpaceTemp->Gate] 
                   = -1;
                 CASpaceTemp->Activ = 0;
	       }
	       */
               break; 
             case AXON :                     /* AXON */
               for(i=0; i<6; i++) 
                 CASpaceTemp->IOBuf[i] =
                    (CASpaceTemp->IOBuf[CASpaceTemp->Gate]);
               if (CASpaceTemp->IOBuf[CASpaceTemp->Gate]) 
                 CASpaceTemp->Activ = 1;
	           else CASpaceTemp->Activ = 0; 
               break;
             case DEND :                     /* DENDRITE */
	       /* Modified by Eiji, 08/26/97 */
	       InputSum = 0;
	       for(i=0; i<6; i++) {
	         if(i != (CASpaceTemp->Gate))
		   InputSum += CASpaceTemp->IOBuf[i];
	       }
               if (InputSum)  /* if one of the 5 signals is present, sends a signal to its output */
	         InputSum = 1;
	       /*
               InputSum = 0 
                 + CASpaceTemp->IOBuf[0]
	         + CASpaceTemp->IOBuf[1]
	         + CASpaceTemp->IOBuf[2]
                 + CASpaceTemp->IOBuf[3]
	         + CASpaceTemp->IOBuf[4]
	         + CASpaceTemp->IOBuf[5] ;
		 
               if(InputSum > 2) InputSum = 2;
	       if(InputSum < -1) InputSum = -1;
               */
               for(i=0; i<6; i++) 
                 CASpaceTemp->IOBuf[i] = 0;
               CASpaceTemp->IOBuf[CASpaceTemp->Gate] = InputSum;
               if (InputSum != 0) CASpaceTemp->Activ = 1;
                 else CASpaceTemp->Activ = 0; 
               break;
           }
           CASpaceTemp++;
	 }
     WrappedKickingSignaling(); 
  }
  return 0;
}

extern char PrintCellInfo(unsigned long NrCell)
{
  if (NrCell > NumberCells) return 1;
  CASpaceTemp = CASpace+NrCell;
      fprintf(stderr, "Cell Nr. : %d\n", NrCell);
#if WHICH_CA == CODI
      fprintf(stderr, "Type     : %d\n", CASpaceTemp->Type);
      fprintf(stderr, "Chromo   : %d\n", CASpaceTemp->Chromo);
        fprintf(stderr, "ChromoBit: ");
      for(i=0;i<8;i++)
        fprintf(stderr, "%d", (CASpaceTemp->Chromo >> i) & 1);
      fprintf(stderr, "\n");
      fprintf(stderr, "Gate     : %d\n", CASpaceTemp->Gate);
      fprintf(stderr, "Activ    : %d\n", CASpaceTemp->Activ);
      for(i=0;i<6;i++)
        fprintf(stderr, "IOBuffer[%d] : %d\n",i ,
          CASpaceTemp->IOBuf[i]);
      fprintf(stderr, "\n");
#endif
  return 0;
}

extern char StepCA(int ANumberSteps, char Phase)
/* returns end of GROWTHPHASE (1) */
{
  switch(Phase) {
    case GROWTHPHASE: 
	if (ANumberSteps != 1) /* to the non-step-by-step mode */
	  g_counter = 0; 
	return GrowthStep(ANumberSteps); 
	break;
    case SIGNALPHASE: return SignalStep(ANumberSteps); break;
    default : fprintf(stderr, "Bad Phase!\n");
  }
  return 0;
}
