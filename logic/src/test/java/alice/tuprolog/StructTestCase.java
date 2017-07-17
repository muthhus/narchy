package alice.tuprolog;

import junit.framework.TestCase;
import org.junit.Assert;

public class StructTestCase extends TestCase {
	
	public void testStructWithNullArgument() {
		try {
			new Struct("p", (Term) null);
			Assert.fail();
		} catch (InvalidTermException expected) {}
		try {
			new Struct("p", new Int(1), null);
			Assert.fail();
		} catch (InvalidTermException expected) {}
		try {
			new Struct("p", new Int(1), new Int(2), null);
			Assert.fail();
		} catch (InvalidTermException expected) {}
		try {
			new Struct("p", new Int(1), new Int(2), new Int(3), null);
			Assert.fail();
		} catch (InvalidTermException expected) {}
		try {
			new Struct("p", new Int(1), new Int(2), new Int(3), new Int(4), null);
			Assert.fail();
		} catch (InvalidTermException expected) {}
		try {
			new Struct("p", new Int(1), new Int(2), new Int(3), new Int(4), new Int(5), null);
			Assert.fail();
		} catch (InvalidTermException expected) {}
		try {
			new Struct("p", new Int(1), new Int(2), new Int(3), new Int(4), new Int(5), new Int(6), null);
			Assert.fail();
		} catch (InvalidTermException expected) {}
		try {
			Term[] args = {new Struct("a"), null, new Var("P")};
			new Struct("p", args);
			Assert.fail();
		} catch (InvalidTermException expected) {}
	}
	
	public void testStructWithNullName() {
		try {
			new Struct(null, new Int(1), new Int(2));
			Assert.fail();
		} catch (InvalidTermException expected) {}
	}
	
	/** Structs with an empty name can only be atoms. */
	public void testStructWithEmptyName() {
		try {
			new Struct("", new Int(1), new Int(2));
			Assert.fail();
		} catch (InvalidTermException expected) {}
		Assert.assertEquals(0, new Struct("").name().length());
	}
	
	public void testEmptyList() {
		Struct list = new Struct();
		Assert.assertTrue(list.isList());
		Assert.assertTrue(list.isEmptyList());
		Assert.assertEquals(0, list.listSize());
		Assert.assertEquals("[]", list.name());
		Assert.assertEquals(0, list.getArity());
	}

	/** Another correct method of building an empty list */
	public void testEmptyListAsSquaredStruct() {
		Struct emptyList = new Struct("[]");
		Assert.assertTrue(emptyList.isList());
		Assert.assertTrue(emptyList.isEmptyList());
		Assert.assertEquals("[]", emptyList.name());
		Assert.assertEquals(0, emptyList.getArity());
		Assert.assertEquals(0, emptyList.listSize());
	}
	
	/** A wrong method of building an empty list */
	public void testEmptyListAsDottedStruct() {
		Struct notAnEmptyList = new Struct(".");
		Assert.assertFalse(notAnEmptyList.isList());
		Assert.assertFalse(notAnEmptyList.isEmptyList());
		Assert.assertEquals(".", notAnEmptyList.name());
		Assert.assertEquals(0, notAnEmptyList.getArity());
	}
	
	/** Use dotted structs to build lists with content */
	public void testListAsDottedStruct() {
		Struct notAnEmptyList = new Struct(".", new Struct("a"), new Struct(".", new Struct("b"), new Struct()));
		Assert.assertTrue(notAnEmptyList.isList());
		Assert.assertFalse(notAnEmptyList.isEmptyList());
		Assert.assertEquals(".", notAnEmptyList.name());
		Assert.assertEquals(2, notAnEmptyList.getArity());
	}
	
	public void testListFromArgumentArray() {
		Assert.assertEquals(new Struct(), new Struct(new Term[0]));
		
		Term[] args = new Term[2];
		args[0] = new Struct("a");
		args[1] = new Struct("b");
		Struct list = new Struct(args);
		Assert.assertEquals(new Struct(), list.listTail().listTail());
	}
	
	public void testListSize() {
		Struct list = new Struct(new Struct("a"),
				       new Struct(new Struct("b"),
				           new Struct(new Struct("c"), new Struct())));
		Assert.assertTrue(list.isList());
		Assert.assertFalse(list.isEmptyList());
		Assert.assertEquals(3, list.listSize());
	}
	
	public void testNonListHead() throws InvalidTermException {
		Struct s = new Struct("f", new Var("X"));
		try {
			Assert.assertNotNull(s.listHead()); // just to make an assertion...
			Assert.fail();
		} catch (UnsupportedOperationException e) {
			Assert.assertEquals("The structure " + s + " is not a list.", e.getMessage());
		}
	}
	
	public void testNonListTail() {
		Struct s = new Struct("h", new Int(1));
		try {
			Assert.assertNotNull(s.listTail()); // just to make an assertion...
			Assert.fail();
		} catch (UnsupportedOperationException e) {
			Assert.assertEquals("The structure " + s + " is not a list.", e.getMessage());
		}
	}
	
