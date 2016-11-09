/* CA on X for cc */

#include <stdio.h>
#include <string.h>
#include <time.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/keysym.h>
#include <X11/keysymdef.h>
#include "ca.h"
#include "CA-Def-Type.c"

/*** defines ***/
#define CM_NO_EVENT               0
#define CM_QUIT_APP               1
#define CM_STOP_STEP_SIM          2
#define CM_CONT_SIM               3
#define CM_INIT_NEW               4
#define CM_CHANGE_TO_GROWTHPHASE  5
#define CM_CHANGE_TO_SIGNALPHASE  6
#define CM_GA_EVOLVE              7


/************ Types ******************/

struct TPoint3D {
         unsigned long           x, y, z;
         struct TPoint3D        *Next;  
       };

/* external Globals from SimWindow.c */

extern Display            *theDisplay; 
extern Window              theWindow;
extern unsigned int        theWidth;
extern unsigned int        theHeight;
extern unsigned long       SpaceSizeX;
extern unsigned long       SpaceSizeY;
extern unsigned long       SpaceSizeZ;
extern unsigned long       NumberCells;
extern unsigned long       PlainSizeXY;

/* external Globals from codi.c */
/* Introduced by Eiji, Sep.1/97 */
extern int g_counter;

/********** local variables **********/

static TCACell            *CASpace;
static TCACell            *TempCASpace;
static int                 i, ix, iy;
static struct TPoint3D    *MarkerList;
static int                 MarkerListLength = 0;

static char                Message;
static char                SimulationStatus = CM_STOP_STEP_SIM;
static char                SimulationPhase = GROWTHPHASE;
static int                 StepEventCount = 0;
static int                 CAStepCount = 0;

/********** functions ****************/

static char EventLoop(void)
/* 0 for go on */
{
  XEvent               theEvent;
  int                  theKeyBufferMaxLen = 64;
  /* in case one key produces a string a characters */
  char                 theKey[65] = "\0";
  KeySym               theKeySym;
  int                  KeyLength;
  /* get the next event if any */
  while (CheckMaskEvent(&theEvent))
    switch(theEvent.type) {
      case Expose: /* an overlapped part gets visible */
        /* fprintf(stderr, "Window is exposed\n"); */
        if (theEvent.xexpose.count == 0) /* a sequence is over */
  	  RefreshWindow(theEvent.xany.window); /* redraw the window */
        break;
      case ConfigureNotify: 
        /* fprintf(stderr, "Window is reconfigured\n"); */
          ActualizeWindowInfo();
  	  RefreshWindow(theEvent.xany.window); /* redraw the window */
        break;
      case MapNotify: 
        /* fprintf(stderr, "Window is mapped\n"); */
  	  RefreshWindow(theEvent.xany.window); /* redraw the window */
        break;
      case KeyPress:
        KeyLength = XLookupString(&theEvent.xkey, theKey, 
          theKeyBufferMaxLen, &theKeySym, NULL);
        /* fprintf(stderr, "Key %s pressed!\n", theKey); */

        if ((theKeySym >= ' ') && (theKeySym <= '~') &&
            (KeyLength > 0))  {/* 26<=key<=126 */
          if (!(theEvent.xkey.state & ControlMask)) 
            /* not with CONTROL KEY */
            switch(theKey[0]){
              case 'q': return(CM_QUIT_APP); /* quit */    
              case 'Q': return(CM_QUIT_APP); /* quit */    
              case 'i': return(CM_INIT_NEW);
              case 'I': return(CM_INIT_NEW);
              case ' ': return(CM_STOP_STEP_SIM); 
              case 'g': return(CM_CHANGE_TO_GROWTHPHASE);
              case 'G': return(CM_CHANGE_TO_GROWTHPHASE);
              case 's': return(CM_CHANGE_TO_SIGNALPHASE);
              case 'S': return(CM_CHANGE_TO_SIGNALPHASE);
              case 'e': return(CM_GA_EVOLVE);
              case 'E': return(CM_GA_EVOLVE);
            }
          else {
            /* fprintf(stderr, "Key + CONTROL pressed!\n"); */
            switch(theKeySym){
              case XK_c:             
                /* fprintf(stderr, "CONTROL-c Key pressed!\n"); */
                return(CM_QUIT_APP); /* quit */    
              case XK_C:                 
                /* fprintf(stderr, "CONTROL-C Key pressed!\n"); */
                return(CM_QUIT_APP); /* quit */    
            }
          }
        } else {
          switch(theKeySym) { /* special keys */
            case XK_Escape : 
              /* fprintf(stderr, "ESCAPE\n");  */
              return(CM_QUIT_APP);
            case XK_Return : 
              /* fprintf(stderr, "RETURN\n"); */
               return(CM_CONT_SIM);
          }
        }
        break;
      case ButtonPress: /* mouse button */
        /* fprintf(stderr, "Mouse button pressed!\n"); */
        ReactToMouse(theEvent);
        break;
      default: /* nothing happend -> event out and dead */
    }
  return(CM_NO_EVENT);
}

