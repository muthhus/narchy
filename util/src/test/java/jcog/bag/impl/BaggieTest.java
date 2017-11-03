package jcog.bag.impl;

import com.google.common.base.Joiner;
import jcog.random.XorShift128PlusRandom;
import org.apache.commons.math3.stat.Frequency;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class BaggieTest {

    @Test
    public void testAddAccumulate() {
        InstrumentedBaggie<String> s = new InstrumentedBaggie();

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

    static class InstrumentedBaggie<X> extends Baggie<X> {
        public int evictions = 0;
        public int insertions = 0;
        public int rejections = 0;
        public int sorts = 0;

        public InstrumentedBaggie() {
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
        protected List<X> update(short from, short to) {
            List<X> x = super.update(from, to);
            sorts++;
            return x;
        }
    }

    @Test
    public void testSampling() {
        int cap = 8;
        Baggie<String> s = new Baggie(cap);
        for (int i = 0; i < cap; i++) {
            s.put("x" + i, i / ((float) cap));
        }
        String t = Joiner.on(",").join(s.toList());
        assertEquals("x7=0.8749924,x6=0.75001526,x5=0.6250076,x4=0.5,x3=0.37499237,x2=0.25001526,x1=0.12500763,x0=0.0", t);

        Random rng = new XorShift128PlusRandom(1);
        Frequency f = new Frequency();
        final int[] num = {cap * 100};
        s.sample(rng, (x) -> {
            f.addValue(x.getOne());
            return num[0]-- > 0;
        });
        System.out.println(f);
        assertTrue(f.getCount("x" + (cap - 1)) > f.getCount("x0"));


        {
            //change all to a new value (0.5)
            final int[] resetNum = {cap * 100};

            //TODO use s.commit()
            s.sample(rng, (x) -> {
                x.set(0.5f);
                return resetNum[0]-- > 0;
            });
            s.forEach((x, p) -> {
                assertEquals(0.5f, p, 0.001f);
                return true;
            });
            assertEquals(cap, s.size()); //effectively cleared
            assertEquals(0.5f, s.priMax(), 0.001f);
            assertEquals(0.5f, s.priMin(), 0.001f);
        }

        {
            //remove everything
            final int[] remNum = {0};
            s.sample(rng, (x) -> {
                x.set(Float.NaN); //delete it
                remNum[0]++;
                return true;
            });
            assertEquals(cap, remNum[0]); //all removed during sampling
            assertEquals(0, s.size()); //effectively cleared
            assertEquals(Float.NaN, s.priMax(), 0.001f);
            assertEquals(Float.NaN, s.priMin(), 0.001f);
        }
    }

}