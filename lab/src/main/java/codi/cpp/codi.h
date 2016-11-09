
static char Alloc3DSpace(void);

extern void InitCASpace(void);

extern char InitCA(TCACell **ACASpace);

extern void ResetCA_IO(void);

extern void FreeCASpace(void);

static void Kicking(void);

static char GrowthStep(int ANumberSteps);

static char SignalStep(int ANumberSteps);

extern char PrintCellInfo(unsigned long NrCell);

extern char StepCA(int ANumberSteps, char Phase);

static void WrappedKickingGrowing(void);

static void WrappedKickingSignaling(void);
