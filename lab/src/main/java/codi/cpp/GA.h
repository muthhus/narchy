
static char AllocChromoMemo(void);

extern void FreeChromoMemo(void);

extern void RandomChromos(void);

extern char LoadChromo(char *AFileName);

extern char SaveChromo(char *AFileName);

extern char PrintChromo(void);

static void InitCASpaceWithChromo(char *AChromo);

static void StartWithRandomChromo(void);

extern void PrintGAInfo();

extern void RunGA(char* FileName, unsigned long NumberGenerations);

static void Select(void);

static void Cross(void);

static void Mutate(void);

static void FlippingBits(void);
