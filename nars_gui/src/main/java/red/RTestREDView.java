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

import java.awt.*;
import javax.swing.*;
import java.util.*;
import junit.framework.*;
import red.lineTree.*;

/** Regression test for REDView.
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDView extends TestCase {
	static final int fcRandomIterations = 100;
	static final int fcRandomTextLength =  40;

	public RTestREDView(String name) {
		super(name);
	}
	
	protected void setUp() {
		fStyles = new REDStyle[5];
        fStyles[0] = new REDStyle(new Color(250, 100, 100), new Color(0, 0, 0), REDLining.SINGLEUNDER, "Monospaced", "PLAIN", 24, null);
        fStyles[1] = new REDStyle(new Color(100, 100, 250), new Color(0, 0, 0), REDLining.SINGLEUNDER, "Monospaced", "PLAIN", 18, null);
        fStyles[2] = new REDStyle(new Color(90, 150, 30), new Color(0, 0, 0), REDLining.SINGLEUNDER, "Monospaced", "PLAIN", 14, null);
        fStyles[3] = new REDStyle(new Color(250, 250, 100), new Color(0, 0, 0), REDLining.SINGLEUNDER, "Monospaced", "PLAIN", 10, null);
        fStyles[4] = new REDStyle(new Color(0, 100, 100), new Color(0, 0, 0), REDLining.SINGLEUNDER, "Monospaced", "PLAIN", 8, null);
		
		fLog = new StringBuffer();
		fOpLog = new StringBuffer();
		fRnd = new Random(System.currentTimeMillis());
		fText = new REDText("");
		fText.replace(0, 0, fcFileContent);
		REDStyle s = new REDStyle(new Color(120, 10, 190), new Color(0, 0, 0), REDLining.SINGLEUNDER, "Monospaced", "PLAIN", 24, null);
		fText.setStyle(5, 20, s);
		fView = new REDView(fText); 
		fView.setBorder(BorderFactory.createMatteBorder(3, 5, 2, 4, Color.red));
        fFrame = new JFrame("Regression test for REDView"); fFrame.setSize(400, 300);
        fFrame.getContentPane().add(new JScrollPane(fView), BorderLayout.CENTER);
	}		
	
	protected void tearDown() {
		fFrame.setVisible(false);
	}

	public void checkTopLine() {
        for (int i = 0; i < fText.getNrOfLines(); i++) {
        	int a = fView.getLineTop(i);
        	int b = fView.debugGetLineTop(i);
			assertTrue("Line #" + i + ":  expected line top at " + b + " but got " + a + 
				"\n\nLog:\n" + fLog + 
				"\nOpLog:\n" + fOpLog, a == b);
        }
	}
	
	public void dumpTree(String header) {
		System.out.println(header);
		fView.fTopLines.iterateInOrder(new RTestDottyPrinter());
	}
	
	public void testEmptyFile() {
		fText.replace(0, fText.length(), "");
		assertEquals(0, fView.getLineTop(0));
		FontMetrics metrics = fView.getFontMetrics(fText.getDefaultStyle().getFont());
		assertEquals(metrics.getHeight(), fView.getLineTop(1));
	}
	
	public void testTopLine() {
        checkTopLine();
        fText.replace(10, 10, "Foobar");
        checkTopLine();
//        dumpTree("Before inserting a new line");
        fText.replace(20, 20, "XXX\nYYY");
//        dumpTree("After inserting a new line");
        checkTopLine();
        REDStyle s1 = new REDStyle(new Color(250, 100, 100), new Color(0, 0, 0), REDLining.SINGLEUNDER, "Monospaced", "PLAIN", 24, null);
        REDStyle s2 = new REDStyle(new Color(100, 100, 250), new Color(0, 0, 0), REDLining.SINGLEUNDER, "Monospaced", "PLAIN", 18, null);
        REDStyle s3 = new REDStyle(new Color(90, 150, 30), new Color(0, 0, 0), REDLining.SINGLEUNDER, "Monospaced", "PLAIN", 14, null);
        REDStyle s4 = new REDStyle(new Color(250, 250, 100), new Color(0, 0, 0), REDLining.SINGLEUNDER, "Monospaced", "PLAIN", 10, null);
        REDStyle s5 = new REDStyle(new Color(0, 100, 100), new Color(0, 0, 0), REDLining.SINGLEUNDER, "Monospaced", "PLAIN", 8, null);
//        dumpTree("Before setting style");
        fText.setStyle(634, 638, s1);
//        dumpTree("After setting style");
        checkTopLine();
        
        fText.setStyle(690, 706, s2);
        checkTopLine();
        
        fText.setStyle(852, 861, s3);
        checkTopLine();
        
        fText.setStyle(913, 919, s4);
        checkTopLine();
        
        fText.setStyle(689, 940, s5);
        checkTopLine();        
        
        fText.replace(950, 953, "");
        checkTopLine();
        
		fText.replace(634, 638, "");
        checkTopLine();
        
        fText.replace(0, 93, "");
        checkTopLine();
	}
	
	public void doOp(int op, int start, int end, int style, String str) {
		switch(op) {
			case 0:
				fText.replace(start, start, str);
			break;
			case 1:
				fText.setStyle(start, end, fStyles[style]);
			break;
			case 2:
				fText.replace(start, end, "");
			break;
			case 3:
				fText.replace(start, end, str);
			break;
		}		
	}
	
	/**
	  * highly inefficient method of quoting <CODE>\n</CODE>
	  */
	public static String quote(String str) {
		String retVal = str;
		int idx = retVal.indexOf('\n');
		while (idx != -1) {
			retVal = retVal.substring(0, idx) + "\\n" + retVal.substring(idx+1);
			idx = retVal.indexOf('\n', idx+1);
		}
		return '"' + retVal + '"';
	}

	public void testRandomOps() {
		int nrOps = fcRandomIterations;
		if (System.getProperty("red.RTestQuick") != null) {
			nrOps = fcRandomIterations / 10;
		}
		for (int x = 0; x < nrOps; x++) {
			int start = fRnd.nextInt(fText.length());
			int end = start + fRnd.nextInt(Math.min(fText.length()-start, fcRandomTextLength));
			String str = createRandomString(fRnd.nextInt(fcRandomTextLength));
			String q = quote(str);
			int op = fRnd.nextInt(4);
			int style = fRnd.nextInt(5);
//			System.out.println("doOp(" + op + ", " + start + ", " + end + ", " + style + ", " + q + ");");
//			System.out.println("checkTopLine();");
			fOpLog.append("doOp(").append(op).append(", ").append(start).append(", ").append(end).append(", ").append(style).append(", ").append(q).append(");\n");
//			fLog.append("Random op #" + x
//				+ " will be " + op + ", start: " + start + ", end: " + end + ", style: " + style
//				+ "\n text length is " + fText.length()
//				+ "\n start of line (" + fText.getLineForPosition(start) + ") for start is: " + fText.getLineStart(fText.getLineForPosition(start))
//				+ "\n end of line for start is: " + fText.getLineEnd(fText.getLineForPosition(start))
//				+ "\n start of line (" + fText.getLineForPosition(end) + ") for end is: " + fText.getLineStart(fText.getLineForPosition(end))
//				+ "\n end of line for end is: " + fText.getLineEnd(fText.getLineForPosition(end))
//				+ "\n string is: <<" + str + ">>\n");
			doOp(op, start, end, style, str);
			checkTopLine();
		}
	}
	
	/** Randomly found problem.
	  * This case is problematic since it deletes multiple lines at the end of the text.
	  */
	public void testRandomBug1() {
		doOp(1, 1804, 1815, 1, "LZY");
		checkTopLine();
		doOp(3, 866, 900, 4, "ZDFZJETFWNEY S");
		checkTopLine();
	}		
	
	/** Randomly found problem.
	  * This case is problematic since it deletes multiple lines at the end of the text.
	  */
	public void testRandomBug2() {
		doOp(1, 1195, 1201, 1, "");
		doOp(0, 2628, 2653, 1, "TIOSZSK\nNJBCVK \nGGEJLMUVYIWA\nX\nGY\nU");
		doOp(3, 874, 893, 1, "FSFO WQUTHTOI");
		doOp(2, 691, 722, 3, "DZOQS");
		doOp(1, 2430, 2456, 3, "ZZD");
		checkTopLine();
		doOp(3, 172, 186, 1, "MUMUGOB XLVYUIFDXG TOROHPHTZUDC GP GNLL");
		doOp(1, 2003, 2020, 4, "RZTXKBNLCCNAUP\nHDWKUEQOFESJFKIO");
		doOp(0, 1743, 1756, 2, "MWRXWYGXFRKO\n");
		doOp(3, 1102, 1138, 4, "YSEDKDY");
		checkTopLine();
		doOp(0, 440, 445, 3, "XVDAC\nHRVQYON");
		doOp(1, 1458, 1479, 4, "YOZFQNMYGOVVAIE AEQBD\nW XZG");
		doOp(3, 169, 171, 3, "KZVWFWBK\nENNDNRXIRYMKPZUQCCGC");
		doOp(1, 2643, 2668, 3, "VUHJCILCBDTYEQFDNKHERUZ");
		checkTopLine();
		doOp(3, 1695, 1703, 0, "WDNXUXBJ\nWUR");
		doOp(0, 1209, 1237, 4, "TNX QXJTORFASOXDLXZWZP PQDQKZETXO\nEWW");
		doOp(0, 447, 482, 3, "RKEGFPSSHUASE L\nX");
		doOp(2, 139, 152, 2, "HWP A\nPU VQPENPONAQFUGGPVTPVYPVVUHTNZ\n");
		checkTopLine();
		doOp(3, 1371, 1375, 3, "OEMDTXSQ");
		checkTopLine();
		doOp(1, 2499, 2519, 0, "Y");
		checkTopLine();
		doOp(0, 167, 173, 0, "TATTRMF M");
		checkTopLine();
		doOp(3, 2255, 2275, 1, "MKAYEEQATXEDH");
		checkTopLine();
		doOp(2, 2706, 2740, 0, "E ");
		checkTopLine();
	}
	
	/** Line height cache problem */
	public void testRandomBug3() {
		doOp(1, 531, 548, 4, "CG WSZVB");
		doOp(3, 1568, 1571, 0, "VC C SYUXUUAACS");
		doOp(2, 1694, 1712, 1, "JIOVE FPF\nPB\nIKMKP");
		doOp(2, 1741, 1754, 3, "AECKKIOOTWOLVCD\nVV");
		doOp(3, 774, 781, 1, "GFJEMNFKOYBMKIQPGLXS\nWUHGQXOATMJARDZHFY");
		checkTopLine();
		doOp(1, 512, 520, 0, "TLHINQOZCZCY\nSHCJCENSEY\nFXV\nTNC\nEGFX");
		doOp(2, 1108, 1127, 0, "RDFKFSGU\nMMNJETXJ");
		doOp(0, 2188, 2214, 4, "WS");
		doOp(1, 88, 119, 2, "AYWCEDCW\nI");
		doOp(1, 761, 774, 2, "BXUEHMJUMFAYGMLFHGI\nEJFWJZKNP CGIWNQDGP");
		doOp(3, 2094, 2111, 2, "GN\nYCW MEZRVCWL GRIQSU\nI");
		checkTopLine();
		doOp(3, 168, 170, 0, "MOJW\nUBX\nXHZRTJYV\nDAESLOIHDS");
		doOp(2, 853, 860, 4, "DIOQL\nIYKXLFURI");
		doOp(3, 344, 382, 3, "DG");
		doOp(0, 902, 910, 0, "KQULDZAXWPOOHXZLAHEGGPUXMQ");
		doOp(1, 821, 860, 3, "SPB KNAQYSFMNDVALTRHWD");
		checkTopLine();
		doOp(3, 2206, 2245, 4, "IUOLBPRVAQDYESNZCVFCJ\nGC QDQOXDYXI");
		doOp(2, 1887, 1920, 4, "HD\nSEJ SW\nGLJDVAFCTJOWMBTOKHFLL");
		doOp(1, 1021, 1042, 4, "CLFDEBTHSPKWOBYRUFUI\nCAP\nK");
		doOp(1, 589, 589, 0, "DVEUJMLJTEQAQKQHDBT");
		doOp(3, 1382, 1415, 1, "CAFYDSCGJVWOLS");
		checkTopLine();
		doOp(1, 2108, 2147, 2, " FNL AZOSYWRKQVABRZMXAKNNPJIDBIC");
		checkTopLine();
		doOp(3, 2204, 2204, 1, "OQEMJXYQJG\nKVNNA");
		checkTopLine();
		doOp(1, 2606, 2628, 1, "KHUBFDZO");
		checkTopLine();
		doOp(2, 773, 790, 2, "VNDQ");
		checkTopLine();
		doOp(3, 1428, 1462, 2, "GHBLV X TXYFNSEVQDEPE\nXYUNNBHVNIV");
		checkTopLine();
		doOp(2, 730, 753, 4, "TXDDQJHF");
		doOp(0, 2130, 2163, 3, "NUHPFJAZKZA");
		doOp(3, 1860, 1870, 2, "ZBSM PGFJSJA \nKQYWSXNV");
		doOp(0, 2216, 2224, 1, "QTVPKBZVHTPT");
		doOp(0, 2451, 2454, 3, "JFJ\nON FEDA");
		checkTopLine();
		doOp(1, 261, 274, 1, "KWHE VBSUY EZC QZVAOAGTK");
		doOp(2, 1913, 1913, 4, "LTLN\nSYGYNWOU\nORHLJPTXBTNUCTRCEGQO");
		doOp(3, 1440, 1455, 4, "MQWT BLVUFDA\nNJCB");
		doOp(2, 2603, 2638, 0, "QGIQQPEZ");
		doOp(1, 2291, 2316, 2, "JZDXGSTAROJLGTQWIMFT");
		checkTopLine();
		doOp(3, 1493, 1509, 1, "MDCMZG");
		doOp(0, 2017, 2019, 3, "");
		doOp(3, 1444, 1445, 3, "MPYRQUOFTO");
		doOp(1, 1817, 1839, 3, "KYBHBUITZXGXNH  SPVZSK\nYR KPA");
		checkTopLine();
		doOp(2, 2345, 2363, 2, "");
		checkTopLine();
	}

	public void testScratch() {
		fText.replace(fText.getLineStart(4), fText.length(), "");
        checkTopLine();
//        dumpTree("Before replace");
		fText.replace(148, 148, "LFXWBSX   FPAGUZMRWFXSRQFLKANSN");
//        dumpTree("After replace");
        checkTopLine();
	}
	
	static class DummyListener implements REDViewEventListener {
		public DummyListener() {
			fBuf = new StringBuffer();
		}
		
		public void beforeSelectionChange(int oldFrom, int oldTo, int newFrom, int newTo) {
			fBuf.append("bsc(").append(oldFrom).append(", ").append(oldTo).append(", ").append(newFrom).append(", ").append(newTo).append(") ");
		}
		
		public void afterSelectionChange(int oldFrom, int oldTo, int newFrom, int newTo) {
			fBuf.append("asc(").append(oldFrom).append(", ").append(oldTo).append(", ").append(newFrom).append(", ").append(newTo).append(") ");
		}
		
		public void gotFocus() {
			fBuf.append("gf() ");
		}
		
		public void lostFocus() {
			fBuf.append("lf() ");
		}
		
		public void beforeModeChange(int oldMode, int newMode) {
			fBuf.append("bmc(").append(oldMode).append(", ").append(newMode).append(") ");
		}
		
		public void afterModeChange(int oldMode, int newMode) {
			fBuf.append("amc(").append(oldMode).append(", ").append(newMode).append(") ");
		}
		
		public String toString() {
			return fBuf.toString();
		}
		
		public void reset() {
			fBuf = new StringBuffer();
		}
		
		StringBuffer fBuf;
	}
	
	public void testListener() {
		DummyListener listener = new DummyListener();
		fView.addREDViewEventListener(listener);
		fView.setSelection(5, 7);
		assertEquals("bsc(0, 0, 5, 7) asc(0, 0, 5, 7) ", listener.toString());
		listener.reset();
		fView.setSelection(3, 3);
		assertEquals("bsc(5, 7, 3, 3) asc(5, 7, 3, 3) ", listener.toString());
		listener.reset();
		fView.setMode(REDAuxiliary.VIEWMODE_READONLY);
		fView.setMode(REDAuxiliary.VIEWMODE_INSERT);
		assertEquals("bmc(" + REDAuxiliary.VIEWMODE_INSERT + ", " + REDAuxiliary.VIEWMODE_READONLY + ") " +
			"amc(" + REDAuxiliary.VIEWMODE_INSERT + ", " + REDAuxiliary.VIEWMODE_READONLY + ") " +
			"bmc(" + REDAuxiliary.VIEWMODE_READONLY + ", " + REDAuxiliary.VIEWMODE_INSERT + ") " +
			"amc(" + REDAuxiliary.VIEWMODE_READONLY + ", " + REDAuxiliary.VIEWMODE_INSERT + ") ", listener.toString());
		listener.reset();
		fView.setSelection(5);
		assertEquals("bsc(3, 3, 5, 5) asc(3, 3, 5, 5) ", listener.toString());
		listener.reset();
		fView.moveLeft(REDView.PACE_CHAR);
		assertEquals("bsc(5, 5, 4, 4) asc(5, 5, 4, 4) ", listener.toString());
		listener.reset();
		fView.moveRight(REDView.PACE_CHAR);
		assertEquals("bsc(4, 4, 5, 5) asc(4, 4, 5, 5) ", listener.toString());
		listener.reset();
		fView.selectLeft(REDView.PACE_CHAR);
		assertEquals("bsc(5, 5, 4, 5) asc(5, 5, 4, 5) ", listener.toString());
		listener.reset();
		fView.selectRight(REDView.PACE_CHAR);
		assertEquals("bsc(4, 5, 5, 5) asc(4, 5, 5, 5) ", listener.toString());
		listener.reset();
	}
	
	public void testHighlightLine() {
		assertEquals(false, fView.hasHighlightLine());
		fView.setHighlightLine(20);
		assertEquals(true, fView.hasHighlightLine());
		assertEquals(20, fView.getHighlightLine());
		fView.setHighlightColor(null);
		assertEquals(true, fView.hasHighlightLine());
		assertEquals(20, fView.getHighlightLine());
		fView.setHighlightLine(-1);
		assertEquals(false, fView.hasHighlightLine());
		assertEquals(-1, fView.getHighlightLine());
		fView.setHighlightLine(-10);
		assertEquals(false, fView.hasHighlightLine());
		assertEquals(-1, fView.getHighlightLine());

		fView.setHighlightLine(5);
		fText.replace(3, 7, "foo");
		assertEquals(false, fView.hasHighlightLine());
		
		fView.setHighlightLine(5);
		fText.replace(3, 7, "");
		assertEquals(false, fView.hasHighlightLine());
	}
	
	public void testSelectionCaretTransition() {
		fText.replace(0, fText.length(), "XXXX\nXXXX\nXXXX\nXXXX");
		
		fView.setSelection(1, 3);
		fView.moveLeft(REDView.PACE_CHAR);
		assertEquals(1, fView.fSelFrom);
		
		fView.setSelection(1, 3);
		fView.moveRight(REDView.PACE_CHAR);
		assertEquals(3, fView.fSelFrom);
		
		fView.setSelection(6, 8);
		fView.moveLeft(REDView.PACE_LINE);
		assertEquals(3, fView.fSelFrom);

		fView.setSelection(8, 6);
		fView.moveLeft(REDView.PACE_LINE);
		assertEquals(1, fView.fSelFrom);
		
		fView.setSelection(6, 8);
		fView.moveRight(REDView.PACE_LINE);
		assertEquals(13, fView.fSelFrom);
		
		fView.setSelection(8, 6);
		fView.moveRight(REDView.PACE_LINE);
		assertEquals(11, fView.fSelFrom);
				
		fView.setSelection(6, 13);
		fView.moveLeft(REDView.PACE_LINE);
		assertEquals(8, fView.fSelFrom);
		
		fView.setSelection(13, 6);
		fView.moveLeft(REDView.PACE_LINE);
		assertEquals(1, fView.fSelFrom);

		fView.setSelection(6, 13);
		fView.moveRight(REDView.PACE_LINE);
		assertEquals(18, fView.fSelFrom);

		fView.setSelection(13, 6);
		fView.moveRight(REDView.PACE_LINE);
		assertEquals(11, fView.fSelFrom);

		fView.setSelection(2, 8);
		fView.moveLeft(REDView.PACE_LINEBOUND);
		assertEquals(5, fView.fSelFrom);
		
		fView.setSelection(8, 2);
		fView.moveLeft(REDView.PACE_LINEBOUND);
		assertEquals(0, fView.fSelFrom);

		fView.setSelection(2, 8);
		fView.moveRight(REDView.PACE_LINEBOUND);
		assertEquals(9, fView.fSelFrom);
		
		fView.setSelection(8, 2);
		fView.moveRight(REDView.PACE_LINEBOUND);
		assertEquals(4, fView.fSelFrom);

		fText.replace(0, fText.length(), "xxxx xxxx xxxx xxxx");
		fView.setSelection(7, 12);
		fView.moveLeft(REDView.PACE_WORD);
		assertEquals(10, fView.fSelFrom);

		fView.setSelection(12, 7);
		fView.moveLeft(REDView.PACE_WORD);
		assertEquals(5, fView.fSelFrom);

		fView.setSelection(7, 12);
		fView.moveRight(REDView.PACE_WORD);
		assertEquals(14, fView.fSelFrom);

		fView.setSelection(12, 7);
		fView.moveRight(REDView.PACE_WORD);
		assertEquals(9, fView.fSelFrom);
	}
	
	public void testSelectLeftBug1() {
		fText.replace(0, fText.length(), "XXXX\nXXXX\nXXXX\nXXXX");
		
		fView.setSelection(3, 3);
		fView.selectLeft(REDView.PACE_CHAR);
		assertEquals(REDView.DIR_RIGHT_TO_LEFT, fView.fSelDir);
		fView.selectLeft(REDView.PACE_CHAR);
		assertEquals(1, fView.fSelFrom);
		assertEquals(3, fView.fSelTo);
	}
	
	public static Test suite() {
		return new TestSuite(RTestREDView.class);
	}
	
	public String createRandomString(int length) {
		String charTable = "\n ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		StringBuilder buf = new StringBuilder(length+1);
		for (int x = 0; x < length; x++) {
			buf.append(charTable.charAt(fRnd.nextInt(charTable.length())));
		}
		return buf.toString();
	}


	Random fRnd;
	REDText fText;
	REDView fView;
	JFrame fFrame;
	StringBuffer fLog, fOpLog;
	REDStyle fStyles[];
	static final String fcFileContent = 
