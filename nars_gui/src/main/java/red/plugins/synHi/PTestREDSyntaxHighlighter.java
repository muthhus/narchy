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
 
package red.plugins.synHi;

import red.*;

/** Test performance of syntax highlighter.
  * Must be started from the directory containing PTestREDSyntaxHighlighter.1.in, i.e. red/plugins/synHi.
  * @author rli@chello.at
  * @tier test
  */
public class PTestREDSyntaxHighlighter {
	
	public static void main(String [] args) {
		fgEditor = new REDEditor("", false);
		fgHighlighter = REDSyntaxHighlighterManager.createHighlighter("Java");
		System.out.println("|*Bytes*|*Lines*|*Load file*|*Apply highlighter*|*total*|*kB/sec.*|*Lines/sec.*|");
		long linesPerSec = 0;
		linesPerSec += testFile("PTestREDSyntaxHighlighter.1.in");		
		linesPerSec += testFile("PTestREDSyntaxHighlighter.2.in");		
		linesPerSec += testFile("PTestREDSyntaxHighlighter.3.in");		
		linesPerSec += testFile("PTestREDSyntaxHighlighter.4.in");		
		linesPerSec += testFile("PTestREDSyntaxHighlighter.5.in");		
		linesPerSec += testFile("PTestREDSyntaxHighlighter.6.in");		
		linesPerSec += testFile("PTestREDSyntaxHighlighter.7.in");		
		linesPerSec += testFile("PTestREDSyntaxHighlighter.8.in");		
		linesPerSec += testFile("PTestREDSyntaxHighlighter.9.in");		
		linesPerSec += testFile("PTestREDSyntaxHighlighter.10.in");
		System.out.println("Average lines/sec.: " + (linesPerSec / 10));
	}
	
	static long testFile(String filename) {
		PTestStopWatch totalWatch = new PTestStopWatch();
		totalWatch.start();
		PTestStopWatch watch = new PTestStopWatch();
		watch.start();
		fgEditor.loadFile(filename, false);
		long loadTime = watch.stop();
		watch.start();
		fgEditor.addPlugin(fgHighlighter);
		try {
			fgHighlighter.waitForParser();
		}
		catch (Exception e) {
			throw new Error(String.valueOf(e));
		}
		long hiTime = watch.stop();
		fgEditor.removePlugin(fgHighlighter);
		long totTime = totalWatch.stop();
		
		System.out.println("| " + fgEditor.length() + " | " + fgEditor.getNrOfLines() + " | " + loadTime + " | " + hiTime + " | " + 
			totTime + " | " + (fgEditor.length() / totTime) + " | " + (fgEditor.getNrOfLines() * 1000 / totTime) + " | " );
		return (fgEditor.getNrOfLines() * 1000 / totTime);
	}
	
	static REDEditor fgEditor;
	static REDSyntaxHighlighter fgHighlighter;
}
	