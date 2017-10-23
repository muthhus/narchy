package mcaixictw;

import org.junit.jupiter.api.*;


public class UtilTest {

	@BeforeAll
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	public static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	public void setUp() throws Exception {
	}

	@AfterEach
	public void tearDown() throws Exception {
	}

	@Test
	public final void randRange() {

		for (int i = 0; i < 1000; i++) {
			int range = 1 + (int) Math.round(Math.random() * 7);
			int r = Util.randRange(range);
			Assertions.assertTrue(r < range && r >= 0);
		}
	}

	@Test
	public final void testDecode() {
		BooleanArrayList list = new BooleanArrayList();
		list.add(true);
		list.add(true);
		list.add(false);
		list.add(true);
		Assertions.assertTrue(Util.decode(list) == 13);
	}

	@Test
	public final void testEncode() {
		BooleanArrayList list = Util.encode(13, 4);
		Assertions.assertTrue(list.get(0) == true);
		Assertions.assertTrue(list.get(1) == true);
		Assertions.assertTrue(list.get(2) == false);
		Assertions.assertTrue(list.get(3) == true);
	}

}
