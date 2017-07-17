package alice.tuprolog;

import junit.framework.TestCase;
import org.junit.Assert;

public class VarTestCase extends TestCase {
	
	public void testIsAtomic() {
		Assert.assertFalse(new Var("X").isAtomic());
	}
	
	public void testIsAtom() {
		Assert.assertFalse(new Var("X").isAtom());
	}
	
	public void testIsCompound() {
		Assert.assertFalse(new Var("X").isCompound());
	}

}