	public void testNonListSize() throws InvalidTermException {
		Struct s = new Struct("f", new Var("X"));
		try {
			Assert.assertEquals(0, s.listSize()); // just to make an assertion...
			Assert.fail();
		} catch (UnsupportedOperationException e) {
			Assert.assertEquals("The structure " + s + " is not a list.", e.getMessage());
		}
	}
	
	public void testNonListIterator() {
		Struct s = new Struct("f", new Int(2));
		try {
			Assert.assertNotNull(s.listIterator()); // just to make an assertion...
			Assert.fail();
		} catch (UnsupportedOperationException e) {
			Assert.assertEquals("The structure " + s + " is not a list.", e.getMessage());
		}
	}
	
	public void testToList() {
		Struct emptyList = new Struct();
		Struct emptyListToList = new Struct(new Struct("[]"), new Struct());
		Assert.assertEquals(emptyListToList, emptyList.toList());
	}
	
	public void testToString() throws InvalidTermException {
		Struct emptyList = new Struct();
		Assert.assertEquals("[]", emptyList.toString());
		Struct s = new Struct("f", new Var("X"));
		Assert.assertEquals("f(X)", s.toString());
		Struct list = new Struct(new Struct("a"),
		          new Struct(new Struct("b"),
		        	  new Struct(new Struct("c"), new Struct())));
		Assert.assertEquals("[a,b,c]", list.toString());
	}
	
	public void testAppend() {
		Struct emptyList = new Struct();
		Struct list = new Struct(new Struct("a"),
				          new Struct(new Struct("b"),
				        	  new Struct(new Struct("c"), new Struct())));
		emptyList.append(new Struct("a"));
		emptyList.append(new Struct("b"));
		emptyList.append(new Struct("c"));
		Assert.assertEquals(list, emptyList);
		Struct tail = new Struct(new Struct("b"),
                          new Struct(new Struct("c"), new Struct()));
		Assert.assertEquals(tail, emptyList.listTail());
		
		emptyList = new Struct();
		emptyList.append(new Struct());
		Assert.assertEquals(new Struct(new Struct(), new Struct()), emptyList);
		
		Struct anotherList = new Struct(new Struct("d"),
				                 new Struct(new Struct("e"), new Struct()));
		list.append(anotherList);
		Assert.assertEquals(anotherList, list.listTail().listTail().listTail().listHead());
	}
	
	public void testIteratedGoalTerm() throws Exception {
		Var x = new Var("X");
		Struct foo = new Struct("foo", x);
		Struct term = new Struct("^", x, foo);
		Assert.assertEquals(foo, term.iteratedGoalTerm());
	}
	
	public void testIsList() {
		Struct notList = new Struct(".", new Struct("a"), new Struct("b"));
		Assert.assertFalse(notList.isList());
	}
	
	public void testIsAtomic() {
		Struct emptyList = new Struct();
		Assert.assertTrue(emptyList.isAtomic());
		Struct atom = new Struct("atom");
		Assert.assertTrue(atom.isAtomic());
		Struct list = new Struct(new Term[] {new Int(0), new Int(1)});
		Assert.assertFalse(list.isAtomic());
		Struct compound = new Struct("f", new Struct("a"), new Struct("b"));
		Assert.assertFalse(compound.isAtomic());
		Struct singleQuoted = new Struct("'atom'");
		Assert.assertTrue(singleQuoted.isAtomic());
		Struct doubleQuoted = new Struct("\"atom\"");
		Assert.assertTrue(doubleQuoted.isAtomic());
	}
	
	public void testIsAtom() {
		Struct emptyList = new Struct();
		Assert.assertTrue(emptyList.isAtom());
		Struct atom = new Struct("atom");
		Assert.assertTrue(atom.isAtom());
		Struct list = new Struct(new Term[] {new Int(0), new Int(1)});
		Assert.assertFalse(list.isAtom());
		Struct compound = new Struct("f", new Struct("a"), new Struct("b"));
		Assert.assertFalse(compound.isAtom());
		Struct singleQuoted = new Struct("'atom'");
		Assert.assertTrue(singleQuoted.isAtom());
		Struct doubleQuoted = new Struct("\"atom\"");
		Assert.assertTrue(doubleQuoted.isAtom());
	}
	
	public void testIsCompound() {
		Struct emptyList = new Struct();
		Assert.assertFalse(emptyList.isCompound());
		Struct atom = new Struct("atom");
		Assert.assertFalse(atom.isCompound());
		Struct list = new Struct(new Term[] {new Int(0), new Int(1)});
		Assert.assertTrue(list.isCompound());
		Struct compound = new Struct("f", new Struct("a"), new Struct("b"));
		Assert.assertTrue(compound.isCompound());
		Struct singleQuoted = new Struct("'atom'");
		Assert.assertFalse(singleQuoted.isCompound());
		Struct doubleQuoted = new Struct("\"atom\"");
		Assert.assertFalse(doubleQuoted.isCompound());
	}
	
	public void testEqualsToObject() {
		Struct s = new Struct("id");
		Assert.assertFalse(s.equals(new Object()));
	}

}
