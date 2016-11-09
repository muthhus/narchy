
extern char InitX(int aX, int aY, int aWidth, int aHeight);

extern void QuitX(void);

static void InitDefaultColors(void);

extern void SetColor(char *ColorName);

extern char LoadFont(char AFontName[]);

static char CreateXWindow(void);

static void DestroyXWindow(void);

extern void XLine(int x1, int y1, int x2, int y2);

extern void XRect(int x1, int y1, int width, int height);

extern void XFillRect(int x1, int y1, int width, int height);

extern void XFill(char *ColorName);

extern void XOval(int x1, int y1, int width, int height);

extern void XFillOval(int x1, int y1, int width, int height);

extern void XText(int x, int y, char *theString);

extern void XFontText(int x, int y, char *theString, 
                      char *AFontName);

extern void XMessageText(char *theString);

extern void XCenteredText(int x, int y, char *theString);

extern void XImageText(int x, int y, char *theString);

extern int  GetXWindowInfo(void);

extern void ActualizeWindowInfo(void);
 
static void InitEvent(Window AWindow);

extern void GetNextEvent(XEvent *AEvent);

extern char CheckMaskEvent(XEvent *theEvent);

extern char CheckTypedEvent(XEvent *theEvent, int theType);

extern int LookupPressedKey(XEvent AEvent, char *AKey, 
                               int AKeyBufferMaxLen, KeySym *AKeySym);