extern char RefreshWindowDebug(char ClearWindow)
{
  if (ClearWindow) XFill("black");
  RefreshWindow(theWindow);
}

static char RefreshWindow(Window AWindow)
{
  /* the origen is in the upper left corner, 
   * +x goes to the right and +y down */

/* if (AWindow != theWindow) return; */
/*     SetColor("black"); */
/*     XLine(0,0,30,60); */
/*     XRect(10,20,30,40); */
/*     SetColor("red"); */
/*     XFillRect(50,50,10,20); */
/*     SetColor("blue"); */
/*     XFillRect(50,10,10,5); */
/*     SetColor("green"); */
/*     XText(30,60, "Text"); */
/*     SetColor("yellow"); */
/*     XFillOval(60,20,10,10); */

  TempCASpace = CASpace;
  
  /* XFill("black"); */
  for(iy=SpaceSizeY-1;iy>=0;iy--)
    for(ix=0;ix<SpaceSizeX;ix++) {
      if (TempCASpace->Type) {
        if (TempCASpace->Activ) {
          if (TempCASpace->Type != NEURON)
            SetColor("yellow"); /* signal */
          else SetColor("white");
	}
        else
          switch (TempCASpace->Type) {
            case  NEURON: 
              SetColor("white");
              break;
            case  AXON: 
              SetColor("red");
              break;
            case  DEND: 
              SetColor("green");
              break;
            default :
              fprintf(stderr, "__%d__",TempCASpace->Type);
              SetColor("blue");
              break;
           }
         XFillRect(ix*10, iy*10, 10 ,10);
      }
      TempCASpace++;
    }
  XFlush(theDisplay);
  return 0;
} 

static unsigned long DisplayXYZtoCACell(int x, int y, int z)
{
   return(x/CELLWIDTH+
          (theHeight-y)/CELLHEIGHT*SpaceSizeX+
          z*PlainSizeXY);
}

static void ReactToMouse(XEvent theEvent)
{
  struct TPoint3D   *Point3DTemp1;
  struct TPoint3D   *Point3DTemp2;
  char               MarkerString[10];
  unsigned long      NrCell;
  /* mouse with SHIFT to get cell info */
  if (theEvent.xbutton.state == ShiftMask) {
    if (theEvent.xbutton.button == Button1) {
      /* fprintf(stderr, " 1. mouse button + shift"); */
      Point3DTemp1 = (struct TPoint3D*) 
                     calloc(1, sizeof(struct TPoint3D));
      if (!Point3DTemp1) return;
      Point3DTemp1->x = theEvent.xbutton.x;
      Point3DTemp1->y = theEvent.xbutton.y;
      Point3DTemp1->z = 0;
      NrCell = DisplayXYZtoCACell(Point3DTemp1->x, 
                 Point3DTemp1->y, Point3DTemp1->z);
      PrintCellInfo(NrCell);
    }
    return;
  }
  /* mouse without SHIFT is for marking */
  /* set a maker */
  if (theEvent.xbutton.button == Button1) {
    /* fprintf(stderr, " 1. mouse button "); */
    Point3DTemp1 = (struct TPoint3D*) 
                   calloc(1, sizeof(struct TPoint3D));
    if (!Point3DTemp1) return;
    Point3DTemp1->x = theEvent.xbutton.x;
    Point3DTemp1->y = theEvent.xbutton.y;
    Point3DTemp1->z = 0;
    Point3DTemp1->Next = NULL;
    if (!MarkerListLength) 
      MarkerList = Point3DTemp1;
    else {
      Point3DTemp2 = MarkerList;
      for (i=0;i<MarkerListLength-1;i++)
        Point3DTemp2 = Point3DTemp2->Next;
      Point3DTemp2->Next = Point3DTemp1;
    }
    MarkerListLength++;
  }
  /* display all markers */
  if ((theEvent.xbutton.button == Button3)||
      (theEvent.xbutton.button == Button1)) {
    SetColor("white");
    LoadFont("default");
    Point3DTemp2 = MarkerList;
    for (i=0;i<MarkerListLength;i++) {
      sprintf(MarkerString,"%d",i);
      XCenteredText(Point3DTemp2->x, Point3DTemp2->y,MarkerString);
      /* XFillOval(Point3DTemp2->x, Point3DTemp2->y, 5,5); */
      Point3DTemp2 = Point3DTemp2->Next;
    }
    XFlush(theDisplay);
    return;
  }
  /* delete all markers */
  if (theEvent.xbutton.button == Button2) {
    FreeMarkers();
    XFill("black");
    RefreshWindow(theEvent.xany.window);
    return;
  }
}

