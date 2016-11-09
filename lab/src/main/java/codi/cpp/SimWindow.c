/* Xwindow for a CA simulation on X for cc */

#include <stdio.h>
#include <string.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/cursorfont.h>
#include "CAIcon"       /* Xbitmap created with iconedit */ 
#include "colors.c"
#include "SimWindow.h"

/*** defines ***/

#define DEFAULT_FONT  "8x13"
#define LARGE_FONT    "12x24" /* get list with xlsfonts -l */

/* define it here for all the childs */

#define EVENT_MASK (ButtonPressMask | KeyPressMask | \
		    ExposureMask | StructureNotifyMask)  


/*** Global Stuff for X ***/

Display               *theDisplay;
char                  *theDisplayName = NULL; /* the sever display */
int                    theScreen;
int                    theDepth; /* monochrome = 1 */
unsigned long          theWhitePixel;
unsigned long          theBlackPixel;
Window                 theWindow;
Cursor                 theCursor;
char                  *theProgramName = "X CA"; 
int                    X = 100 , Y = 100;
unsigned int           theWidth = 100, theHeight = 100;
unsigned int           theBorderWidth = 3;
/* false for for keyboard focus : */
char                   OverrideRedirectFlag = False; 
GC                     theGC;
Colormap               theColormap;
unsigned long          theColorPixels[MaxColorPixels];
XFontStruct           *theFontStruct;
/* choose the right font with the size you 
 *want with xlsfonft -l */
char                   theFontName[20]; 


/* external Globals from other files */

extern char           *theColorName[];

/*** some local Variables ***/

static int             i;

/*** Functions ***/

/********** inti and free *************/

extern char InitX(int aX, int aY, int aWidth, int aHeight)
{
  X = aX; Y = aY; theHeight = aHeight; theWidth = aWidth;
  theDisplay = XOpenDisplay(theDisplayName);
  if (theDisplay == NULL) {
     fprintf(stderr, "Can't XOpenDisplay %s\n", 
      XDisplayName(theDisplayName));
    return(1);
  }
  theScreen     = DefaultScreen(theDisplay);
  theDepth      = DefaultDepth(theDisplay, theScreen);
  /* Get some Color */
  theWhitePixel = WhitePixel(theDisplay, theScreen);
  theBlackPixel = BlackPixel(theDisplay, theScreen);
  theColormap   = DefaultColormap(theDisplay, theScreen);
  InitDefaultColors(); 
  return(CreateXWindow()); 
  XFlush(theDisplay);
}

static void InitDefaultColors(void)
{
  XColor                 theRGBColor, theHardwareColor;
  int                    theColorStatus;

  if (theDepth <= 1) { /* not monochrome */
    theColorPixels[0] = theBlackPixel;
    for(i=1; i<MaxColorPixels; i++) 
      theColorPixels[i] = theWhitePixel;
  } else {
    for(i=0; i<MaxColorPixels; i++) 
      if (!(theColorStatus = XLookupColor(theDisplay, theColormap, 
          theColorName[i], &theRGBColor, &theHardwareColor))) 
        theColorPixels[i] = theBlackPixel;
      else
        if (!(theColorStatus = XAllocColor(theDisplay, theColormap, 
            &theHardwareColor)))
          theColorPixels[i] = theBlackPixel;
        else
          /* the theHardwareColor.pixel refers to a Colorcell in
           * the Colormap, not to a pixel on the screen */
          theColorPixels[i] = theHardwareColor.pixel;
  }
}

extern char LoadFont(char *AFontName)
{
  char TempFontName[20];
  strcpy(TempFontName, AFontName);
  if (!strcmp(AFontName, "default")) 
    strcpy(TempFontName, DEFAULT_FONT);
  if((theFontStruct) && (!strcmp(theFontName, TempFontName))) return 0;
  else
    if(theFontStruct) XFreeFont(theDisplay, theFontStruct);
  /* get the font, the font ID */
  theFontStruct = XLoadQueryFont(theDisplay, TempFontName);
  if (theFontStruct == NULL) {
      fprintf(stderr, "Can't XLoadQueryFont !\n");
      return 1;
  }
  XSetFont(theDisplay, theGC, theFontStruct->fid);
  strcpy(theFontName, TempFontName);
  return (0);
}

extern void QuitX(void)
{
  DestroyXWindow();
  if (theDisplay) {
    if(theFontStruct) XFreeFont(theDisplay, theFontStruct);
    XCloseDisplay(theDisplay);
  }
}

