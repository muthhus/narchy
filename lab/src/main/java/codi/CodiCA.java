package codi;

import java.awt.*;

public class CodiCA extends CA {
  private int CAChanged;
  private int InputSum;
  private boolean SignalingInited;
  // We shadow the CASpace Variable with a new type.
  class CodiCell { // instead of a struct
    int Type;
    int Chromo;
    int Gate;
    int Activ;
    int IOBuf[] = new int[6];
  }
  protected CodiCell CASpace[][][];
  // Constants for the celltypes.
  private final int BLANK      =0; // black
  private final int NEURONSEED =1; 
  private final int NEURON     =1; // white
  private final int AXON       =2; // red
  private final int DEND       =4; // green
  private final int AXON_SIG   =2;
  private final int DEND_SIG   =4;
  // get the grphics faster
  private final int SIG_COL   =5;    // yellow
  private final int ERROR_COL   =13; // blue
  private int ColorSpace[][][]; // Only draw what has changed.
  private int ActualColor;

  public CodiCA() {
    super();
    MaxSteps = 100;
    SpaceSizeX = 100;
    SpaceSizeY = 50;
    SpaceSizeZ = 1;
    CellSizeX = 3;
    CellSizeY = 3;
    SpaceSize  = SpaceSizeX*SpaceSizeY*SpaceSizeZ;
    CAFrameSizeX = SpaceSizeX*CellSizeX;
    CAFrameSizeY = SpaceSizeY*CellSizeY;
    ColorSpace = new int[SpaceSizeX][SpaceSizeY][SpaceSizeZ];
    CASpace = new CodiCell[SpaceSizeX][SpaceSizeY][SpaceSizeZ];
    for(int ix=0;ix<SpaceSizeX;ix++)
      for(int iy=0;iy<SpaceSizeY;iy++) 
	for(int iz=0;iz<SpaceSizeZ;iz++)
	  CASpace[ix][iy][iz] = new CodiCell();
    CLRGraphicsAfterStep = false;
  }
  
  protected void InitCA() {
    CountCAStps=0; 
    CAChanged = 1;
    SignalingInited = false; 
    bFirstStart = true;
    for(int ix=0;ix<SpaceSizeX;ix++)
      for(int iy=0;iy<SpaceSizeY;iy++)
	for(int iz=0;iz<SpaceSizeZ;iz++) {
	  ColorSpace[ix][iy][iz] = 0;
	  CASpace[ix][iy][iz].Type = 0;
	  CASpace[ix][iy][iz].Activ = 0;
	  for(int i=0; i<6; i++) CASpace[ix][iy][iz].IOBuf[i]=0;	  
	  CASpace[ix][iy][iz].Chromo = (random.nextInt() % 256);
	  // Restrict to grid.
	  if(((ix+1)%2)*(iy%2)==1) 
	    CASpace[ix][iy][iz].Chromo =
	      (CASpace[ix][iy][iz].Chromo & ~3) | 12;
	  if((ix%2)*((iy+1)%2)==1) 
	    CASpace[ix][iy][iz].Chromo = 
	      (CASpace[ix][iy][iz].Chromo & ~12) | 3;
	  // Kill unwanted neuronseeds. Neuronsee only on crossings.
	  if((ix%2)+(iy%2)!=0) CASpace[ix][iy][iz].Chromo &= ~192;
	  // Decrease prob of neuronseeds (decrease along x-asis).
	  if((CASpace[ix][iy][iz].Chromo >>> 6) == NEURONSEED)
	    if((random.nextInt() % SpaceSizeX) < ix/2) 
	      CASpace[ix][iy][iz].Chromo &= ~192;
	  // Restrict axon-initial-growth in neuron to XY-plain.
	  if((CASpace[ix][iy][iz].Chromo >>> 6) == NEURONSEED)
	    CASpace[ix][iy][iz].Chromo = 
	      (CASpace[ix][iy][iz].Chromo & 192) | 
	      ((CASpace[ix][iy][iz].Chromo & 63) % 4);
	}
  } 

