package nars.learn.gng;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by me on 5/25/16.
 */
public class MyShortIntHashMapTest {

    @Test
    public void testAddToValues() {
        MyShortIntHashMap m = new MyShortIntHashMap();
        for (int i = 0; i < 100; i++) {
            m.put((short) (Math.random()*1000), 1);
        }
        m.addToValues(4);
        m.forEachKeyValue((k,v) -> {
            assertEquals(5, v);
        });
    }

}