static char CreateXWindow(void)
{
  XSetWindowAttributes   theWindowAttributes;
  unsigned long          theWindowMask;
  unsigned int           theCursorShape = XC_star;
  XSizeHints             theSizeHints;
  XWMHints               theWMHints;
  Pixmap                 theIconPixmap;
  XClassHint             theClassHint;
  XGCValues              theGCValues;
  unsigned long          theGCValueMask = 0L;

  theWindowAttributes.border_pixel      = theBlackPixel;
  theWindowAttributes.background_pixel  = theBlackPixel;
  theWindowAttributes.override_redirect = OverrideRedirectFlag;
  theWindowAttributes.cursor            = theCursor;
  /* Set the mask for the actual used fields */
  theWindowMask = (CWBackPixel | CWBorderPixel | CWOverrideRedirect |
                   CWCursor);
  /* create the cursor first */
  theCursor = XCreateFontCursor(theDisplay, theCursorShape);
  theWindow = XCreateWindow(theDisplay, 
              RootWindow( theDisplay, theScreen), /* -parent */
              X, Y, theWidth, theHeight, theBorderWidth,
              theDepth, InputOutput, CopyFromParent,
              theWindowMask, &theWindowAttributes);
  if (theWindow == 0) { fprintf(stderr, "Can't create Window\n"); return(1); }
  /* give the windows manager some hints */
  theClassHint.res_name  = theProgramName;
  theClassHint.res_class = "BasicWindow";
  XSetClassHint(theDisplay, theWindow, &theClassHint);
  theSizeHints.flags   = USPosition | USSize; /* US -> P for promp */
  theSizeHints.x       = X;
  theSizeHints.y       = Y;
  theSizeHints.width   = theWidth;
  theSizeHints.height  = theHeight;
  XSetNormalHints(theDisplay, theWindow, &theSizeHints);
  /* change the cursor */
  XDefineCursor(theDisplay, theWindow, theCursor);
  /* icon hints */
  theIconPixmap = XCreateBitmapFromData(theDisplay, theWindow, 
                    CAIcon_bits, CAIcon_width, CAIcon_height);
  theWMHints.icon_pixmap   = theIconPixmap;
  theWMHints.initial_state = NormalState;
  theWMHints.flags         = IconPixmapHint | StateHint;
  XSetWMHints(theDisplay, theWindow, &theWMHints);
  /* get a graphics context GC for the new window */
  theGC = XCreateGC(theDisplay, theWindow, theGCValueMask, 
                    &theGCValues);
  if (theGC == 0) { fprintf(stderr, "Can't create GC\n"); return(1); }
  XSetForeground(theDisplay, theGC, theWhitePixel);
  XSetBackground(theDisplay, theGC, theBlackPixel);
  /* set up the font for the GC*/
  LoadFont("default");
  /* Init Event Handling */
  InitEvent(theWindow);
  /* map the window to the sceen and raise it to the top */
  XMapRaised(theDisplay, theWindow);
  /* set the window display actual, flush up all requests */
  XFlush(theDisplay);

  return 0;
}

static void DestroyXWindow(void)
{
  if (theWindow && theDisplay) {
    if (theGC) XFreeGC(theDisplay, theGC); /* only stop maintainig */
    XUnmapWindow(theDisplay, theWindow);
    XDestroyWindow(theDisplay, theWindow);
  }
}

/********** drawing , getting info  *************/


extern void SetColor(char *ColorName)
{
  i = 0;
  while((strcmp(ColorName, theColorName[i])) && (i < MaxColorPixels))
    i++;
  if (i < MaxColorPixels)
    XSetForeground(theDisplay, theGC, theColorPixels[i]);
}

extern void XLine(int x1, int y1, int x2, int y2)
{
  XDrawLine(theDisplay, theWindow, theGC, x1, y1, x2, y2);
}

extern void XRect(int x1, int y1, int width, int height)
{
  XDrawRectangle(theDisplay, theWindow, theGC, x1, y1, width, height);
}

extern void XFillRect(int x1, int y1, int width, int height)
{
  XFillRectangle(theDisplay, theWindow, theGC, x1, y1, width, height);
}

extern void XFill(char *ColorName)
{
  SetColor(ColorName);
  XFillRectangle(theDisplay, theWindow, theGC, 0,0, 
    DisplayWidth(theDisplay, theScreen), 
    DisplayHeight(theDisplay, theScreen));
}

