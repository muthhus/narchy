package cleargl;

import com.jogamp.opengl.GL;

public class GLAttribute implements GLInterface {
	private final GLProgram mGlProgram;
	private final int mAttributeIndex;

	public GLAttribute(final GLProgram pGlProgram, final int pAttributeId) {
		mGlProgram = pGlProgram;
		mAttributeIndex = pAttributeId;
	}

	@Override
	public GL getGL() {
		return mGlProgram.getGL();
	}

	@Override
	public int getId() {
		return mAttributeIndex;
	}

	public int getIndex() {
		return mAttributeIndex;
	}

	@Override
	public String toString() {
		return "GLAttribute [mGlProgram=" + mGlProgram
				+ ", mAttributeIndex="
				+ mAttributeIndex
				+ "]";
	}

}
