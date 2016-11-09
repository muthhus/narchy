/* This file contains the functions for CA- GA evolution
 * It is used by codi.c
*/

#include <stdio.h>
#include <math.h>
#include "GA.h"
#include "CA-Def-Type.c"

/********* defines  **********************/

#define STANDARDGENRATION  10   /* Number of chromosomes in the population */ 
#define MAX_GENERATIONS    -1   /* -1 :  no limit */
#define FIT_ENOUGH         1.0
#define CROSS_FRACTION     0.3    /* fraction of individuals crossed */
#define MUTATION_RATE      0.05
#define NEURON_MUT_RATE    0.07

/******* external Globals from codi.c */

extern unsigned long               SpaceSizeX;
extern unsigned long               SpaceSizeY;
extern unsigned long               SpaceSizeZ;
extern unsigned long               NumberCells;
extern unsigned long               PlainSizeXY;
extern TCACell                    *CASpace;
extern TCACell                    *CASpaceTemp;

/********* Variable Declarations **********/

unsigned char      **ChromoList;
unsigned char       *BestChromoSoFar;
unsigned long        ChromoLength;
unsigned int         ChromoListLength = 0;
float               *Fitness;
float                BestFitness;
float                BestFitnessVec[NR_GENERATIONS];  /* to store the best fitnesses */
float                AverFitnessVec[NR_GENERATIONS];  /* to store the average fitnesses */
float                BestFitnessSoFar = 0;
int                  NrBestChromo;

/********** local variables ***************/

static int                  ix, iy, iz, it ;
static unsigned long        GenerationTotal = 0;
static int                  xt[] = {4,8,12};
static int                  yt[] = {5,10};

/********* functions **********************/


static char AllocChromoMemo(void)
{
  int                  i;
  char                 status = 0;
  /* in case some one forgot: */
  FreeChromoMemo();
  /* memory for the list */
  ChromoList = (unsigned char**) 
               calloc(ChromoListLength, sizeof(char*));
  if (!ChromoList) status = 1;
  if (status) {
    fprintf(stderr, "Couldn't alloc memory!\n");
    return status;
  }
  /* memory for the chromos */
  for(i=0;i<ChromoListLength;i++) 
    if(!(*(ChromoList+i) = (unsigned char*) 
      calloc(ChromoLength, sizeof(char))))
      status = 1;
  /* memory for the fitness */
  if(!(Fitness = (float*) calloc(ChromoListLength, sizeof(float))))
    status = 1;
  /* Added by Eiji, Sep.8/97 */
  if (!(BestChromoSoFar = (unsigned char*) calloc(ChromoLength, sizeof(char))))
    status = 1;
   
  if (status) fprintf(stderr, "Couldn't alloc memory!\n");
  return status;
}

extern void FreeChromoMemo(void)
{
 int                  i;
 /* free memory for the chromos */
  for(i=0;i<ChromoListLength;i++) 
    if (ChromoList) free(*(ChromoList+i));
  /* free memory for the list */
  if (ChromoList) free(ChromoList);
  /* free memory for the fitness */
  if (Fitness) free(Fitness);
  /* free memory for the best chromosome so far */ 
  if (BestChromoSoFar) free(BestChromoSoFar);
}

