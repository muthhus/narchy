package alice.tuprolog;

import junit.framework.TestCase;
import org.junit.Assert;

public class SolutionTestCase extends TestCase {

	public void testGetSubsequentQuery() {
		Prolog engine = new Prolog();
		Term query = new Struct("is", new Var("X"), new Struct("+", new Int(1), new Int(2)));
		Solution result = engine.solve(query);
		Assert.assertTrue(result.isSuccess());
		Assert.assertEquals(query, result.getQuery());
		query = new Struct("functor", new Struct("p"), new Var("Name"), new Var("Arity"));
		result = engine.solve(query);
		Assert.assertTrue(result.isSuccess());
		Assert.assertEquals(query, result.getQuery());
	}

}
