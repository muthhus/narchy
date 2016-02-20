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

import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import red.*;
import red.lineTree.*;
import red.rexParser.*;
import red.util.*;

/** This plugin performs on-the-fly syntax highlighting.
  * @author rli@chello.at
  * @tier plugin
  * @see REDSyntaxHighlighterManager
  */
public class REDSyntaxHighlighter extends REDPlugin implements REDRexParserStopper {
	public REDSyntaxHighlighter(REDSyntaxHighlighterDefinition def) {
		fChangeCount = 0;
		fRestartFromLine = -1;
		fSetStateCollector = new ArrayList();
		fBatchQ = new LinkedList();
		fParser = new REDRexParser();
		fParser.putClientProperty("lastLit", new REDSyntaxHighlighterPosition());
		fParser.putClientProperty("style0", REDStyleManager.getDefaultStyle());
		Iterator iter = def.iterator();
		while (iter.hasNext()) {
			REDSyntaxHighlighterRule r = (REDSyntaxHighlighterRule) iter.next();
			try {
				r.installInParser(REDRexParser.defaultState(), def.getCaseSensitive(), fParser, REDStyleManager.getDefaultStyle());
			}
			catch (REDRexMalformedPatternException mpe) {
				REDGLog.error("RED", "Malformed pattern '" + mpe.getPattern() + "' in Syntax Highlighter Definition '" + def.getName() + "'.");
			}
		}
		REDSyntaxHighlighterKeyword wordIgnorer = new REDSyntaxHighlighterKeyword(def.getIgnorePattern(), REDStyleManager.getDefaultStyle());
		try {
			wordIgnorer.installInParser(REDRexParser.defaultState(), def.getCaseSensitive(), fParser, REDStyleManager.getDefaultStyle());
		}
		catch (REDRexMalformedPatternException mpe) {
			REDGLog.error("RED", "Malformed ignore pattern '" + def.getIgnorePattern() + "' in Syntax Highlighter Definition '" + def.getName() +"'.");
		}
	}
	
	public void setEditor(REDEditor editor) {
		super.setEditor(editor);
		fParser.putClientProperty("editor", fEditor);
	
		if (editor != null) {
			fMarks = editor.createMarkTree();
			synchronized (this) {
				updateLines(0, true);	
			}
		}
	}
	
	int getState(int line) {
		if (line == 0) {
			return REDRexParser.defaultState();
		}
		int lineStart = fEditor.getLineStart(line);
		fSetStateCollector.clear();
		fSetStateCollector = fMarks.collectMarks(lineStart, lineStart, null, fSetStateCollector);
		if (fSetStateCollector.size() == 0) {
			return -1;
		}
		else {
			return ((REDSyntaxHighlighterState) ((REDMark) fSetStateCollector.get(0)).getValue()).fState;
		}
	}
	
	void setState(int line, int state) {
		int lineStart = fEditor.getLineStart(line);
		fSetStateCollector.clear();
		fSetStateCollector = fMarks.collectMarks(lineStart, lineStart, null, fSetStateCollector);
		REDSyntaxHighlighterState hiState;
		if (fSetStateCollector.size() == 0) {
			hiState = new REDSyntaxHighlighterState();
			fMarks.createMark(lineStart, hiState);
		}
		else {
			hiState = (REDSyntaxHighlighterState) ((REDMark) fSetStateCollector.get(0)).getValue();
		}
		hiState.fState = state;
	}
	
	void deleteStates(int fromLine, int toLine) {
		fMarks.deleteMarks(fEditor.getLineStart(fromLine), fEditor.getLineStart(toLine));
	}
	
//	void dumpStates() {
//		for (int i = 0; i < fEditor.getNrOfLines(); i++) {
//			REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighter", "State for line " + i + " is " + getState(i));
//		}
//	}
	
	public boolean mustStop(REDRexParser parser, int line, int offset, int state) {		
//		REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighter", "mustStop called for line: " + line + " with state: " + state + " and getState() == " + getState(line) + ", offset = " + offset);
		if (fRestartFromLine != -1) {
			return true;
		}
		boolean retVal = !fUpdateAll && line > fLastParsedLine && state == getState(line);
		if (!retVal && offset == 0) {
//			REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighter",  "Setting state for line " + line + " to " + state);
			setState(line, state);
		}
//		REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighter",  "mustStop returns: " + retVal);
		fLastParsedLine = line;
		return retVal;
	}

	private static class MyRunnable implements Runnable {
		public void run() { }
	}

	class ConcurrentParser extends Thread {
		ConcurrentParser() {
			fLine = -1;
			fRestartFromLine = -1;
		}	
		
		synchronized boolean isRunning() {
			return fLine > -1;
		}
		
		synchronized void setRestartFromLine(int line) {
			if (fRestartFromLine == -1 || line < fRestartFromLine) {
				fRestartFromLine = line;
			}
		}
		
		public void run() {
			do {
				fParser.putClientProperty("batchQ", fBatchQ);
				fParser.putClientProperty("changeCount", fChangeCount);
				fParser.putClientProperty("batchExecutor", new BatchExecutor());
				int state = prepareParsing(fLine);
				fParser.parse(fEditor.getLineSource(), fLine, state, REDSyntaxHighlighter.this, false);
				REDSyntaxHighlighterRule.updateLastLit(fParser, fLastParsedLine, 0, 0);
				synchronized (this) {
					fLine = fRestartFromLine;
					fRestartFromLine = -1; 						
				}
			}
			while (fLine > -1);
			SwingUtilities.invokeLater(new BatchExecutor());
		}	
		int fLine;		
	}
	
