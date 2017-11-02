package jcog.bag.impl;

import jcog.Util;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArrayBag2Test {

    @Test
    public void testAddAccumulate() {
        ArrayBag2<String> s = new ArrayBag2(4);
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
    }
}