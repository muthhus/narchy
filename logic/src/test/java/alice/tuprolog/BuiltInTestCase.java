package alice.tuprolog;

import junit.framework.TestCase;
import org.junit.Assert;

public class BuiltInTestCase extends TestCase {
	
	public void testConvertTermToGoal() throws InvalidTermException {
		Term t = new Var("T");
		Struct result = new Struct("call", t);
		Assert.assertEquals(result, BuiltIn.convertTermToGoal(t));
		Assert.assertEquals(result, BuiltIn.convertTermToGoal(new Struct("call", t)));
		
		t = new Int(2);
		Assert.assertNull(BuiltIn.convertTermToGoal(t));
		
		t = new Struct("p", new Struct("a"), new Var("B"), new Struct("c"));
		result = (Struct) t;
		Assert.assertEquals(result, BuiltIn.convertTermToGoal(t));
		
		Var linked = new Var("X");
		linked.setLink(new Struct("!"));
		Term[] arguments = { linked, new Var("Y") };
		Term[] results = { new Struct("!"), new Struct("call", new Var("Y")) };
		Assert.assertEquals(new Struct(";", results), BuiltIn.convertTermToGoal(new Struct(";", arguments)));
		Assert.assertEquals(new Struct(",", results), BuiltIn.convertTermToGoal(new Struct(",", arguments)));
		Assert.assertEquals(new Struct("->", results), BuiltIn.convertTermToGoal(new Struct("->", arguments)));
	}
	
	//Based on the bug #59 Grouping conjunctions in () changes result on sourceforge
	public void testGroupingConjunctions() throws InvalidTheoryException, MalformedGoalException {
		Prolog engine = new Prolog();
		engine.setTheory(new Theory("g1. g2."));
		Solution info = engine.solve("(g1, g2), (g3, g4).");
		Assert.assertFalse(info.isSuccess());
		engine.setTheory(new Theory("g1. g2. g3. g4."));
		info = engine.solve("(g1, g2), (g3, g4).");
		Assert.assertTrue(info.isSuccess());
	}

}