extern void RandomChromos(void)
{
  int                  i, ii, kk;
  int                  iaux, flag;  /* Introduced by Eiji, Sep.2/97 */

  for(i=0;i<ChromoListLength;i++) {
    for(ii=0;ii<ChromoLength;ii++)  {

 /*     *(*(ChromoList+i)+ii) = (unsigned char) random()&255;  */


     *(*(ChromoList+i)+ii) = 0;
     if (ii%2 == 0) {  /* for the byte Chromo */
       for(kk=0; kk<6; kk++) {
         if (0.194 > (fmod(random(),RAND_MAX)/RAND_MAX)) /* 1/6 + 1/36 */
           *(*(ChromoList+i)+ii) |= (1 << kk);
       }
     }
     else {  /* for the byte Sign */
       for(kk=0; kk<6; kk++) {
         if (0.5 > (fmod(random(),RAND_MAX)/RAND_MAX)) 
           *(*(ChromoList+i)+ii) |= (1 << kk);
       } 
     }
    }
  }

  /* Modified by Eiji, Sep.2/97 */
  for(i=0;i<ChromoListLength;i++) {
    ii = 0;  
    for(iz=0; iz<SpaceSizeZ; iz++)
      for(iy=0; iy<SpaceSizeY; iy++)
        for(ix=0; ix<SpaceSizeX; ix++) {	  
	  *(*(ChromoList+i)+ii) &= 63;
	  /* Neurons have to be positioned in a 2x2x4 grid in the space */
	  if (NEURON_DENSITY > (fmod(random(),RAND_MAX)/RAND_MAX)) {
	    if ((ix%X_WINDOW == 0) && (iy%Y_WINDOW == 0) && (iz%Z_WINDOW == 0))
	      *(*(ChromoList+i)+ii) |= (NEURONSEED << 6); /* bit 6,7 */
	  }

	  /*
	  if ((ix == 0) && (iy == 4)) {
	    if ((iz == 2) || (iz == 4) || (iz == 6) || (iz == 10) || (iz == 12) || (iz == 14))
	      CASpaceTemp->Chromo |= (NEURONSEED << 6); 
	  }
	  */
	  if ((iz == 0) && (iy == 4)) {
	    if ((ix == 4) || (ix == 8) || (ix == 12))
	      *(*(ChromoList+i)+ii) |= (NEURONSEED << 6); 
	  }
	  if ((iz == 0) && (iy == 8)) {
	    if ((ix == 4) || (ix == 8) || (ix == 12))
	      *(*(ChromoList+i)+ii) |= (NEURONSEED << 6); 
	  }

	  ii=ii+2;  /* Advances 2, skips the sign byte */
	}

/*
  for(i=0;i<ChromoListLength;i++)
    for(ii=0;ii<ChromoLength;ii++) {
      *(*(ChromoList+i)+ii) &= 63;

     if (NEURON_DENSITY > (fmod(random(),RAND_MAX)/RAND_MAX))  
      *(*(ChromoList+i)+ii) |= (NEURONSEED << 6); 

     if((ii == (4+5*SPACESIZEX))||(ii ==(8+5*SPACESIZEX))||(ii ==
							    (12+5*SPACESIZEX))||(ii == (4+10*SPACESIZEX))||(ii == (8+10*SPACESIZEX))||(ii == (12+10*SPACESIZEX))||(ii == ((SPACESIZEX*SPACESIZEY*(SPACESIZEZ-1)+(8*SPACESIZEX)+8))))

      *(*(ChromoList+i)+ii) |= (NEURONSEED << 6); 
*/
  }
}

extern char LoadChromo(char *AFileName)
{
  int                  i, ii;
  FILE                *f;
  int                  scanint;
  f = fopen(AFileName, "r");
  if (!f) {
    fprintf(stderr, "couldn't open file '%s'\n", AFileName);
    fclose(f);
    return(1);
  }
  /* Fileformat: NumberChromo ChromoLength Chromo1 ... */
  if (fscanf(f,"%u", &ChromoListLength) != 1) return 1;
  if (fscanf(f,"%u", &ChromoLength) != 1) return 1;
  if (AllocChromoMemo()) return 1;
  for(i=0;i<ChromoListLength;i++)
    for(ii=0;ii<ChromoLength;ii++) {
      if (fscanf(f,"%u", &scanint) == EOF) return 1;
      *(*(ChromoList+i)+ii) = scanint;
    }
  fprintf(stderr, "Chromo read file '%s'\n", AFileName);
  fclose(f);
  return 0;
}

extern char SaveChromo(char *AFileName)
{
  char                *fname;
  int                  i, ii;
  FILE                *f;
  f = fopen(AFileName, "w");
  if (!f) {
    fprintf(stderr, "couldn't open file '%s'\n", AFileName);
    fclose(f);
    return(1);
  }
  /* Fileformat: NumberChromo ChromoLength Chromo1 ... */
  fprintf(f, "%u\n", ChromoListLength);
  fprintf(f, "%u\n", ChromoLength);

  for(i=0;i<ChromoListLength;i++) {
    fprintf(f, "\n\n");
      for(ii=0;ii<ChromoLength;ii++)
        if (fprintf(f, "%u ", 
          *(*(ChromoList+i)+ii)) == EOF) return 1;
  }
  fprintf(stderr, "Chromo saved in file '%s'\n", AFileName);
  fclose(f);
  /* Added by Eiji, Sep.9/97 */
  fname = (char *) malloc(strlen(AFileName)+5*sizeof(char));
  strcpy(fname, AFileName);
  strcat(fname,".dat\0");
  f = fopen(fname, "a");
  if (!f) {
    fprintf(stderr, "couldn't open file '%s'\n", fname);
    fclose(f);
    return(1);
  }

  for(i=0;i<NR_GENERATIONS;i++) 
    fprintf(f, "%f %f\n", BestFitnessVec[i],AverFitnessVec[i]);
  
  fprintf(stderr, "Data saved in file '%s'\n", fname);
  fclose(f);

  return 0;
}

