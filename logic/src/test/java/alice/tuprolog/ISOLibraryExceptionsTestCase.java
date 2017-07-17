package alice.tuprolog;

import junit.framework.TestCase;
import org.junit.Assert;

/**
 * @author Matteo Iuliani
 * 
 *         Test del funzionamento delle eccezioni lanciate dai predicati della ISOLibrary
 */
public class ISOLibraryExceptionsTestCase extends TestCase {

	// verifico che atom_length(X, Y) lancia un errore di instanziazione
	public void test_atom_length_2_1() throws Exception {
		Prolog engine = new Prolog();
		String goal = "catch(atom_length(X, Y), error(instantiation_error, instantiation_error(Goal, ArgNo)), true).";
		Solution info = engine.solve(goal);
		Assert.assertTrue(info.isSuccess());
		Struct g = (Struct) info.getTerm("Goal");
		Assert.assertTrue(g.isEqual(new Struct("atom_length", new Var("X"), new Var("Y"))));
		Int argNo = (Int) info.getTerm("ArgNo");
        assertEquals(1, argNo.intValue());
	}

	// verifico che atom_length(1, Y) lancia un errore di tipo
	public void test_atom_length_2_2() throws Exception {
		Prolog engine = new Prolog();
		String goal = "catch(atom_length(1, Y), error(type_error(ValidType, Culprit), type_error(Goal, ArgNo, ValidType, Culprit)), true).";
		Solution info = engine.solve(goal);
		Assert.assertTrue(info.isSuccess());
		Struct g = (Struct) info.getTerm("Goal");
		Assert.assertTrue(g.isEqual(new Struct("atom_length", new Int(1), new Var("Y"))));
		Int argNo = (Int) info.getTerm("ArgNo");
        assertEquals(1, argNo.intValue());
		Struct validType = (Struct) info.getTerm("ValidType");
		Assert.assertTrue(validType.isEqual(new Struct("atom")));
		Int culprit = (Int) info.getTerm("Culprit");
        assertEquals(1, culprit.intValue());
	}
	
	// verifico che atom_chars(1, X) lancia un errore di tipo
	public void test_atom_chars_2_1() throws Exception {
		Prolog engine = new Prolog();
		String goal = "catch(atom_chars(1, X), error(type_error(ValidType, Culprit), type_error(Goal, ArgNo, ValidType, Culprit)), true).";
		Solution info = engine.solve(goal);
		Assert.assertTrue(info.isSuccess());
		Struct g = (Struct) info.getTerm("Goal");
		Assert.assertTrue(g.isEqual(new Struct("atom_chars", new Int(1), new Var("X"))));
		Int argNo = (Int) info.getTerm("ArgNo");
        assertEquals(1, argNo.intValue());
		Struct validType = (Struct) info.getTerm("ValidType");
		Assert.assertTrue(validType.isEqual(new Struct("atom")));
		Int culprit = (Int) info.getTerm("Culprit");
        assertEquals(1, culprit.intValue());
	}
	
	// verifico che atom_chars(X, a) lancia un errore di tipo
	public void test_atom_chars_2_2() throws Exception {
		Prolog engine = new Prolog();
		String goal = "catch(atom_chars(X, a), error(type_error(ValidType, Culprit), type_error(Goal, ArgNo, ValidType, Culprit)), true).";
		Solution info = engine.solve(goal);
		Assert.assertTrue(info.isSuccess());
		Struct g = (Struct) info.getTerm("Goal");
		Assert.assertTrue(g.isEqual(new Struct("atom_chars", new Var("X"), new Struct("a"))));
		Int argNo = (Int) info.getTerm("ArgNo");
        assertEquals(2, argNo.intValue());
		Struct validType = (Struct) info.getTerm("ValidType");
		Assert.assertTrue(validType.isEqual(new Struct("list")));
		Struct culprit = (Struct) info.getTerm("Culprit");
		Assert.assertTrue(culprit.isEqual(new Struct("a")));
	}
	
	// verifico che char_code(ab, X) lancia un errore di tipo
	public void test_char_code_2_1() throws Exception {
		Prolog engine = new Prolog();
		String goal = "catch(char_code(ab, X), error(type_error(ValidType, Culprit), type_error(Goal, ArgNo, ValidType, Culprit)), true).";
		Solution info = engine.solve(goal);
		Assert.assertTrue(info.isSuccess());
		Struct g = (Struct) info.getTerm("Goal");
		Assert.assertTrue(g.isEqual(new Struct("char_code", new Struct("ab"), new Var("X"))));
		Int argNo = (Int) info.getTerm("ArgNo");
        assertEquals(1, argNo.intValue());
		Struct validType = (Struct) info.getTerm("ValidType");
		Assert.assertTrue(validType.isEqual(new Struct("character")));
		Struct culprit = (Struct) info.getTerm("Culprit");
		Assert.assertTrue(culprit.isEqual(new Struct("ab")));
	}
	
	// verifico che char_code(X, a) lancia un errore di tipo
	public void test_char_code_2_2() throws Exception {
		Prolog engine = new Prolog();
		String goal = "catch(char_code(X, a), error(type_error(ValidType, Culprit), type_error(Goal, ArgNo, ValidType, Culprit)), true).";
		Solution info = engine.solve(goal);
		Assert.assertTrue(info.isSuccess());
		Struct g = (Struct) info.getTerm("Goal");
		Assert.assertTrue(g.isEqual(new Struct("char_code", new Var("X"), new Struct("a"))));
		Int argNo = (Int) info.getTerm("ArgNo");
        assertEquals(2, argNo.intValue());
		Struct validType = (Struct) info.getTerm("ValidType");
		Assert.assertTrue(validType.isEqual(new Struct("integer")));
		Struct culprit = (Struct) info.getTerm("Culprit");
		Assert.assertTrue(culprit.isEqual(new Struct("a")));
	}
	
	// verifico che sub_atom(1, B, C, D, E) lancia un errore di tipo
	public void test_sub_atom_5_2() throws Exception {
		Prolog engine = new Prolog();
		String goal = "catch(sub_atom(1, B, C, D, E), error(type_error(ValidType, Culprit), type_error(Goal, ArgNo, ValidType, Culprit)), true).";
		Solution info = engine.solve(goal);
		Assert.assertTrue(info.isSuccess());
		Struct g = (Struct) info.getTerm("Goal");
		Assert.assertTrue(g.isEqual(new Struct("sub_atom_guard", new Int(1), new Var("B"),  new Var("C"),  new Var("D"),  new Var("E"))));
		Int argNo = (Int) info.getTerm("ArgNo");
        assertEquals(1, argNo.intValue());
		Struct validType = (Struct) info.getTerm("ValidType");
		Assert.assertTrue(validType.isEqual(new Struct("atom")));
		Int culprit = (Int) info.getTerm("Culprit");
        assertEquals(1, culprit.intValue());
	}

}