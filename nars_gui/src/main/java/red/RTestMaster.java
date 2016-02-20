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

import junit.framework.*;
import red.lineTree.*;
import red.file.*;
import red.plugins.*;
import red.plugins.synHi.*;
import red.plugins.brMatcher.*;
import red.rexParser.*;
import red.util.*;
import red.xml.*;
	
/** Regression / Unit test master file.
  * This class sums up all the available unit tests within the RED system.
  * @author rli@chello.at
  * @tier test
  */
public class RTestMaster extends TestCase {
	public RTestMaster(String name) {
		super(name);
	}
	
	public static Test suite() {
		REDAuxiliary.setBeepEnabled(false);	// no beeping while testing :-).
		TestSuite suite = new TestSuite();
		suite.addTest(RTestREDLineTree.suite());
		suite.addTest(RTestREDMarkTree.suite());
		suite.addTest(RTestREDFile.suite());
		suite.addTest(RTestREDText.suite());
		suite.addTest(RTestREDView.suite());
		suite.addTest(RTestREDViewLineHeightCache.suite());
		suite.addTest(RTestREDViewController.suite());
		suite.addTest(RTestREDViewReadonlyController.suite());
		suite.addTest(RTestREDEditor.suite());
		suite.addTest(RTestREDCommandProcessor.suite());
		suite.addTest(RTestREDAutoIndent.suite());
		suite.addTest(RTestREDAutoSave.suite());
		suite.addTest(RTestREDTextServer.suite());
		suite.addTest(RTestREDTextProtector.suite());
		suite.addTest(RTestREDRexParser.suite());
		suite.addTest(RTestREDRexStringFinder.suite());
		suite.addTest(RTestREDSyntaxHighlighterManager.suite());
		suite.addTest(RTestREDSyntaxHighlighterDefinition.suite());
		suite.addTest(RTestREDSyntaxHighlighter.suite());
		suite.addTest(RTestREDFinder.suite());
		suite.addTest(RTestREDFinderDialogFactory.suite());		
		suite.addTest(RTestREDResourceManager.suite());
		suite.addTest(RTestREDStream.suite());
		suite.addTest(RTestREDCharacterIterator.suite());
		suite.addTest(RTestREDBracketMatcherManager.suite());
		suite.addTest(RTestREDBracketMatcherDefinition.suite());
		suite.addTest(RTestREDBracketMatcher.suite());
		suite.addTest(RTestREDXMLReading.suite());
		suite.addTest(RTestREDXMLWriting.suite());
		suite.addTest(RTestREDStyleEditor.suite());
		suite.addTest(RTestREDTracer.suite());
		suite.addTest(RTestREDGLog.suite());
		return suite;
	}
}