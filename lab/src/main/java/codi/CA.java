package codi;

import java.applet.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.net.*;
 
public class CA extends Applet implements Runnable {
  
  protected Thread animatorThread;
  protected boolean frozen; // To stop/start animation.
  protected boolean NoStepDelay; 
  protected int StepDelay;
  protected long MaxSteps;
  protected int SpaceSizeX;
  protected int SpaceSizeY;
  protected int SpaceSizeZ; // not used on this level.
  protected int SpaceSize; 
  protected final int Offset = 10; // min=1 for frame
  protected int CASpace[][];
  protected int CASpaceOld[][];
  protected long CountCAStps;
  protected  int CellSizeX;
  protected  int CellSizeY;
  protected  int CAFrameSizeX;
  protected  int CAFrameSizeY;
  protected Image offImage;
  protected Graphics offGraphics;
  protected Dimension offDimension;
  protected boolean CLRGraphicsAfterStep;
  protected Random random;
  protected boolean bFirstStart;
  protected boolean bInitedNew;

  public CA() {
    random = new Random();
    frozen = false;
    NoStepDelay = true;
    MaxSteps = 100;
    SpaceSizeX = 80;
    SpaceSizeY = 30;
    SpaceSizeZ = 1;
    SpaceSize  = SpaceSizeX*SpaceSizeY*SpaceSizeZ;
    CASpace    = new int[SpaceSizeX][SpaceSizeY];
    CASpaceOld = new int[SpaceSizeX][SpaceSizeY];
    CountCAStps=0;
    CellSizeX = 2;
    CellSizeY = 2;
    CAFrameSizeX = SpaceSizeX*CellSizeX;
    CAFrameSizeY = SpaceSizeY*CellSizeY;
    Random random = new Random();
    bFirstStart = true;
    CLRGraphicsAfterStep = true;
  }
  
  public void init() {
    // determine the AnimationStepDelay
    String str;
    int fps=0; // frames per second
    str = getParameter("fps");
    try {
      if (str != null) {
	fps = Integer.parseInt(str);
      }
    } catch (Exception e) {}
    StepDelay = (fps > 0) ? (1000 / fps) : 80;
    Color foreground = getColorParameter("foreground");
    Color background = getColorParameter("background");
    if(foreground == null) foreground = Color.white;
    if(background == null) background = Color.black;
    setBackground(background);
    setForeground(foreground);
    // Init the CA
    InitCA(); bInitedNew = true;
    repaint(); //Display CA
  }

  public void start() {
    //System.out.println("starting... ");
    if (frozen) { 
      // Do nothing.
    } else {
      //Start animating!
      if (animatorThread == null) {
	animatorThread = new Thread(this);
      }
      animatorThread.start();
    }    
  }

  public void stop() {
    //System.out.println("stopping... ");
    animatorThread = null; 
  }

  public void destroy() {
    //System.out.println("preparing for unloading...");
  }
  		  
  public void run() {
    //System.out.println("running...");
    // Lower this thread's priority,
    // so it can't interfere with other processing going on.
    //Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
    // Remember the starting time.
    long startTime = System.currentTimeMillis();
    // Remember which thread we are.
    Thread currentThread = Thread.currentThread();
    // This is the animation loop.
    while (currentThread == animatorThread) {
      repaint(); // Step and Display CA.
      // Delay depending on how far we are behind.
      if(!NoStepDelay) {
	try {
	  startTime += StepDelay;
	  Thread.sleep(Math.max(0, startTime-System.currentTimeMillis()));
	} catch (InterruptedException e) { break; } // thrown by sleep
      }
    }
  }

  public void paint(Graphics g) { update(g); }

  public void update(Graphics g) {
    // To how the init configuration don't step the first time.
    if(!bInitedNew) StepCA(); else bInitedNew = false;
    // Create the offscreen graphics context, if not exists.
    Dimension d = size();
    if ( (offGraphics == null)
	 || (d.width != offDimension.width)
	 || (d.height != offDimension.height) ) {
      offDimension = d;
      offImage = createImage(d.width, d.height);
      offGraphics = offImage.getGraphics();
    }
    // Erase the previous image.
    if(CLRGraphicsAfterStep || bFirstStart) {
      offGraphics.setColor(getBackground());
      offGraphics.fillRect(0, 0, d.width, d.height);
    }
    else { // clear CountCAStps.
      offGraphics.setColor(getBackground());
      offGraphics.fillRect(Offset,Offset+(SpaceSizeY+1)*CellSizeY, 
			    d.width, d.height);
    }
    // Draw the new stuff.
    DrawCA(offGraphics);
    offGraphics.setColor(getForeground());
    offGraphics.drawString("CA-Step: " + CountCAStps, 
			   Offset, Offset+10+(SpaceSizeY+1)*CellSizeY);
    // Paint the image onto the screen.
    g.drawImage(offImage, 0, 0, this); 
  }

