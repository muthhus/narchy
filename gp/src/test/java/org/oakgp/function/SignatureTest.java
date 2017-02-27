/*
 * Copyright 2015 S. Webber
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.oakgp.function;

import org.junit.Test;
import org.oakgp.Type;

import java.util.List;

import static org.junit.Assert.*;
import static org.oakgp.TestUtils.assertUnmodifiable;
import static org.oakgp.Type.booleanType;
import static org.oakgp.Type.integerType;
import static org.oakgp.function.Signature.build;

public class SignatureTest {
    @Test
    public void testGetArgumentType() {
        Signature signature = build(integerType(), booleanType(), integerType(), booleanType());

        assertEquals(3, signature.size());
        assertSame(integerType(), signature.returnType());
        assertSame(booleanType(), signature.argType(0));
        assertSame(integerType(), signature.argType(1));
        assertSame(booleanType(), signature.argType(2));

        assertArrayIndexOutOfBoundsException(signature, -1);
        assertArrayIndexOutOfBoundsException(signature, 3);
    }

    private void assertArrayIndexOutOfBoundsException(Signature signature, int index) {
        try {
            signature.argType(index);
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testGetArgumemntTypes() {
        Signature signature = build(integerType(), booleanType(), integerType(), booleanType());
        List<Type> types = signature.argTypes();
        assertEquals(3, types.size());
        assertSame(booleanType(), types.get(0));
        assertSame(integerType(), types.get(1));
        assertSame(booleanType(), types.get(2));
        assertUnmodifiable(types);
    }

    @Test
    public void testToString() {
        Signature signature = build(integerType(), booleanType(), integerType(), booleanType());
        assertEquals("integer [boolean, integer, boolean]", signature.toString());
    }

    @Test
    public void testEquals() {
        Signature s1 = build(integerType(), booleanType(), integerType(), booleanType());
        Signature s2 = build(integerType(), booleanType(), integerType(), booleanType());
        assertEquals(s1.hashCode(), s2.hashCode());
        assertTrue(s1.equals(s1));
        assertTrue(s1.equals(s2));
        assertTrue(s2.equals(s1));
    }

    @Test
    public void testNotEquals() {
        Signature s1 = build(integerType(), booleanType(), integerType(), booleanType());
        Signature s2 = build(integerType(), integerType(), booleanType(), booleanType());
        Signature s3 = build(booleanType(), booleanType(), integerType(), booleanType());
        Signature s4 = build(integerType(), booleanType(), integerType(), booleanType(), integerType());
        Signature s5 = build(integerType(), booleanType(), integerType(), booleanType(), booleanType());

        assertTrue(s1.equals(s1));
        assertFalse(s1.equals(s2));
        assertFalse(s1.equals(s3));
        assertFalse(s1.equals(s4));
        assertFalse(s1.equals(s5));

        assertTrue(s2.equals(s2));
        assertFalse(s2.equals(s1));
        assertFalse(s2.equals(s3));
        assertFalse(s2.equals(s4));
        assertFalse(s2.equals(s5));

        assertTrue(s3.equals(s3));
        assertFalse(s3.equals(s1));
        assertFalse(s3.equals(s2));
        assertFalse(s3.equals(s4));
        assertFalse(s3.equals(s5));

        assertTrue(s4.equals(s4));
        assertFalse(s4.equals(s1));
        assertFalse(s4.equals(s2));
        assertFalse(s4.equals(s3));
        assertFalse(s4.equals(s5));

        assertTrue(s5.equals(s5));
        assertFalse(s5.equals(s1));
        assertFalse(s5.equals(s2));
        assertFalse(s5.equals(s3));
        assertFalse(s5.equals(s4));

        assertFalse(s1.equals("string"));
    }
}