extern char PrintChromo(void)
{
  int                  i, ii;
  /* Fileformat: NumberChromo ChromoLength Chromo1 ... */
  fprintf(stderr, "%u\n", ChromoListLength);
  fprintf(stderr, "%u\n", ChromoLength);

  for(i=0;i<ChromoListLength;i++) {
    fprintf(stderr, "\n");
      for(ii=0;ii<ChromoLength;ii++)
        if (fprintf(stderr, "%u ",
          *(*(ChromoList+i)+ii)) != 1) return 1;
  }
  return 0;
}

static void InitCASpaceWithChromo(char *AChromo)
{
  int                  i;
  CASpaceTemp = CASpace;
  /* Modification by Eiji, 08/27/97 */
  for(i=0;i<ChromoLength;i+=2){
    CASpaceTemp->Chromo = *(AChromo+i);
    CASpaceTemp->Type   = BLANK;
    CASpaceTemp->Gate   = 0;
    CASpaceTemp->Activ  = 0;
    CASpaceTemp->Sign   = *(AChromo+i+1);
    CASpaceTemp++;
  }
  /*
  for(i=0;(i<NumberCells)&&(i<ChromoLength);i++){
    CASpaceTemp->Chromo = *(AChromo+i);
    CASpaceTemp->Type   = BLANK;
    CASpaceTemp->Gate   = 0;
    CASpaceTemp->Activ  = 0;
    CASpaceTemp++;
  }
  */
}

static void StartWithRandomChromo(void)
{
  ChromoListLength = STANDARDGENRATION;
  /* Modified by Eiji, 08/26/97 */
  ChromoLength     = 2*NumberCells;  /* to store the new char Sign */
  /*ChromoLength     = NumberCells;*/
  AllocChromoMemo();
  RandomChromos();
}

extern void PrintGAInfo()
{
  fprintf(stderr, "ChromoListLength     : %u\n", ChromoListLength);
  fprintf(stderr, "ChromoLength         : %u\n", ChromoLength);
}

extern void RunGA(char* FileName, unsigned long NumberGenerations)
{
  int                  i, ii;
  unsigned long GenerationCount = 0;
  unsigned int SubstitutedChrom ;
  float FitnessAverage;

  if (!NumberGenerations) NumberGenerations = MAX_GENERATIONS;

  if (!ChromoList) {
   if (!FileName) 
      StartWithRandomChromo();
    else
      if (LoadChromo(FileName)) StartWithRandomChromo();

/*   StartWithRandomChromo();  */
  }
  PrintGAInfo();
  while ((GenerationCount != NumberGenerations) && (BestFitness < FIT_ENOUGH)) {
    BestFitness = 0;
    GenerationCount++;
    GenerationTotal++;
    FitnessAverage = 0;
    for(i=0;i<ChromoListLength;i++) {
      Fitness[i] = 0;
      InitCASpaceWithChromo(*(ChromoList+i));
      RunApp(&Fitness[i]);
      fprintf(stderr, "  Chromo Nr : %d   Fitness : %f\n", i, Fitness[i]);
      FitnessAverage += Fitness[i];
      if (Fitness[i] > BestFitness) {
        BestFitness = Fitness[i];
	NrBestChromo = i;
      }

    }
    /* Stores the best and average fitnesses */
    if (NumberGenerations <= NR_GENERATIONS) {
      BestFitnessVec[GenerationCount-1] = BestFitness;
      AverFitnessVec[GenerationCount-1] = FitnessAverage/STANDARDGENRATION;
    }
    /* Stores the best so far */
    /*
    if (BestFitness > BestFitnessSoFar) { 
      BestFitnessSoFar = BestFitness;
      for(i=0; i<ChromoLength; i++)
        BestChromoSoFar[i] = *(*(ChromoList+NrBestChromo)+i);
    }*/

    fprintf(stderr, "Generation    : %u\n", GenerationTotal);
    fprintf(stderr, "BestFitness   : %f\n", BestFitness);
    fprintf(stderr, "NrBestChromo  : %d\n", NrBestChromo);
    fprintf(stderr, "FitnessAverage: %f\n", FitnessAverage/STANDARDGENRATION);
    fprintf(stderr, "\n");
    Select();
    Cross();
    FlippingBits();
    
    /*for(i=0; i<ChromoLength; i++)
      *(*(ChromoList+ChromoListLength-1)+i) =  BestChromoSoFar[i];*/
  }
  InitCASpaceWithChromo(*(ChromoList+NrBestChromo));
  if (FileName) SaveChromo(FileName);
  else SaveChromo(CHROMOFILENAME);
}

