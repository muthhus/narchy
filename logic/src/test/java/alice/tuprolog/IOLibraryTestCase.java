package alice.tuprolog;

import alice.tuprolog.lib.IOLibrary;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.List;
import java.util.Map;

public class IOLibraryTestCase extends TestCase {
	
	public void testGetPrimitives() {
		Library library = new IOLibrary();
		Map<Integer, List<PrimitiveInfo>> primitives = library.getPrimitives();
		Assert.assertEquals(3, primitives.size());
		Assert.assertEquals(0, primitives.get(PrimitiveInfo.DIRECTIVE).size());
		Assert.assertTrue(primitives.get(PrimitiveInfo.PREDICATE).size() > 0);
		Assert.assertEquals(0, primitives.get(PrimitiveInfo.FUNCTOR).size());
	}
	
	public void testTab1() throws MalformedGoalException {
		Prolog engine = new Prolog();
		TestOutputListener l = new TestOutputListener();
		engine.addOutputListener(l);
		engine.solve("tab(5).");
		Assert.assertEquals("     ", l.output);
	}

}
