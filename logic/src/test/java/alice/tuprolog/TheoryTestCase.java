package alice.tuprolog;

import junit.framework.TestCase;
import org.junit.Assert;

public class TheoryTestCase extends TestCase {

	public void testToStringWithParenthesis() throws InvalidTheoryException {
		String before = "a :- b, (d ; e).";
		Theory theory = new Theory(before);
		String after = theory.toString();
		Assert.assertEquals(theory.toString(), new Theory(after).toString());
	}
	
	public void testAppendClauseLists() throws InvalidTheoryException, MalformedGoalException {
		Term[] clauseList = {new Struct("p"), new Struct("q"), new Struct("r")};
		Term[] otherClauseList = {new Struct("a"), new Struct("b"), new Struct("c")};
		Theory theory = new Theory(new Struct(clauseList));
		theory.append(new Theory(new Struct(otherClauseList)));
		Prolog engine = new Prolog();
		engine.setTheory(theory);
		Assert.assertTrue((engine.solve("p.")).isSuccess());
		Assert.assertTrue((engine.solve("b.")).isSuccess());
	}

}
