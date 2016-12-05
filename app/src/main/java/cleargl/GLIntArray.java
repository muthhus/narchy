package cleargl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

public class GLIntArray {

	private final int mElementSize;
	private IntBuffer mIntBuffer;

	public GLIntArray(final int pNumberOfElements, final int pElementSize) {
		super();
		mElementSize = pElementSize;
		mIntBuffer = allocate(pNumberOfElements);
	}

	private IntBuffer allocate(final int pNewCapacity) {
		return ByteBuffer.allocateDirect(pNewCapacity * mElementSize
				* (Integer.SIZE / Byte.SIZE))
				.order(ByteOrder.nativeOrder())
				.asIntBuffer();
	}

	public void add(final int... pElementInts) {
		if (mIntBuffer.remaining() < pElementInts.length) {
			final int lNewCapacity = (int) 1.5 * mIntBuffer.capacity();
			final IntBuffer lNewBuffer = allocate(lNewCapacity);
			lNewBuffer.put(mIntBuffer);
			mIntBuffer = lNewBuffer;
		}

		mIntBuffer.put(pElementInts);
	}

	public void clear() {
		mIntBuffer.clear();
	}

	public void fillZeros() {
		mIntBuffer.rewind();
		padZeros();
	}

	public void padZeros() {
		while (mIntBuffer.hasRemaining())
			mIntBuffer.put(0);
	}

	public void rewind() {
		mIntBuffer.rewind();
	}

	public void flip() {
		mIntBuffer.flip();
	}

	public IntBuffer getIntBuffer() {
		rewind();
		return mIntBuffer;
	}

	public int getNumberOfElements() {
		return mIntBuffer.limit();
	}

	public int getElementSize() {
		return mElementSize;
	}

}
