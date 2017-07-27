package alice.tuprolog;

import junit.framework.TestCase;
import org.junit.Assert;

import java.util.List;

public class TheoryManagerTestCase extends TestCase {

//	public void testUnknownDirective() throws InvalidTheoryException {
//		String theory = ":- unidentified_directive(unknown_argument).";
//		Prolog engine = new Prolog();
//		TestWarningListener warningListener = new TestWarningListener();
//		engine.addWarningListener(warningListener);
//		engine.setTheory(new Theory(theory));
//		assertTrue(warningListener.warning.indexOf("unidentified_directive/1") > 0);
//		assertTrue(warningListener.warning.indexOf("is unknown") > 0);
//	}
//
//	public void testFailedDirective() throws InvalidTheoryException {
//		String theory = ":- load_library('UnknownLibrary').";
//		Prolog engine = new Prolog();
//		TestWarningListener warningListener = new TestWarningListener();
//		engine.addWarningListener(warningListener);
//		engine.setTheory(new Theory(theory));
//		assertTrue(warningListener.warning.indexOf("load_library/1") > 0);
//		assertTrue(warningListener.warning.indexOf("InvalidLibraryException") > 0);
//	}

	public void testAssertNotBacktrackable() throws PrologException {
		Prolog engine = new Prolog();
		Solution firstSolution = engine.solve("assertz(a(z)).");
		Assert.assertTrue(firstSolution.isSuccess());
		Assert.assertFalse(firstSolution.hasOpenAlternatives());
	}

	public void testAbolish() throws PrologException {
		Prolog engine = new Prolog();
		String theory = "test(A, B) :- A is 1+2, B is 2+3.";
		engine.setTheory(new Theory(theory));
		TheoryManager manager = engine.getTheoryManager();
		Struct testTerm = new Struct("test", new Struct("a"), new Struct("b"));
		List<ClauseInfo> testClauses = manager.find(testTerm);
		Assert.assertEquals(1, testClauses.size());
		manager.abolish(new Struct("/", new Struct("test"), new Int(2)));
		testClauses = manager.find(testTerm);
		// The predicate should also disappear completely from the clause
		// database, i.e. ClauseDatabase#get(f/a) should return null
		Assert.assertEquals(0, testClauses.size());
	}

	public void testAbolish2() throws InvalidTheoryException, MalformedGoalException{
		Prolog engine = new Prolog();
		engine.setTheory(new Theory("fact(new).\n" +
									"fact(other).\n"));

		Solution info = engine.solve("abolish(fact/1).");
		Assert.assertTrue(info.isSuccess());
		info = engine.solve("fact(V).");
		Assert.assertFalse(info.isSuccess());
	}
	
	// Based on the bugs 65 and 66 on sourceforge
	public void testRetractall() throws Exception {
		Prolog engine = new Prolog();
		Solution info = engine.solve("assert(takes(s1,c2)), assert(takes(s1,c3)).");
		Assert.assertTrue(info.isSuccess());
		info = engine.solve("takes(s1, N).");
		Assert.assertTrue(info.isSuccess());
		Assert.assertTrue(info.hasOpenAlternatives());
		Assert.assertEquals("c2", info.getVarValue("N").toString());
		info = engine.solveNext();
		Assert.assertTrue(info.isSuccess());
		Assert.assertEquals("c3", info.getVarValue("N").toString());

		info = engine.solve("retractall(takes(s1,c2)).");

		Assert.assertTrue(info.isSuccess());
		info = engine.solve("takes(s1, N).");
		Assert.assertTrue(info.isSuccess());
		if (info.hasOpenAlternatives())
			System.err.println(engine.solveNext());
//		Assert.assertFalse(info.hasOpenAlternatives());
		Assert.assertEquals("c2", info.getVarValue("N").toString());
	}

	// TODO test retractall: ClauseDatabase#get(f/a) should return an
	// empty list
	
	public void testRetract() throws InvalidTheoryException, MalformedGoalException {
		Prolog engine = new Prolog();
		TestOutputListener listener = new TestOutputListener();
		engine.addOutputListener(listener);
		engine.setTheory(new Theory("insect(ant). insect(bee)."));
		Solution info = engine.solve("retract(insect(I)), write(I), retract(insect(bee)), fail.");
		Assert.assertFalse(info.isSuccess());
		Assert.assertEquals("antbee", listener.output);
		
	}

}