  protected void Kicking() {
    // For the Neighborhood interaction.
    // Names for the buffer correspond to the I-Buf,
    // so where the Information (signal) came from;
    // I.g. in the north buffer (+y=2) of a cell is either what came
    // from the norht (after kick) or the what will go to the south 
    // (before kick).
    // IOBuf[0..5] = east(+x), west(-x), north(+y), south(-y),
    // top(+z), bottom(-z) 
    // actulize the 3 direction one at a time, without wrap-around.
   
    // For the positive directions 
    for(int iz=0;iz<SpaceSizeZ;iz++)
      for(int iy=0;iy<SpaceSizeY;iy++)
	for(int ix=0;ix<SpaceSizeX;ix++) {
	  // top (+z) 
	  if(iz!=SpaceSizeZ-1)
	    CASpace[ix][iy][iz].IOBuf[4] = 
	      CASpace[ix][iy][iz+1].IOBuf[4]; //[ix][iy][iz+1]
	  else CASpace[ix][iy][iz].IOBuf[4] = 0;
	  // north (+y) 
	  if(iy!=SpaceSizeY-1)
	    CASpace[ix][iy][iz].IOBuf[2] = 
	      CASpace[ix][iy+1][iz].IOBuf[2]; //[ix][iy+1][iz]
	  else CASpace[ix][iy][iz].IOBuf[2] = 0;
	  // east (+x) 
	  if(ix!=SpaceSizeX-1)
	    CASpace[ix][iy][iz].IOBuf[0] = 
	      CASpace[ix+1][iy][iz].IOBuf[0]; //[ix+1][iy][iz]
	  else CASpace[ix][iy][iz].IOBuf[0] = 0;
	}
    // For the negtive directions 
    // CASpaceTemp pionts to the last cell now 
    for(int iz=SpaceSizeZ-1;iz>=0;iz--)
      for(int iy=SpaceSizeY-1;iy>=0;iy--)
	for(int ix=SpaceSizeX-1;ix>=0;ix--) {
	  // bottom (-z) 
	  if(iz!=0)
	    CASpace[ix][iy][iz].IOBuf[5] = 
	      CASpace[ix][iy][iz-1].IOBuf[5]; //[ix][iy][iz-1]
	  else CASpace[ix][iy][iz].IOBuf[5] = 0;
	  // south (-y) 
	  if(iy!=0)
	    CASpace[ix][iy][iz].IOBuf[3] = 
	      CASpace[ix][iy-1][iz].IOBuf[3]; //[ix][iy-1][iz]
	  else CASpace[ix][iy][iz].IOBuf[3] = 0;
	  // west (-x) 
	  if(ix!=0)
	    CASpace[ix][iy][iz].IOBuf[1] = 
	      CASpace[ix-1][iy][iz].IOBuf[1]; //[ix-1][iy][iz]
	  else CASpace[ix][iy][iz].IOBuf[1] = 0;
	}
  }

  private void  InitSignaling(){
    SignalingInited = true;
    for(int iz=0;iz<SpaceSizeZ;iz++)
      for(int iy=0;iy<SpaceSizeY;iy++)
	for(int ix=0;ix<SpaceSizeX;ix++) {
	  CASpace[ix][iy][iz].Activ=0;
	  for(int i=0; i<6; i++) CASpace[ix][iy][iz].IOBuf[i]=0;
	  if(CASpace[ix][iy][iz].Type == NEURON)
	    CASpace[ix][iy][iz].Activ= (random.nextInt() % 32);
	}
  }

  protected void StepCA() {
    CountCAStps++; if(CountCAStps==MaxSteps) InitCA();
    if(CAChanged==1) GrowthStep(); 
    else {
      if(!SignalingInited) InitSignaling();
      SignalStep();
    }
  }

