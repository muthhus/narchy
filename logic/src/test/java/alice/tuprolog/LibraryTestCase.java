package alice.tuprolog;

import junit.framework.TestCase;
import org.junit.Assert;

public class LibraryTestCase extends TestCase {
	
	public void testLibraryFunctor() throws PrologException {
		Prolog engine = new Prolog();
		engine.addLibrary(new TestLibrary());
		Solution goal = engine.solve("N is sum(1, 3).");
		Assert.assertTrue(goal.isSuccess());
		Assert.assertEquals(new Int(4), goal.getVarValue("N"));
	}
	
	public void testLibraryPredicate() throws PrologException {
		Prolog engine = new Prolog();
		engine.addLibrary(new TestLibrary());
		TestOutputListener l = new TestOutputListener();
		engine.addOutputListener(l);
		engine.solve("println(sum(5)).");
		Assert.assertEquals("sum(5)", l.output);
	}

}
