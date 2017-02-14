/**
 * Copyright (C) 1997-2010 Junyang Gu <mikejyg@gmail.com>
 * 
 * This file is part of javaiPacman.
 *
 * javaiPacman is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * javaiPacman is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with javaiPacman.  If not, see <http://www.gnu.org/licenses/>.
 */

package nars.experiment.pacman0;

import jcog.Util;

import java.awt.*;

import static nars.experiment.pacman0.cmaze.WALL;

public class cpac
{
	// frames to wait after eaten a dot
	final int DOT_WAIT=4;

	//int iDotWait;

	// current position
	public int iX, iY;
	// current direction
	int iDir;

	// the applet this object is associated to
	Window applet;
	Graphics graphics;

	// the pac image
	Image[][] imagePac;

	// the knowledge of the maze
	cmaze maze;

	// the knowledge of the power dots
	cpowerdot powerDot;

	//    cpacmove cAuto;

	//  cpac(Window a, Graphics g, cmaze m, cpowerdot p, cghost cghost[])
	cpac(Window a, Graphics g, cmaze m, cpowerdot p)    {
		applet=a;
		graphics=g;
		maze=m;
		powerDot=p;

		//      cAuto=new cpacmove(this, cghost, m);

		if (a!=null) {
			// initialize pac and pac image
			imagePac = new Image[4][4];
			for (int i = 0; i < 4; i++)
				for (int j = 0; j < 4; j++) {
					imagePac[i][j] = applet.createImage(18, 18);
					cimage.drawPac(imagePac[i][j], i, j);
				}
		}
	}

	public void start()
	{
		iX=10*16;
		iY=10*16;
		iDir=1;		// downward, illegal and won't move
	}

	public void draw()
	{

		try {
			maze.DrawDot(iX/16, iY/16);
			maze.DrawDot(iX/16+(iX%16>0?1:0), iY/16+(iY%16>0?1:0));

			int iImageStep=(iX%16 + iY%16)/2; 	// determine shape of PAc
			if (iImageStep<4)
				iImageStep=3-iImageStep;
			else
				iImageStep-=4;
			graphics.drawImage(imagePac[iDir][Math.abs(iImageStep%4)], iX - 1, iY - 1, applet);
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		}
	}	

	// return 1 if eat a dot
	// return 2 if eat power dot
	public synchronized int move(int iDir)
	{
		int eaten=0;

		//      iNextDir=cAuto.GetNextDir();

//		if (iNextDir!=-1 && iNextDir!=iDir)	// not set or same
//			// change direction
//		{
//			if (iX%16!=0 || iY%16!=0)
//			{
//				// only check go back
//				if (iNextDir%2==iDir%2)
//					iDir=iNextDir;
//			}
//			else    // need to see whether ahead block is OK
//			{
//				if ( mazeOK(iX/16+ ctables.iXDirection[iNextDir],
//						iY/16+ ctables.iYDirection[iNextDir]) )
//				{
//					iDir=iNextDir;
//					iNextDir=-1;
//				}
//			}
//		}
		int jy = Math.round((iY) / 16f);
		int jx = Math.round((iX) / 16f);
		//boolean center = iX % 16 == 0 && iY % 16 == 0;
		//if (center)
		{

			// see whether has eaten something
			switch (maze.iMaze[jy][jx])
			{
			case cmaze.DOT:
				eaten=1;
				maze.iMaze[jy][jx]=cmaze.BLANK;	// eat dot
				maze.iTotalDotcount--;
				//iDotWait=DOT_WAIT;
				break;
			case cmaze.POWER_DOT:
				eaten=2;
				powerDot.eat(jx, jy);
				maze.iMaze[jy][jx]=cmaze.BLANK;
				break;
			}

		}
		int dy = ctables.iYDirection[iDir];
		int dx = ctables.iXDirection[iDir];
		int ny = Util.unitize(iY+dy, 0, maze.HEIGHT*16);
		int nx = Util.unitize(iX+dx, 0, maze.WIDTH*16);

		int ty = Math.round((ny) / 16f);
		int tx = Math.round((nx) / 16f);
		if ( maze.iMaze[ty][tx] != WALL) {
			this.iDir = iDir;
			iX = nx;
			iY = ny;
		} else {
			//just clamp the current position
			iY = Util.unitize(iY, 0, maze.HEIGHT*16);
			iX = Util.unitize(iX, 0, maze.WIDTH*16);
		}


		return(eaten);
	}	

	boolean mazeOK(int iRow, int icol)
	{
		return (maze.iMaze[icol][iRow] & (WALL | cmaze.DOOR)) == 0;
	}

	/** returns if moved */
	public boolean move(int d, int s, cpcman c) {
		int px = iX;
		int py = iY;
		int eaten = 0;
		for ( ; s >= 0; s--) {
			eaten += move(d);
		}
		if (eaten > 0)
			c.eatDots(eaten);
		return (px!=iX) || (py!=iY);
	}
}