  protected  int GrowthStep()
  { 
    CAChanged = 0;
    for(int iz=0;iz<SpaceSizeZ;iz++)
      for(int iy=0;iy<SpaceSizeY;iy++)
	for(int ix=0;ix<SpaceSizeX;ix++) {
	  ////// now the Rules ////
	  // the Chromo is a bitmask for the 6 directions[Bit0..5]:
	  // east(+x), west(-x), north(+y), south(-y),
	  // top(+z), bottom(-z) 
	  // As the direction are to be seen as input directios,
	  // the Gate and Chromo are masks that invert the directions
	  // when applied to output selection. 
	  switch(CASpace[ix][iy][iz].Type) {
	  case BLANK :                    // BLANK 
	    // see if it is a NeuronSeed (in bit ..7,8 of Chromo) 
	    if ((CASpace[ix][iy][iz].Chromo >>> 6) == NEURONSEED) {
	      CASpace[ix][iy][iz].Type = NEURON;
	      CAChanged = 1;
	      // and inform the neighbors immediately 
	      CASpace[ix][iy][iz].Gate = 
		(int)((CASpace[ix][iy][iz].Chromo & 63) % 6);
	      for(int i=0; i<6; i++) 
		CASpace[ix][iy][iz].IOBuf[i] = DEND_SIG;
	      CASpace[ix][iy][iz].IOBuf[CASpace[ix][iy][iz].Gate] 
		= AXON_SIG;
	      CASpace[ix][iy][iz].IOBuf
		[((int)(CASpace[ix][iy][iz].Gate%2)*-2)+1+CASpace
						       [ix][iy][iz].Gate] 
		= AXON_SIG;
	      break;
	    }
	    // Test for no signal 
	    InputSum =  
	      CASpace[ix][iy][iz].IOBuf[0]+
	      CASpace[ix][iy][iz].IOBuf[1]+
	      CASpace[ix][iy][iz].IOBuf[2]+
	      CASpace[ix][iy][iz].IOBuf[3]+
	      CASpace[ix][iy][iz].IOBuf[4]+
	      CASpace[ix][iy][iz].IOBuf[5];
	    if(InputSum == 0) break;
	    // Test for AXON_SIG's 
	    InputSum =  
	      (CASpace[ix][iy][iz].IOBuf[0] & AXON_SIG)+
	      (CASpace[ix][iy][iz].IOBuf[1] & AXON_SIG)+
	      (CASpace[ix][iy][iz].IOBuf[2] & AXON_SIG)+
	      (CASpace[ix][iy][iz].IOBuf[3] & AXON_SIG)+
	      (CASpace[ix][iy][iz].IOBuf[4] & AXON_SIG)+
	      (CASpace[ix][iy][iz].IOBuf[5] & AXON_SIG);
	    if(InputSum == AXON_SIG) {// exactly one AXON_SIG 
	      CASpace[ix][iy][iz].Type = AXON;
	      CAChanged = 1;
	      for(int i=0; i<6; i++) 
		if (CASpace[ix][iy][iz].IOBuf[i] == AXON)
		  CASpace[ix][iy][iz].Gate = (int)i;
	      for(int i=0; i<6; i++) 
		if (((CASpace[ix][iy][iz].Chromo >>> i) & 1) != 0)
		  CASpace[ix][iy][iz].IOBuf[i] = AXON_SIG;
		else CASpace[ix][iy][iz].IOBuf[i] = 0;
	      break;
	    }
	    if(InputSum > AXON_SIG) {// more than one AXON_SIG 
	      for(int i=0; i<6; i++) 
		CASpace[ix][iy][iz].IOBuf[i] = 0;
	      break;
	    }
	    // Test for DEMD_SIG's 
	    InputSum =  
	      (CASpace[ix][iy][iz].IOBuf[0] & DEND_SIG)+
	      (CASpace[ix][iy][iz].IOBuf[1] & DEND_SIG)+
	      (CASpace[ix][iy][iz].IOBuf[2] & DEND_SIG)+
	      (CASpace[ix][iy][iz].IOBuf[3] & DEND_SIG)+
	      (CASpace[ix][iy][iz].IOBuf[4] & DEND_SIG)+
	      (CASpace[ix][iy][iz].IOBuf[5] & DEND_SIG);
	    if(InputSum == DEND_SIG) {// exactly one DEND_SIG 
	      CAChanged = 1;
	      CASpace[ix][iy][iz].Type = DEND;
	      for(int i=0; i<6; i++) 
		if ((CASpace[ix][iy][iz].IOBuf[i])!=0)
		  CASpace[ix][iy][iz].Gate = (int)(((i%2)*-2)+1+i);
	      for(int i=0; i<6; i++) 
		if (((CASpace[ix][iy][iz].Chromo >>> i) & 1) !=0)
		  CASpace[ix][iy][iz].IOBuf[i] = DEND_SIG;
		else CASpace[ix][iy][iz].IOBuf[i] = 0;
	      break;
	    }
	    // default(more than one DEND_SIG and no AXON_SIG) 
	    for(int i=0; i<6; i++) 
	      CASpace[ix][iy][iz].IOBuf[i] = 0;
	    break;
	  case NEURON :                       // NEURON 
	    for(int i=0; i<6; i++) 
	      CASpace[ix][iy][iz].IOBuf[i] = DEND_SIG;
	    CASpace[ix][iy][iz].IOBuf[CASpace[ix][iy][iz].Gate] 
	      = AXON_SIG;
	    CASpace[ix][iy][iz].IOBuf
	      [((CASpace[ix][iy][iz].
		 Gate%2)*-2)+1+CASpace[ix][iy][iz].Gate] 
	      = AXON_SIG;
	    break;
	  case AXON :                         // AXON 
	    for(int i=0; i<6; i++) 
	      if (((CASpace[ix][iy][iz].Chromo >>> i) & 1) !=0)
		CASpace[ix][iy][iz].IOBuf[i] = AXON_SIG;
	      else CASpace[ix][iy][iz].IOBuf[i] = 0; 
	    break;
	  case DEND :                         // DENDRITE 
	    for(int i=0; i<6; i++) 
	      if (((CASpace[ix][iy][iz].Chromo >>> i) & 1) !=0)
		CASpace[ix][iy][iz].IOBuf[i] = DEND_SIG;
	      else CASpace[ix][iy][iz].IOBuf[i] = 0;
	    break;
	  }
	}
    Kicking(); 
    return CAChanged;
  }

