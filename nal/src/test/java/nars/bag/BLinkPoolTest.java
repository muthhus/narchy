package nars.bag;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by me on 5/19/16.
 */
public class BLinkPoolTest {

    @Test
    public void test1() {
        int c = 4;
        BLinkPool<String> p = new BLinkPool<>(c);
        assertEquals(p.b.length, c * 7);


        BLinkPool<String>.BLinkI b0 = p.get(0);
        BLinkPool<String>.BLinkI b1 = p.get(1);

        b0.init(1f, 0.5f, 0.25f);
        b1.init(0.25f, 0.5f, 1f);
        b0.commit();
        b1.commit();

        BLinkPool<String>.BLinkI b00 = p.get(0);
        assertEquals(b00.pri(), b0.pri(), 0.01f);
        BLinkPool<String>.BLinkI b11 = p.get(1);
        assertEquals(b11.pri(), b1.pri(), 0.01f);

        assertNotEquals(b00.pri(), b11.pri());

    }
}