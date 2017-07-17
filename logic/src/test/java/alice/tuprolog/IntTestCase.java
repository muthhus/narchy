package alice.tuprolog;

import junit.framework.TestCase;
import org.junit.Assert;

public class IntTestCase extends TestCase {
	
	public void testIsAtomic() {
		Assert.assertTrue(new Int(0).isAtomic());
	}
	
	public void testIsAtom() {
		Assert.assertFalse(new Int(0).isAtom());
	}
	
	public void testIsCompound() {
		Assert.assertFalse(new Int(0).isCompound());
	}
	
	public void testEqualsToStruct() {
		Struct s = new Struct();
		Int zero = new Int(0);
		Assert.assertFalse(zero.equals(s));
	}
	
	public void testEqualsToVar() throws InvalidTermException {
		Var x = new Var("X");
		Int one = new Int(1);
		Assert.assertFalse(one.equals(x));
	}
	
	public void testEqualsToInt() {
		Int zero = new Int(0);
		Int one = new Int(1);
		Assert.assertFalse(zero.equals(one));
		Int anotherZero = new Int(1-1);
		assertEquals(anotherZero, zero);
	}
	
	public void testEqualsToLong() {
		// TODO Test Int numbers for equality with Long numbers
	}
	
	public void testEqualsToDouble() {
		Int integerOne = new Int(1);
		alice.tuprolog.Double doubleOne = new alice.tuprolog.Double(1);
		Assert.assertFalse(integerOne.equals(doubleOne));
	}
	
	public void testEqualsToFloat() {
		// TODO Test Int numbers for equality with Float numbers
	}

}
