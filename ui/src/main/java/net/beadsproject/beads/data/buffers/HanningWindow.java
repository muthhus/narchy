/*
 * This file is part of Beads. See http://www.beadsproject.net for all information.
 */
package net.beadsproject.beads.data.buffers;

import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.data.BufferFactory;

/**
 * Creates a {@link Buffer} of a Hanning window.
 * 
 * @see Buffer BufferFactory
 * @author ollie
 */
public class HanningWindow extends BufferFactory {

    /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#generateBuffer(int)
     */
    @Override
    public Buffer generateBuffer(int bufferSize) {
    	Buffer b = new Buffer(bufferSize);
    	int lowerThresh = bufferSize / 4;
    	int upperThresh = bufferSize - lowerThresh;
    	for(int i = 0; i < bufferSize; i++) {
            b.buf[i] = i < lowerThresh || i > upperThresh ? 0.5f * (1.0f + (float) Math.cos((Math.PI + Math.PI * 4.0f * (float) i / (float) (bufferSize - 1)))) : 1.0f;
    	}
    	return b;
    }
    
    /* (non-Javadoc)
     * @see net.beadsproject.beads.data.BufferFactory#getName()
     */
    @Override
    public String getName() {
    	return "Hanning";
    }


    
}