static void FreeMarkers(void)
{
  struct TPoint3D   *Point3DTemp;
  Point3DTemp = MarkerList;
  for (i=0;i<MarkerListLength;i++) {
    MarkerList = MarkerList->Next;
    free(Point3DTemp);
    Point3DTemp = MarkerList;
  }
  MarkerListLength = 0;
}

static char ScanCommandLine(int argc, char **argv)
{
  /* fprintf(stderr, "argc: %d\n", argc); */
  if (argc != 7) {
    fprintf(stderr, 
     "Use  -x -y -z integer Parameter to specify the Space Size:\n");
    fprintf(stderr, "The default is -x %d -y %d -z %d\n\n",
      SpaceSizeX, SpaceSizeY, SpaceSizeZ);
  }
  if (argc>1) 
    for (i=1; i<argc; i++) {
      /* fprintf(stderr, " argv[%d] : %s\n", i, argv[i]); */
      /* fprintf(stderr, " %s -> %d...\n",argv[i], atol(argv[i])); */
      if (!strcmp(argv[i], "-x"))
        if ((i+1) < argc) SpaceSizeX = atol(argv[i+1]);
      if (!strcmp(argv[i], "-y"))
        if ((i+1) < argc) SpaceSizeY = atol(argv[i+1]);
      if (!strcmp(argv[i], "-z"))
        if ((i+1) < argc) SpaceSizeZ = atol(argv[i+1]);
    }
  if (SpaceSizeX <= 0) SpaceSizeX = SPACESIZEX;
  if (SpaceSizeY <= 0) SpaceSizeY = SPACESIZEY;
  if (SpaceSizeZ <= 0) SpaceSizeZ = SPACESIZEZ;
  fprintf(stderr, 
   "The Space Size is set to: -x %d -y %d -z %d\n\n",
    SpaceSizeX, SpaceSizeY, SpaceSizeZ);
}

static void PrintProgammInfo(void)
{
fprintf(stderr, "Cellular Automata Simulator\n\n");
fprintf(stderr, "User Keys:\n");
fprintf(stderr, "C-c           : Quit\n");
fprintf(stderr, "ESCAPE        : Quit\n");
fprintf(stderr, "q,Q           : Quit\n");
fprintf(stderr, "\n");

fprintf(stderr, "SPACE         : Step Simulation\n");
fprintf(stderr, "RETURN        : Run Continuously\n");
fprintf(stderr, "i,I           : Init Space\n");
fprintf(stderr, "s,S           : Signal-Phase\n");
fprintf(stderr, "g,G           : Growth-Phase\n");
fprintf(stderr, "e,E           : Start GA (evolve)\n");
fprintf(stderr, "\n");

fprintf(stderr, "SHIFT + Left Button   : Get Cell Info\n");
fprintf(stderr, "SHIFT + Middle Button : \n");
fprintf(stderr, "SHIFT + Right Button  : \n");
fprintf(stderr, "\n");

fprintf(stderr, "Left Button   : Set Marker\n");
fprintf(stderr, "Middle Button : Delete all Marker\n");
fprintf(stderr, "Right Button  : Display Marker\n");
fprintf(stderr, "\n");
}

