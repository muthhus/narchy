package mcaixictw;

import static mcaixictw.Util.asInt;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import com.gs.collections.impl.list.mutable.primitive.BooleanArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


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
	public final void testDecode2() {
		List<Boolean> list = new ArrayList<>();
		list.add(true);
		list.add(true);
		list.add(false);
		list.add(true);
		list.add(true);
		list.add(false);
		BooleanArrayList bs = Util.asBitSet(list);
		assertEquals(list.toString() + " " + bs.toString(),27, asInt(bs));
	}

	@Test
	public final void testDecode() {
		List<Boolean> list = new ArrayList<>();
		list.add(true);
		list.add(true);
		list.add(false);
		list.add(true);
		BooleanArrayList bs = Util.asBitSet(list);
		assertEquals(list.toString() + " " + bs.toString(), 11, asInt(bs));
	}

	@Test
	public final void testEncode() {
		BooleanArrayList list = Util.encode(11, 4);
		//System.out.println(list.toString());
		assertTrue(list.get(0));
		assertTrue(list.get(1));
		assertFalse(list.get(2));
		assertTrue(list.get(3));
	}

}