"Don't meddle in the affairs of wizards,\n" +
"for they are subtle and quick to anger.\n" +
		'\n' +
"The Lord of the Rings.\n" +
		'\n' +
"And it is also said: \n" +
"\t\"Don't ask the elves for counsel,\n" +
"\tfor they will both say yes and no\".\n" +
		'\n' +
"NR.\tTest A\tTest B\n" +
"#1\tpos.\t\tneg.\n" +
"#2\tneg.\t\tpos.\n" +
		'\n' +
"#\n" +
"\t#\n" +
"\t\t#\n" +
"\t\t\t#\n" +
"\t\t\t\t#\n" +
"\t\t\t\t\t#\n" +
"\t\t\t\t\t\t#\n" +
"#\t#\t#\t#\t#\t#\t#\t#\t\n" +
		'\n' +
		'\n' +
"The Balrog reached the bridge. Gandalf stood in the middle of the span, leaning on the staff in his left hand, \n" +
"but in his other hand Glamdring gleamed, cold and white. \n" +
		'\n' +
"His enemy halted again, facing him, and the shadow about it reached out like two vast wings. It raised the \n" +
"whip, and the thongs whined and cracked. Fire came from its nostrils. But Gandalf stood firm. \n" +
		'\n' +
"`You cannot pass,' he said. The orcs stood still, and a dead silence fell. `I am a servant of the Secret Fire, \n" +
"wielder of the flame of Anor. You cannot pass. The dark fire will not avail you, flame of Udun. Go back to the \n" +
"Shadow! You cannot pass.' \n" +
		'\n' +
