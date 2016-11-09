
static char EventLoop(void);

extern char RefreshWindowDebug(char ClearWindow);

static char RefreshWindow(Window AWindow);
 
static unsigned long DisplayXYZtoCACell(int x, int y, int z);
       
static void ReactToMouse(XEvent theEvent);

static void FreeMarkers(void);

static char ScanCommandLine(int argc, char **argv);

static void PrintProgammInfo(void);

static void ResetCA_after_Event(void);

static void Run();

static void InitRandSeed(void);
