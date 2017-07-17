package alice.tuprolog;

import junit.framework.TestCase;
import org.junit.Assert;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 
 * @author <a href="mailto:giulio.piancastelli@unibo.it">Giulio Piancastelli</a>
 */
public class StructIteratorTestCase extends TestCase {
	
	public void testEmptyIterator() {
		Struct list = new Struct();
		Iterator<? extends Term> i = list.listIterator();
		Assert.assertFalse(i.hasNext());
		try {
			i.next();
			Assert.fail();
		} catch (NoSuchElementException expected) {}
	}
	
	public void testIteratorCount() {
		Struct list = new Struct(new Term[] {new Int(1), new Int(2), new Int(3), new Int(5), new Int(7)});
		Iterator<? extends Term> i = list.listIterator();
		int count = 0;
		for (; i.hasNext(); count++)
			i.next();
		Assert.assertEquals(5, count);
		Assert.assertFalse(i.hasNext());
	}
	
	public void testMultipleHasNext() {
		Struct list = new Struct(new Term[] {new Struct("p"), new Struct("q"), new Struct("r")});
		Iterator<? extends Term> i = list.listIterator();
		Assert.assertTrue(i.hasNext());
		Assert.assertTrue(i.hasNext());
		Assert.assertTrue(i.hasNext());
		Assert.assertEquals(new Struct("p"), i.next());
	}
	
	public void testMultipleNext() {
		Struct list = new Struct(new Term[] {new Int(0), new Int(1), new Int(2), new Int(3), new Int(5), new Int(7)});
		Iterator<? extends Term> i = list.listIterator();
		Assert.assertTrue(i.hasNext());
		i.next(); // skip the first term
		Assert.assertEquals(new Int(1), i.next());
		Assert.assertEquals(new Int(2), i.next());
		Assert.assertEquals(new Int(3), i.next());
		Assert.assertEquals(new Int(5), i.next());
		Assert.assertEquals(new Int(7), i.next());
		// no more terms
		Assert.assertFalse(i.hasNext());
		try {
			i.next();
			Assert.fail();
		} catch (NoSuchElementException expected) {}
	}
	
	public void testRemoveOperationNotSupported() {
		Struct list = new Struct(new Int(1), new Struct());
		Iterator<? extends Term> i = list.listIterator();
		Assert.assertNotNull(i.next());
		try {
			i.remove();
			Assert.fail();
		} catch (UnsupportedOperationException expected) {}
	}

}
