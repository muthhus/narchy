package mcaixictw;

import org.junit.*;

import static org.junit.Assert.assertTrue;


public class UtilTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public final void randRange() {

		for (int i = 0; i < 1000; i++) {
			int range = 1 + (int) Math.round(Math.random() * 7);
			int r = Util.randRange(range);
			assertTrue(r < range && r >= 0);
		}
	}

	@Test
	public final void testDecode() {
		BooleanArrayList list = new BooleanArrayList();
		list.add(true);
		list.add(true);
		list.add(false);
		list.add(true);
		assertTrue(Util.decode(list) == 13);
	}

	@Test
	public final void testEncode() {
		BooleanArrayList list = Util.encode(13, 4);
		assertTrue(list.get(0) == true);
		assertTrue(list.get(1) == true);
		assertTrue(list.get(2) == false);
		assertTrue(list.get(3) == true);
	}

}
