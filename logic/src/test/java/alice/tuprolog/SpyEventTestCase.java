package alice.tuprolog;

import alice.tuprolog.event.SpyEvent;
import junit.framework.TestCase;
import org.junit.Assert;

public class SpyEventTestCase extends TestCase {
	
	public void testToString() {
		String msg = "testConstruction";
		SpyEvent e = new SpyEvent(new Prolog(), msg);
		Assert.assertEquals(msg, e.toString());
	}

}
