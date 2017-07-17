package alice.tuprolog;

import junit.framework.TestCase;
import org.junit.Assert;

/**
 * @author Matteo Iuliani
 * 
 *         Test del funzionamento delle eccezioni lanciate dai predicati della
 *         DCGLibrary
 */
public class DCGLibraryExceptionsTestCase extends TestCase {

	// verifico che phrase(X, []) lancia un errore di instanziazione
	public void test_phrase_2_1() throws Exception {
		Prolog engine = new Prolog();
		engine.addLibrary("alice.tuprolog.lib.DCGLibrary");
		String goal = "catch(phrase(X, []), error(instantiation_error, instantiation_error(Goal, ArgNo)), true).";
		Solution info = engine.solve(goal);
		Assert.assertTrue(info.isSuccess());
		Struct g = (Struct) info.getTerm("Goal");
		Assert.assertTrue(g.isEqual(new Struct("phrase_guard", new Var("X"),
				new Struct())));
		Int argNo = (Int) info.getTerm("ArgNo");
        assertEquals(1, argNo.intValue());
	}

	// verifico che phrase(X, [], []) lancia un errore di instanziazione
	public void test_phrase_3_1() throws Exception {
		Prolog engine = new Prolog();
		engine.addLibrary("alice.tuprolog.lib.DCGLibrary");
		String goal = "catch(phrase(X, [], []), error(instantiation_error, instantiation_error(Goal, ArgNo)), true).";
		Solution info = engine.solve(goal);
		Assert.assertTrue(info.isSuccess());
		Struct g = (Struct) info.getTerm("Goal");
		Assert.assertTrue(g.isEqual(new Struct("phrase_guard", new Var("X"),
				new Struct(), new Struct())));
		Int argNo = (Int) info.getTerm("ArgNo");
        assertEquals(1, argNo.intValue());
	}

}