static void InitRandSeed(void) 
{
    long time_now;
    time(&time_now);
    srand((unsigned int)time_now);
}

static void ResetCA_after_Event(void)
{
  /* Added by Eiji, Sep.1/97 */
  g_counter = 0;

  CAStepCount = 0;
  ResetCA_IO();
  XFill("black");
  RefreshWindow(theWindow);
}

static void Run()
{
  PrintProgammInfo();
  /* Added by Eiji, Sep.23/97 */
  InitRandSeed();
  if (InitCA(&CASpace)) return;
  /*
  if (InitX(0, 0, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;
  if (InitX(140, 0, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;
  if (InitX(280, 0, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;
  if (InitX(420, 0, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;
  if (InitX(560, 0, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;
  if (InitX(700, 0, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;
  if (InitX(840, 0, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;
  if (InitX(980, 0, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;

  if (InitX(0, 170, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;
  if (InitX(140, 170, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;
  if (InitX(280, 170, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;
  if (InitX(420, 170, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;
  if (InitX(560, 170, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;
  if (InitX(700, 170, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;
  if (InitX(840, 170, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;
   */
  if (InitX(980, 170, SpaceSizeX*CELLWIDTH, SpaceSizeY*CELLHEIGHT)) 
   return;
  
  GetXWindowInfo();
  while(SimulationStatus != CM_QUIT_APP) {
    while(!(Message = EventLoop())) {
      if (SimulationStatus == CM_GA_EVOLVE) {
        if (SimulationPhase == SIGNALPHASE) CAStepCount++;
	if (CAStepCount >= MAX_SIGNALSTEPS) {
          RunGA(CHROMOFILENAME, NR_GENERATIONS);
          SimulationPhase = GROWTHPHASE;
          ResetCA_after_Event();
        }     
      }

      /* Run Simulation */
      if ((SimulationStatus == CM_STOP_STEP_SIM) && (StepEventCount)
  	|| (SimulationStatus == CM_CONT_SIM) 
        || (SimulationStatus == CM_GA_EVOLVE)) {
        if (StepEventCount) StepEventCount--;
        /* do one Step */
        if (StepCA(1,SimulationPhase)) {
          /* change Phase */
          SimulationPhase = (SimulationPhase+1) % NUMBERPHASES; 
          ResetCA_after_Event();
        }
        RefreshWindow(theWindow);
      }
    }

    switch(Message) {
      case CM_STOP_STEP_SIM: 
        if (SimulationStatus == CM_STOP_STEP_SIM) StepEventCount++;
        else SimulationStatus = CM_STOP_STEP_SIM;
        break;
      case CM_CONT_SIM:
        SimulationStatus = CM_CONT_SIM;
        break;
      case CM_INIT_NEW:
        XMessageText("Init New");
        SimulationPhase = GROWTHPHASE;
        InitCASpace();
        ResetCA_after_Event();
        break;
      case CM_CHANGE_TO_GROWTHPHASE:
        XMessageText("Growth-Phase");
        SimulationPhase = GROWTHPHASE;
        ResetCA_after_Event();
        break;
      case CM_CHANGE_TO_SIGNALPHASE:
        XMessageText("Signal-Phase");
        SimulationPhase = SIGNALPHASE;
        ResetCA_after_Event();
        break;
      case CM_GA_EVOLVE:
	XMessageText("Evolve");
        SimulationStatus = CM_GA_EVOLVE;
        ResetCA_after_Event();
        CAStepCount = MAX_SIGNALSTEPS; /* start immediately */
        break;
      case CM_QUIT_APP:      
        XMessageText("QUIT");
        SimulationStatus = CM_QUIT_APP;
        break;
    }
  }
  FreeMarkers();
  QuitX();
  FreeCASpace();
  FreeChromoMemo();
}

main(int argc, char **argv)
{
  ScanCommandLine(argc, argv);
  Run();
}

 