extern void XOval(int x1, int y1, int width, int height)
{
  XDrawArc(theDisplay, theWindow, theGC, 
    x1-width/2, y1-height/2, width, height, 0,360*64);
}

extern void XFillOval(int x1, int y1, int width, int height)
{
  XFillArc(theDisplay, theWindow, theGC, 
    x1-width/2, y1-height/2, width, height, 0,360*64);
}

extern void XText(int x, int y, char *theString)
{
  int theStringLength = strlen(theString);
  XDrawString(theDisplay, theWindow, theGC, 
              x, y, theString, theStringLength);
}

extern void XFontText(int x, int y, char *theString, 
                      char *AFontName)
{
  int     theStringLength = strlen(theString);
  char    TempFontName[20];
  strcpy(TempFontName,theFontName);
  LoadFont(AFontName);
  XDrawString(theDisplay, theWindow, theGC, 
              x, y, theString, theStringLength);
  LoadFont(TempFontName);
}

extern void XMessageText(char *theString)
{
  int     x,y;
  int     theStringLength = strlen(theString);
  char    TempFontName[20];
  strcpy(TempFontName, theFontName);
  /* get the actual geometry */
  ActualizeWindowInfo();

  if (LoadFont(LARGE_FONT) == 0)
      {
	  x = theWidth/2-
	      XTextWidth(theFontStruct, theString, theStringLength)/2;
      }
  y = theHeight/2;
  SetColor("white");
  XDrawImageString(theDisplay, theWindow, theGC,
                   x, y, theString,theStringLength);
  LoadFont(TempFontName);
  XFlush(theDisplay);
  sleep(1);
}
extern void XCenteredText(int x, int y, char *theString)
{
  if (theFontStruct) {
    int theStringLength = strlen(theString);
    XDrawString(theDisplay, theWindow, theGC, 
     x-XTextWidth(theFontStruct, theString, strlen(theString))/2, 
     y+theFontStruct->ascent/2,
     theString, theStringLength);
  } else fprintf(stderr, "No FontStruct");
}

extern void XImageText(int x, int y, char *theString)
{
  int theStringLength = strlen(theString);
  XDrawImageString(theDisplay, theWindow, theGC, 
                   x, y, theString, theStringLength);
}

extern int GetXWindowInfo(void)
{
  ActualizeWindowInfo();
  fprintf(stderr, "%s version %d of the X window System, X%d R%d\n",
    ServerVendor(theDisplay), VendorRelease(theDisplay),
    ProtocolVersion(theDisplay), ProtocolRevision(theDisplay));
  fprintf(stderr, "The Display    : %s\n", 
    XDisplayName(theDisplayName));
  fprintf(stderr, "Display Height : %d\n", 
    DisplayHeight(theDisplay, theScreen));
  fprintf(stderr, "Display Width  : %d\n", 
    DisplayWidth(theDisplay, theScreen));
  fprintf(stderr, "ColorDepth     : %d\n", theDepth);
  fprintf(stderr, "Window         : %d\n", theWindow);
  fprintf(stderr, "Window Height : %d\n", theHeight);
  fprintf(stderr, "Window Width  : %d\n", theWidth);
  fprintf(stderr, "\n");
  return(theWindow);
}

extern void ActualizeWindowInfo(void)
{
  Window root;
  XGetGeometry(theDisplay, theWindow, &root,
    &X, &Y, &theWidth, &theHeight, &theBorderWidth, &theDepth);
}

/************ Events ****************/

static void InitEvent(Window AWindow)
{
  XSelectInput(theDisplay, theWindow, EVENT_MASK);
}

extern void GetNextEvent(XEvent *AEvent)
/* waits for the event, so blocks your program */
{
  XNextEvent(theDisplay, AEvent);
}

extern char CheckMaskEvent(XEvent *theEvent)
/* get the next event if any, and reports true */
{
  return(XCheckMaskEvent(theDisplay, EVENT_MASK, theEvent));
}

extern char CheckTypedEvent(XEvent *theEvent, int theType)
/* looks for something special, no waiting */
{
  return(XCheckTypedEvent(theDisplay, theType, theEvent));
}

extern int LookupPressedKey(XEvent AEvent, char *AKey, 
                               int AKeyBufferMaxLen, KeySym *AKeySym)
{
  XComposeStatus       theComposeStatus; /* or use NULL */

  return(XLookupString(&AEvent.xkey, AKey, AKeyBufferMaxLen,
                       AKeySym, &theComposeStatus));
}
