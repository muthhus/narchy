package alice.tuprolog;

import junit.framework.TestCase;
import org.junit.Assert;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 
 * @author <a href="mailto:giulio.piancastelli@unibo.it">Giulio Piancastelli</a>
 */
public class TermIteratorTestCase extends TestCase {
	
	public void testEmptyIterator() {
		String theory = "";
		Iterator<Term> i = Term.getIterator(theory);
		Assert.assertFalse(i.hasNext());
		try {
			i.next();
			Assert.fail();
		} catch (NoSuchElementException expected) {}
	}
	
	public void testIterationCount() {
		String theory = "q(1)." + "\n" +
		                "q(2)." + "\n" +
		                "q(3)." + "\n" +
		                "q(5)." + "\n" +
		                "q(7).";
		Iterator<Term> i = Term.getIterator(theory);
		int count = 0;
		for (; i.hasNext(); count++)
			i.next();
		Assert.assertEquals(5, count);
		Assert.assertFalse(i.hasNext());
	}
	
	public void testMultipleHasNext() {
		String theory = "p. q. r.";
		Iterator<Term> i = Term.getIterator(theory);
		Assert.assertTrue(i.hasNext());
		Assert.assertTrue(i.hasNext());
		Assert.assertTrue(i.hasNext());
		Assert.assertEquals(new Struct("p"), i.next());
	}
	
	public void testMultipleNext() {
		String theory = "p(X):-q(X),X>1." + "\n" +
		                "q(1)." + "\n" +
						"q(2)." + "\n" +
						"q(3)." + "\n" +
						"q(5)." + "\n" +
						"q(7).";
		Iterator<Term> i = Term.getIterator(theory);
		Assert.assertTrue(i.hasNext());
		i.next(); // skip the first term
		Assert.assertEquals(new Struct("q", new Int(1)), i.next());
		Assert.assertEquals(new Struct("q", new Int(2)), i.next());
		Assert.assertEquals(new Struct("q", new Int(3)), i.next());
		Assert.assertEquals(new Struct("q", new Int(5)), i.next());
		Assert.assertEquals(new Struct("q", new Int(7)), i.next());
		// no more terms
		Assert.assertFalse(i.hasNext());
		try {
			i.next();
			Assert.fail();
		} catch (NoSuchElementException expected) {}
	}
	
	public void testIteratorOnInvalidTerm() {
		String t = "q(1)"; // missing the End-Of-Clause!
		try {
			Term.getIterator(t);
			Assert.fail();
		} catch (InvalidTermException expected) {}
	}
	
	public void testIterationOnInvalidTheory() {
		String theory = "q(1)." + "\n" +
		                "q(2)." + "\n" +
						"q(3) " + "\n" + // missing the End-Of-Clause!
						"q(5)." + "\n" +
						"q(7).";
		Struct firstTerm = new Struct("q", new Int(1));
		Struct secondTerm = new Struct("q", new Int(2));
		Iterator<Term> i1 = Term.getIterator(theory);
		Assert.assertTrue(i1.hasNext());
		Assert.assertEquals(firstTerm, i1.next());
		Assert.assertTrue(i1.hasNext());
		Assert.assertEquals(secondTerm, i1.next());
		try {
			i1.hasNext();
			Assert.fail();
		} catch (InvalidTermException expected) {}
		Iterator<Term> i2 = Term.getIterator(theory);
		Assert.assertEquals(firstTerm, i2.next());
		Assert.assertEquals(secondTerm, i2.next());
		try {
			i2.next();
			Assert.fail();
		} catch (InvalidTermException expected) {}
	}
	
	public void testRemoveOperationNotSupported() {
		String theory = "p(1).";
		Iterator<Term> i = Term.getIterator(theory);
		Assert.assertNotNull(i.next());
		try {
			i.remove();
			Assert.fail();
		} catch (UnsupportedOperationException expected) {}
	}

}
