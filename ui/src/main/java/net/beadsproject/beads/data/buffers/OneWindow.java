/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data.buffers;

import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.BufferFactory;

import java.util.Arrays;

/**
 * Creates a {@link Buffer} filled with 1's.
 * 
 * @author ben
 *
 */
public class OneWindow extends BufferFactory {

	 /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#generateBuffer(int)
     */
    @Override
    public Buffer generateBuffer(int bufferSize) {
    	Buffer b = new Buffer(bufferSize);
    	Arrays.fill(b.buf, 1.f);
    	return b;
    }
    
    /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#getName()
     */
    @Override
    public String getName() {
    	return "Ones";
    }
}