static void Select(void)
{
  int                  i, ii, j, k;
  float                TempFitness;
  unsigned char        *TempChromo, TempSelection[ChromoLength];
  unsigned char        bubbeledup = 0;
  /* bubble sort the fitness */
  while (!bubbeledup) {
    bubbeledup = 1;
    for(i=1;i<ChromoListLength;i++) 
      if (Fitness[i-1]>Fitness[i]) {
        TempFitness  = Fitness[i];
        Fitness[i]   = Fitness[i-1];
        Fitness[i-1] = TempFitness;
        TempChromo   = ChromoList[i];
        ChromoList[i] = ChromoList[i-1];
        ChromoList[i-1] = TempChromo;
        bubbeledup = 0;
      }
  }

  /* let the better half survive */
  /*
  for(i=0;i<ChromoListLength/2;i++)
    for(ii=0;ii<ChromoLength;ii++)
      *(*(ChromoList+i)+ii) = *(*(ChromoList+ChromoListLength-i-1)+ii);
  */
  /*
  for(i=0;i<ChromoListLength/2;i++) {
    for(ii=0;ii<ChromoLength;ii++)
      TempSelection[ii]	= (ChromoList[ChromoListLength-i-1])[ii];
    for(ii=0;ii<ChromoLength;ii++)
      (ChromoList[i])[ii] = TempSelection[ii];
  }
  */
  i = 0;
  for(ii=0;ii<ChromoLength;ii++)
    TempSelection[ii] = (ChromoList[ChromoListLength-i-1])[ii];
  for(ii=0;ii<ChromoLength;ii++)
    (ChromoList[i])[ii] = TempSelection[ii];
}

static void Cross(void)
{
  int                  i, ii;
  unsigned char        TempChromo;
  unsigned long        CuttingPoint;
  unsigned int         j,k;
  /* do Cross over with some of them */
  for(i=0;i<(unsigned int)ChromoListLength*CROSS_FRACTION;i++) {
    /* The best is left unchanged */
    j = (unsigned int) fmod(random(),ChromoListLength-1);
    k = (unsigned int) fmod(random(),ChromoListLength-1);
    CuttingPoint = (unsigned long) fmod(random(),ChromoLength);
    for(ii=0;ii<CuttingPoint;ii++) {
      TempChromo          =  (ChromoList[j])[ii];
      (ChromoList[j])[ii] =  (ChromoList[k])[ii];
      (ChromoList[k])[ii] =  TempChromo;
    }
  }
}

