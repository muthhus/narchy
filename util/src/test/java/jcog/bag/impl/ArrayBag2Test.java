package jcog.bag.impl;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArrayBag2Test {

    @Test
    public void testAddAccumulate() {
        InstrumentedArrayBag2<String> s = new InstrumentedArrayBag2();

        s.put("a", 0.1f);
        s.put("b", 0.2f);
        s.put("c", 0.3f);
        s.put("d", 0.4f);
        assertEquals(4, s.size());
        assertEquals("a", s.lowest());
        assertEquals("d", s.highest());

        s.put("a", 0.2f);
        assertEquals(4, s.size());
        assertEquals(0.3f, s.get("a"), 0.001f);
        assertEquals("b", s.lowest());

        s.put("e", 0.5f);
        assertEquals("e", s.highest());
        assertEquals("a", s.lowest());
        assertTrue(!s.contains("b"));
        assertEquals(4, s.size());
        assertEquals(1, s.evictions);

        boolean addedF = s.put("f", 0.01f); //no change, rejected
        assertFalse(addedF);
        assertEquals(4, s.size());
        assertEquals("e", s.highest());
        assertEquals("a", s.lowest());

        int sortsBeforeaddedAzero = s.sorts;
        boolean addedAzero = s.put("a", 0f); //no change
        assertTrue(addedAzero);
        assertEquals(4, s.size());
        assertEquals("e", s.highest());
        assertEquals("a", s.lowest());
        assertEquals(sortsBeforeaddedAzero, s.sorts);

        s.clear();
        assertEquals(0, s.size());
        assertEquals(null, s.highest());
        assertEquals(null, s.lowest());
    }

    static class InstrumentedArrayBag2<X> extends ArrayBag2<X> {
        public int evictions = 0;
        public int insertions = 0;
        public int rejections = 0;
        public int sorts = 0;

        public InstrumentedArrayBag2() {
            super(4);
        }

        @Override
        public boolean put(X o, float pri) {
            if (!super.put(o, pri)) {
                rejections++;
                return false;
            }
            return true;
        }

        @Override
        public void onAdded(X o) {
            super.onAdded(o);
            insertions++;
        }

        @Override
        public void onRemoved(X o) {
            super.onRemoved(o);
            evictions++;
        }

        @Override
        protected List<X> sort(short from, short to) {
            List<X> x = super.sort(from, to);
            sorts++;
            return x;
        }
    }
}