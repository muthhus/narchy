//    RED - A Java Editor Library
//    Copyright (C) 2003  Robert Lichtenberger
//
//    This library is free software; you can redistribute it and/or
//    modify it under the terms of the GNU Lesser General Public
//    License as published by the Free Software Foundation; either
//    version 2.1 of the License, or (at your option) any later version.
//
//    This library is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//    Lesser General Public License for more details.
//
//    You should have received a copy of the GNU Lesser General Public
//    License along with this library; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 
package red;


import javax.swing.*;
import java.awt.*;


/** Performance tests for fonts / rendering
  * @author Robert Lichtenberger, rli@chello.at
  * @tier test
  */
public class PTestFont {
	static public void  main(String args[]) {
		int iterations;
		try {
			iterations = Integer.valueOf(args[0]);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			iterations = 1000;
		}
		Component jc = new JPanel();
		FontMetrics m = jc.getFontMetrics(jc.getFont());
		Graphics g = jc.getGraphics();
		PTestStopWatch sw = new PTestStopWatch();
		System.out.println("Starting test with " + iterations + " iterations.");
		sw.start();
		for (int x = 0; x < iterations; x++) {
			m.getStringBounds("He who runs away, lives to run another day.", g);
		}
		sw.stop("Measuring metrics");
		sw.start();
		for (int x = 0; x < iterations; x++) {
			m.getStringBounds("He who runs away, lives to run another day. He who runs away, lives to run another day.", g);
		}
		sw.stop("Measuring metrics, double size string");
		sw.start();
		for (int x = 0; x < iterations; x++) {
			m.getStringBounds("He who runs away, lives to run another day. He who runs away, lives to run another day.He who runs away, lives to run another day. He who runs away, lives to run another day.He who runs away, lives to run another day. He who runs away, lives to run another day.He who runs away, lives to run another day. He who runs away, lives to run another day.He who runs away, lives to run another day. He who runs away, lives to run another day.", g);
		}
		sw.stop("Measuring metrics, ten times size string");
		sw.start();
		for (int x = 0; x < iterations; x++) {
			m.getStringBounds("Abrakadabr", g);
		}
		sw.stop("Measuring metrics, small string");
		System.exit(0);
	}

}