  protected void InitCA() {
    CountCAStps=0;
    for(int i=0;i<SpaceSizeX;i++)
      for(int ii=0;ii<SpaceSizeY;ii++) {
	CASpace[i][ii] = (random.nextInt() % 2) * (random.nextInt() % 2);
      }
    // copy to CASpaceOld
    try { 
      for(int ix=0;ix<SpaceSizeX;ix++) 
	System.arraycopy(CASpace[ix],0,CASpaceOld[ix],0,SpaceSizeY); 
    } catch (Exception e) { System.out.println("arraycopy error!"); }
  } 

  protected void StepCA() { 
    long CountZeroCells=0;
    int iNeighbourSum;
    CountCAStps++;
    // We wrap the borders.
    // Brian's brain (0=ready, 1=activ, 2,4,8,..=recover)
    for(int i=0;i<SpaceSizeX;i++)
      for(int ii=0;ii<SpaceSizeY;ii++) {
	if(CASpaceOld[i][ii]==0) {
	  CountZeroCells++;
	  iNeighbourSum = 
	    (CASpaceOld[(i+SpaceSizeX-1)%SpaceSizeX]
	     [(ii+SpaceSizeY+1)%SpaceSizeY] & 1)
            + (CASpaceOld[(i+SpaceSizeX-1)%SpaceSizeX]
	       [(ii+SpaceSizeY+0)%SpaceSizeY] & 1)
            + (CASpaceOld[(i+SpaceSizeX-1)%SpaceSizeX]
	       [(ii+SpaceSizeY-1)%SpaceSizeY] & 1)
            + (CASpaceOld[(i+SpaceSizeX+0)%SpaceSizeX]
	       [(ii+SpaceSizeY+1)%SpaceSizeY] & 1)
            + (CASpaceOld[(i+SpaceSizeX+0)%SpaceSizeX]
	       [(ii+SpaceSizeY-1)%SpaceSizeY] & 1)
            + (CASpaceOld[(i+SpaceSizeX+1)%SpaceSizeX]
	       [(ii+SpaceSizeY+1)%SpaceSizeY] & 1)
            + (CASpaceOld[(i+SpaceSizeX+1)%SpaceSizeX]
	       [(ii+SpaceSizeY+0)%SpaceSizeY] & 1)
            + (CASpaceOld[(i+SpaceSizeX+1)%SpaceSizeX]
	       [(ii+SpaceSizeY-1)%SpaceSizeY] & 1);
          if(iNeighbourSum >= 2) CASpace[i][ii] = 1;
	} else CASpace[i][ii] = (CASpaceOld[i][ii]*2) % 4;
      }
    if((CountZeroCells==SpaceSize) || (CountCAStps>MaxSteps)) InitCA();
    // copy to CASpaceOld
    try { 
      for(int ix=0;ix<SpaceSizeX;ix++) 
	System.arraycopy(CASpace[ix],0,CASpaceOld[ix],0,SpaceSizeY); 
    } catch (Exception e) { System.out.println("arraycopy error!"); }
  } 

  protected void DrawCAFrame(Graphics g) {
    g.setColor(getForeground());
    g.drawLine(Offset-1,Offset-1,Offset-1,Offset+CAFrameSizeY);
    g.drawLine(Offset-1,Offset-1,Offset+CAFrameSizeX,Offset-1);
    g.drawLine(Offset+CAFrameSizeX,Offset-1,
	       Offset+CAFrameSizeX,Offset+CAFrameSizeY);
    g.drawLine(Offset-1,Offset+CAFrameSizeY,
	       Offset+CAFrameSizeX,Offset+CAFrameSizeY);
  }

  protected void DrawCA(Graphics g) {
    int PosX, PosY;
    DrawCAFrame(g);
    // plot CA-Space    
    PosX = Offset-CellSizeX;
    for(int i=0;i<SpaceSizeX;i++) {
      PosX += CellSizeX;
      PosY = Offset-CellSizeY;
      for(int ii=0;ii<SpaceSizeY;ii++) {
	PosY += CellSizeY;
	// drawPoint does not exist
        if(CASpace[i][ii]>0) {
	  switch(CASpace[i][ii]) {
	    //case 0: g.setColor(Color.white);  break;
	  case 1: g.setColor(Color.black); break;
	  case 2: g.setColor(Color.blue);   break;
	  case 4: g.setColor(Color.getColor("00000F"));   break;
	  case 8: g.setColor(Color.getColor("000010"));   break;
	  default: g.setColor(Color.getColor("888888")); 
	  }
	  g.fillRect(PosX,PosY,CellSizeX,CellSizeY);
	}
      }
    }
  }

  protected Color getColorParameter(String name) {
    String value = this.getParameter(name);
    try { return new Color(Integer.parseInt(value, 16)); }
    catch (Exception e) { return null; }
  }
    
}