"The Balrog made no answer. The fire in it seemed to die, but the darkness grew. It stepped forward slowly on \n" +
"to the bridge, and suddenly it drew itself up to a great height, and its wings were spread from wall to wall; \n" +
"but still Gandalf could be seen, glimmering in the gloom; he seemed small, and altogether alone: grey and bent, \n" +
"like a wizened tree before the onset of a storm. \n" +
		'\n' +
"From out of the shadow a red sword leaped flaming. Glamdring glittered white in answer. There was a ringing \n" +
"clash and a stab of white fire. The Balrog fell back and its sword flew up in molten fragments. The wizard \n" +
"swayed on the bridge, stepped back a pace, and then again stood still. 'You cannot pass! ' he said. \n" +
		'\n' +
"With a bound the Balrog leaped full upon the bridge. Its whip whirled and hissed. 'He cannot stand alone! ' \n" +
"cried Aragorn suddenly and ran back along the bridge. 'Elendil!' he shouted. 'I am with you, Gandalf! ' \n" +
"`Gondor! ' cried Boromir and leaped after him. At that moment Gandalf lifted his staff, and crying aloud he \n" +
"smote the bridge before him. The staff broke asunder and fell from his hand. A blinding sheet of white flame \n" +
"sprang up. The bridge cracked. Right at the Balrog's feet it broke, and the stone upon which it stood crashed \n" +
"into the gulf, while the rest remained, poised, quivering like a tongue of rock thrust out into emptiness. \n" +
		'\n' +
"With a terrible cry the Balrog fell forward, and its shadow plunged down and vanished. But even as it fell it \n" +
"swung its whip, and the thongs lashed and curled about the wizard's knees, dragging him to the brink. He \n" +
"staggered and fell, grasped vainly at the stone, and slid into the abyss. 'Fly, you fools! ' he cried, and was \n" +
"gone. The fires went out, and blank darkness fell. The Company stood rooted with horror staring into the pit. \n" +
		'\n';
}