	class BatchExecutor implements Runnable {
		public void run() {
			if (!SwingUtilities.isEventDispatchThread()) {
				throw new Error("Tried to execute style op batch on wrong thread.");
			}
			synchronized (fBatchQ) {
				try {
					while (true) {
						REDSyntaxHighlighterBatchEntry e = (REDSyntaxHighlighterBatchEntry) fBatchQ.removeFirst();
						e.execute(fEditor, fChangeCount);
					}
				}
				catch (NoSuchElementException nsee) { }
			}
			fParser.putClientProperty("batchExecutor", new BatchExecutor());			
		}
	}
	
	synchronized public boolean isParsing() {
		return fConcurrentParser != null && fConcurrentParser.isRunning();
	}
	
	/** Wait for parser to finish and styles to be applied.
	  * This method can be used by test cases to wait for the syntax highlighter to finish its work.
	  */
	synchronized public void waitForParser() throws InterruptedException {
		if (fConcurrentParser != null) {
			fConcurrentParser.join();
			try {
				SwingUtilities.invokeAndWait(new MyRunnable());	// Wait for batch executor ...
			}
			catch (InvocationTargetException ite) { 
				throw new Error(String.valueOf(ite));
			}
		}
	}
		
	
	private int prepareParsing(int line) {
		int lineStart = fEditor.getLineStart(line);
		int state = getState(line); REDAssert.ensure(state != -1);
		fParser.putClientProperty("envStyle", fParser.getClientProperty("style"+state));
		REDSyntaxHighlighterPosition pos = (REDSyntaxHighlighterPosition) fParser.getClientProperty("lastLit");
		pos.fPosition = lineStart;
		fLastParsedLine = line;
		return state;
	}
	
	synchronized void updateLines(int line, boolean all) {
		long start = System.currentTimeMillis();
//		REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighter", "updateLines entered for line = " + line + ", all = " + all);		
		fUpdateAll = all;
		if (fUpdateAll) 
		{
			fConcurrentParser = new ConcurrentParser();
			fConcurrentParser.fLine = line;
			fConcurrentParser.start();
		}
		else {
			int state = prepareParsing(line);
			fParser.putClientProperty("batchQ", null);
			fParser.parse(fEditor.getLineSource(), line, state, this, false);
			REDSyntaxHighlighterRule.updateLastLit(fParser, fLastParsedLine, 0, 0);
		}
		long end = System.currentTimeMillis();
		REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighter", "Syntax Highlighter took " + (end - start) + " msec.");
	}
	
	synchronized public void afterInsert(int from, int to) { 
		fChangeCount++;
		if (fConcurrentParser != null) {
			synchronized (fConcurrentParser) {
				if (fConcurrentParser.isRunning()) {
					fConcurrentParser.setRestartFromLine(fEditor.getLineForPosition(from));
					return;
				}
			}
		}
		updateLines(fEditor.getLineForPosition(from), false);
	}

	public void beforeDelete(int from, int to) {
//		REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighter", "deleteStates: " + (fEditor.getLineForPosition(from) + 1) + " - " + fEditor.getLineForPosition(to));
		deleteStates(fEditor.getLineForPosition(from) + 1, fEditor.getLineForPosition(to));
	}

	synchronized public void afterDelete(int from, int to) {
		fChangeCount++;
		if (fConcurrentParser != null) {
			synchronized (fConcurrentParser) {
				if (fConcurrentParser.isRunning()) {
					fConcurrentParser.setRestartFromLine(fEditor.getLineForPosition(from));
					return;
				}
			}
		}
		updateLines(fEditor.getLineForPosition(from), false);
	}
	
	synchronized public void afterLoad() { 
		fChangeCount++;
		if (fConcurrentParser != null) {
			synchronized (fConcurrentParser) {
				if (fConcurrentParser.isRunning()) {
					fConcurrentParser.setRestartFromLine(0);
					return;
				}
			}
		}
		updateLines(0, true);
	}
	
	synchronized public void afterFileLoad(String filename) { 
//		REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighter", "Creating new mark tree.");
		fChangeCount++;
		fMarks = fEditor.createMarkTree();
		if (fConcurrentParser != null) {
			synchronized (fConcurrentParser) {
				if (fConcurrentParser.isRunning()) {
					fConcurrentParser.setRestartFromLine(0);
					return;
				}
			}
		}
		updateLines(0, true);
	}
	
	synchronized public void afterFileSave(String filename) {
//		REDTracer.info("red.plugins.synHi", "REDSyntaxHighlighter", "Creating new mark tree.");
		fChangeCount++;
		fMarks = fEditor.createMarkTree();
		if (fConcurrentParser != null) {
			synchronized (fConcurrentParser) {
				if (fConcurrentParser.isRunning()) {
					fConcurrentParser.setRestartFromLine(0);
					return;
				}
			}
		}
		updateLines(0, true);	
	}
	
	REDRexParser fParser;
	boolean fUpdateAll;
	REDMarkTree fMarks;
	ArrayList fSetStateCollector;
	int fLastParsedLine;
	int fChangeCount;
	int fRestartFromLine;
	LinkedList fBatchQ;
	ConcurrentParser fConcurrentParser;
}
	