static void Mutate(void)
{
  int                  i, ii, it;
  /* keep the best one unchanged */
  for(i=0;i<ChromoListLength-1;i++) {
    /* Modified by Eiji, Sep.7/97 */
    ii = 0;
    for (iz=0; iz < SpaceSizeZ; iz++)
      for (iy=0; iy < SpaceSizeY; iy++)
	for (ix=0; ix < SpaceSizeX; ix++) {
	  if (ii < ChromoLength) {
	    if (ii%2 == 0) { /* byte Chromo */
	      if ((MUTATION_RATE > (fmod(random(),RAND_MAX)/RAND_MAX)) || (BestFitness == 0)) {
	        *(*(ChromoList+i)+ii) &= (unsigned char) 192;		
	        *(*(ChromoList+i)+ii) |= (unsigned char) (random()&63); 
	        if ((ix%X_WINDOW == 0) && (iy%Y_WINDOW == 0) && (iz%Z_WINDOW == 0)) {
	          if (NEURON_DENSITY > (fmod(random(),RAND_MAX)/RAND_MAX)) {
		    *(*(ChromoList+i)+ii) |= (NEURONSEED << 6);
		  }
		  else {
		    *(*(ChromoList+i)+ii) &= (unsigned char) 63;
		  }
	        }
	      }
	    }
	    ii++;
	    if (ii%2 != 0) { /* byte Sign */  
	      if ((MUTATION_RATE > (fmod(random(),RAND_MAX)/RAND_MAX)) || (BestFitness == 0)) {
	        *(*(ChromoList+i)+ii) = (unsigned char) (random()&63); 
	      }
	    }
	    /* Keep the input neurons */
	    /*
            if ((ix == 0) && (iy == 4)) {
	      if ((iz == 2) || (iz == 4) || (iz == 6) || (iz == 10) || (iz == 12) || (iz == 14))
	        *(*(ChromoList+i)+ii) |= (NEURONSEED << 6);
	    }
	    */
	    if ((iz == 0) && (iy == 4)) {
	      if ((ix == 4) || (ix == 8) || (ix == 12))
		*(*(ChromoList+i)+ii) |= (NEURONSEED << 6);
	    }
	    if ((iz == 0) && (iy == 8)) {
	      if ((ix == 4) || (ix == 8) || (ix == 12))
		*(*(ChromoList+i)+ii) |= (NEURONSEED << 6);
	    }
	    ii++;
	  }
	}
  }
  /* Commented by Eiji, Sep. 7/97  */
	/*
	if((ii == (4+5*SPACESIZEX))||(ii ==(8+5*SPACESIZEX))||(ii == (12+5*SPACESIZEX))||(ii ==
	(4+10*SPACESIZEX))||(ii == (8+10*SPACESIZEX))||(ii == (12+10*SPACESIZEX))||(ii ==
	((SPACESIZEX*SPACESIZEY*(SPACESIZEZ-1)+(8*SPACESIZEX)+8))))  

	  *(*(ChromoList+i)+ii) |= (NEURONSEED << 6);  bit 6,7 */  



  /* keep input neurons 
     for(it=0; it<3; it++) {
     CASpaceTemp = CASpace;
     CASpaceTemp += xt[it];
     if (CASpaceTemp->Type == NEURON)
     CASpaceTemp->Activ++;
     }
     for(it=0; it<3; it++) {
     CASpaceTemp = CASpace;
     CASpaceTemp += (yt[it]*SPACESIZEX);
     CASpaceTemp->Activ++;
     }  
   */
/* Commented by Eiji, Sep.9/97 */
/*CASpaceTemp = CASpace;*/

}

/* Modified version for the mutation operator, Eiji Sep.16/97 */
static void FlippingBits(void)
{
  int                  i, ii;
  /* keep the best one unchanged */
  for(i=0;i<ChromoListLength-1;i++) {
    for (iz=0; iz < SpaceSizeZ; iz++)
      for (iy=0; iy < SpaceSizeY; iy++)
	for (ix=0; ix < SpaceSizeX; ix++) {
	  ii = 2*(iz*PlainSizeXY + iy*SpaceSizeX + ix);
	  if (ii < ChromoLength-1) {  /* Paranoid check...*/
	    /* byte Chromo */
	    if ((MUTATION_RATE > (fmod(random(),RAND_MAX)/RAND_MAX)) || (BestFitness == 0)) {
	      *(*(ChromoList+i)+ii) &= (unsigned char) 192;		
	      *(*(ChromoList+i)+ii) |= (unsigned char) (random()&63); 
	      if ((ix%X_WINDOW == 0) && (iy%Y_WINDOW == 0) && (iz%Z_WINDOW == 0)) {
	        if (NEURON_DENSITY > (fmod(random(),RAND_MAX)/RAND_MAX)) {
	          *(*(ChromoList+i)+ii) |= (NEURONSEED << 6);
	        }
	        else {
	          *(*(ChromoList+i)+ii) &= (unsigned char) 63;
	        }
	      }
	    }
	    /* Keep input neurons */
	    if ((iz == 0) && (iy == 4)) {
	      if ((ix == 4) || (ix == 8) || (ix == 12))
	        *(*(ChromoList+i)+ii) |= (NEURONSEED << 6);
	    }
	    if ((iz == 0) && (iy == 8)) {
	      if ((ix == 4) || (ix == 8) || (ix == 12))
	        *(*(ChromoList+i)+ii) |= (NEURONSEED << 6);
	    }
	    /* byte Sign */  
	    ii++;
	    if ((MUTATION_RATE > (fmod(random(),RAND_MAX)/RAND_MAX)) || (BestFitness == 0)) {
	      *(*(ChromoList+i)+ii) = (unsigned char) (random()&63); 
	    }
	    /* Keep the input neurons */
	    /*
            if ((ix == 0) && (iy == 4)) {
	      if ((iz == 2) || (iz == 4) || (iz == 6) || (iz == 10) || (iz == 12) || (iz == 14))
	        *(*(ChromoList+i)+ii) |= (NEURONSEED << 6);
	    }
	    */
	  }
	}
  }
}
