package alice.tuprolog;

import junit.framework.TestCase;
import org.junit.Assert;

public class PrologTestCase extends TestCase {
	
	public void testEngineInitialization() {
		Prolog engine = new Prolog();
		//assertEquals(4, engine.getCurrentLibraries().length);
		Assert.assertNotNull(engine.library("alice.tuprolog.lib.BasicLibrary"));
		Assert.assertNotNull(engine.library("alice.tuprolog.lib.ISOLibrary"));
		Assert.assertNotNull(engine.library("alice.tuprolog.lib.IOLibrary"));
		Assert.assertNotNull(engine.library("alice.tuprolog.lib.OOLibrary"));
	}
	
	public void testLoadLibraryAsString() throws InvalidLibraryException {
		Prolog engine = new Prolog();
		engine.addLibrary("alice.tuprolog.StringLibrary");
		Assert.assertNotNull(engine.library("alice.tuprolog.StringLibrary"));
	}

//	@Ignore
//	public void testLoadLibraryAsObject() throws InvalidLibraryException {
//		Prolog engine = new Prolog();
//		Library stringLibrary = new StringLibrary();
//		engine.loadLibrary(stringLibrary);
//		assertNotNull(engine.getLibrary("alice.tuprolog.StringLibrary"));
//		Library javaLibrary = new alice.tuprolog.lib.OOLibrary();
//		engine.loadLibrary(javaLibrary);
//		assertSame(javaLibrary, engine.getLibrary("alice.tuprolog.lib.JavaLibrary"));
//	}
	
	public void testGetLibraryWithName() throws InvalidLibraryException {
		Prolog engine = new Prolog("alice.tuprolog.TestLibrary");
		Assert.assertNotNull(engine.library("TestLibraryName"));
	}
	
	public void testUnloadLibraryAfterLoadingTheory() throws Exception {
		Prolog engine = new Prolog();
		Assert.assertNotNull(engine.library("alice.tuprolog.lib.IOLibrary"));
		Theory t = new Theory("a(1).\na(2).\n");
		engine.setTheory(t);
		engine.removeLibrary("alice.tuprolog.lib.IOLibrary");
		Assert.assertNull(engine.library("alice.tuprolog.lib.IOLibrary"));
	}
	
	public void testAddTheory() throws InvalidTheoryException {
		Prolog engine = new Prolog();
		Theory t = new Theory("test :- notx existing(s).");
		try {
			engine.addTheory(t);
			Assert.fail();
		} catch (InvalidTheoryException expected) {
			Assert.assertEquals("", engine.getTheory().toString());
		}
	}
	
//	public void testSpyListenerManagement() {
//		Prolog engine = new Prolog();
//		SpyListener listener1 = new SpyListener() {
//			public void onSpy(SpyEvent e) {}
//		};
//		SpyListener listener2 = new SpyListener() {
//			public void onSpy(SpyEvent e) {}
//		};
//		engine.addSpyListener(listener1);
//		engine.addSpyListener(listener2);
//		assertEquals(2, engine.getSpyListenerList().size());
//	}
	
	public void testLibraryListener() throws InvalidLibraryException {
		Prolog engine = new Prolog(new String[]{});
		engine.addLibrary("alice.tuprolog.lib.BasicLibrary");
		engine.addLibrary("alice.tuprolog.lib.IOLibrary");
		TestPrologEventAdapter a = new TestPrologEventAdapter();
		engine.addLibraryListener(a);
		engine.addLibrary("alice.tuprolog.lib.JavaLibrary");
		Assert.assertEquals("alice.tuprolog.lib.JavaLibrary", a.firstMessage);
		engine.removeLibrary("alice.tuprolog.lib.JavaLibrary");
		Assert.assertEquals("alice.tuprolog.lib.JavaLibrary", a.firstMessage);
	}
	
	public void testTheoryListener() throws InvalidTheoryException {
		Prolog engine = new Prolog();
		TestPrologEventAdapter a = new TestPrologEventAdapter();
		engine.addTheoryListener(a);
		Theory t = new Theory("a(1).\na(2).\n");
		engine.setTheory(t);
		Assert.assertEquals("", a.firstMessage);
		Assert.assertEquals("a(1).\n\na(2).\n\n", a.secondMessage);
		t = new Theory("a(3).\na(4).\n");
		engine.addTheory(t);
		Assert.assertEquals("a(1).\n\na(2).\n\n", a.firstMessage);
		Assert.assertEquals("a(1).\n\na(2).\n\na(3).\n\na(4).\n\n", a.secondMessage);
	}
	
	public void testQueryListener() throws Exception {
		Prolog engine = new Prolog();
		TestPrologEventAdapter a = new TestPrologEventAdapter();
		engine.addQueryListener(a);
		engine.setTheory(new Theory("a(1).\na(2).\n"));
		engine.solve("a(X).");
		Assert.assertEquals("a(X)", a.firstMessage);
		Assert.assertEquals("yes.\nX / 1", a.secondMessage);
		engine.solveNext();
		Assert.assertEquals("a(X)", a.firstMessage);
		Assert.assertEquals("yes.\nX / 2", a.secondMessage);
	}

}