  protected int SignalStep() {
    // feed in some activity 
    // appFeedIn();
    for(int iz=0;iz<SpaceSizeZ;iz++)
      for(int iy=0;iy<SpaceSizeY;iy++)
	for(int ix=0;ix<SpaceSizeX;ix++) {
	  // // now the Rules ////
	  switch(CASpace[ix][iy][iz].Type) {
	  case BLANK :                    // BLANK 
	    //for(int i=0; i<6; i++) 
	    //CASpace[ix][iy][iz].IOBuf[i] = 0;
	    break;
	  case NEURON :                // NEURON 
	    InputSum = 1 +            // add default gain +
	      CASpace[ix][iy][iz].IOBuf[0]+
	      CASpace[ix][iy][iz].IOBuf[1]+
	      CASpace[ix][iy][iz].IOBuf[2]+
	      CASpace[ix][iy][iz].IOBuf[3]+
	      CASpace[ix][iy][iz].IOBuf[4]+
	      CASpace[ix][iy][iz].IOBuf[5]-
	      CASpace[ix][iy][iz].IOBuf[CASpace[ix][iy][iz].Gate]- 
	      CASpace[ix][iy][iz].IOBuf
	      [((CASpace[ix][iy][iz].
		 Gate%2)*-2)+1+CASpace[ix][iy][iz].Gate];
	    for(int i=0; i<6; i++) 
	      CASpace[ix][iy][iz].IOBuf[i] = 0;
	    CASpace[ix][iy][iz].Activ += InputSum;
	    if (CASpace[ix][iy][iz].Activ > 31) { // Fire now.
	      CASpace[ix][iy][iz].IOBuf[CASpace[ix][iy][iz].Gate] = 1;
	      CASpace[ix][iy][iz].IOBuf
		[((CASpace[ix][iy][iz].
		   Gate%2)*-2)+1+CASpace[ix][iy][iz].Gate] 
		= 1;
	      CASpace[ix][iy][iz].Activ = 0;
	    }
	    break; 
	  case AXON :                     // AXON 
	    for(int i=0; i<6; i++) 
	      CASpace[ix][iy][iz].IOBuf[i] =
		(CASpace[ix][iy][iz].IOBuf[CASpace[ix][iy][iz].Gate]);
	    if((CASpace[ix][iy][iz].IOBuf[CASpace[ix][iy][iz].Gate])!=0)
	      CASpace[ix][iy][iz].Activ = 1;
	    else CASpace[ix][iy][iz].Activ = 0; 
	    break;
	  case DEND :                     // DENDRITE
	    InputSum =
	      CASpace[ix][iy][iz].IOBuf[0]+
	      CASpace[ix][iy][iz].IOBuf[1]+
	      CASpace[ix][iy][iz].IOBuf[2]+
	      CASpace[ix][iy][iz].IOBuf[3]+
	      CASpace[ix][iy][iz].IOBuf[4]+
	      CASpace[ix][iy][iz].IOBuf[5] ;
	    if (InputSum > 2) InputSum = 2;
	    for(int i=0; i<6; i++) 
	      CASpace[ix][iy][iz].IOBuf[i] = 0;
	    CASpace[ix][iy][iz].IOBuf
	      [CASpace[ix][iy][iz].Gate] = (int)InputSum;
	    if (InputSum!=0) CASpace[ix][iy][iz].Activ = 1;
	    else CASpace[ix][iy][iz].Activ = 0; 
	    break;
	  }
	}
    Kicking(); 
    return 0;
  }

