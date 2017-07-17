package alice.tuprolog;

import junit.framework.TestCase;
import org.junit.Assert;

public class DoubleTestCase extends TestCase {
	
	public void testIsAtomic() {
		Assert.assertTrue(new alice.tuprolog.Double(0).isAtomic());
	}
	
	public void testIsAtom() {
		Assert.assertFalse(new alice.tuprolog.Double(0).isAtom());
	}
	
	public void testIsCompound() {
		Assert.assertFalse(new alice.tuprolog.Double(0).isCompound());
	}
	
	public void testEqualsToStruct() {
		alice.tuprolog.Double zero = new alice.tuprolog.Double(0);
		Struct s = new Struct();
		Assert.assertFalse(zero.equals(s));
	}
	
	public void testEqualsToVar() throws InvalidTermException {
		alice.tuprolog.Double one = new alice.tuprolog.Double(1);
		Var x = new Var("X");
		Assert.assertFalse(one.equals(x));
	}
	
	public void testEqualsToDouble() {
		alice.tuprolog.Double zero = new alice.tuprolog.Double(0);
		alice.tuprolog.Double one = new alice.tuprolog.Double(1);
		Assert.assertFalse(zero.equals(one));
		alice.tuprolog.Double anotherZero = new alice.tuprolog.Double(0.0);
        assertEquals(anotherZero, zero);
	}
	
	public void testEqualsToFloat() {
		// TODO Test Double numbers for equality with Float numbers
	}
	
	public void testEqualsToInt() {
		alice.tuprolog.Double doubleOne = new alice.tuprolog.Double(1.0);
		Int integerOne = new Int(1);
		Assert.assertFalse(doubleOne.equals(integerOne));
	}
	
	public void testEqualsToLong() {
		// TODO Test Double numbers for equality with Long numbers
	}

}