  protected void DrawCA(Graphics g) {
    int iz=0;
    int PosX, PosY;
    if (bFirstStart) { 
      DrawCAFrame(g);
      g.setColor(Color.black);
      g.fillRect(Offset,Offset, 
		 SpaceSizeX*CellSizeX, SpaceSizeY*CellSizeY);
      bFirstStart = false; 
    }
    // plot CA-Space    
    PosX = Offset-CellSizeX;
    for(int ix=0;ix<SpaceSizeX;ix++) {
      PosX += CellSizeX;
      PosY = Offset-CellSizeY;
      for(int iy=0;iy<SpaceSizeY;iy++) {
	PosY += CellSizeY;
	if(CASpace[ix][iy][iz].Type!=0) {
	  if(CASpace[ix][iy][iz].Activ!=0) {
	    if(CASpace[ix][iy][iz].Type != NEURON)
	      ActualColor = 5;//g.setColor(Color.yellow); /* signal */
	    else ActualColor = 1;//g.setColor(Color.white);
	  }
	  else
	    switch (CASpace[ix][iy][iz].Type) {
            case  NEURON: ActualColor = 1;//g.setColor(Color.white); 
	      break;
            case  AXON:   ActualColor = 2;//g.setColor(Color.red); 
	      break;
            case  DEND:   ActualColor = 4;//g.setColor(Color.green); 
	      break;
            default :     ActualColor = 13;//g.setColor(Color.blue);
	      System.out.println("__"+CASpace[ix][iy][iz].Type+"__"); 
	    }
	  if(ColorSpace[ix][iy][iz] != ActualColor) {
	    ColorSpace[ix][iy][iz] = ActualColor;
	    switch (ActualColor) {
            case  NEURON: g.setColor(Color.white); 
	      break;
            case  AXON:   g.setColor(Color.red); 
	      break;
            case  DEND:   g.setColor(Color.green); 
	      break;
            case SIG_COL: g.setColor(Color.yellow); 
	      break;
            default :     g.setColor(Color.blue);
	    }
	    g.fillRect(PosX,PosY,CellSizeX,CellSizeY);
	  }
	}
      }
    }
  }

}
