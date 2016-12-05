package cleargl;

import java.nio.FloatBuffer;
import java.util.Arrays;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLException;

public class GLVertexAttributeArray implements
		GLCloseable,
		GLInterface {

	private final GLAttribute mGLAttribute;
	private final int[] mVertexAttributeBuffersId;

	private final int mElementsPerIndex;

	public GLVertexAttributeArray(final GLAttribute pGLAttribute,
			final int pElementsPerIndex) {
		super();
		mGLAttribute = pGLAttribute;
		mElementsPerIndex = pElementsPerIndex;
		mVertexAttributeBuffersId = new int[3];
		mGLAttribute.getGL()
				.glGenBuffers(3, mVertexAttributeBuffersId, 0);
	}

	@Override
	public void close() throws GLException {
		mGLAttribute.getGL().glDeleteBuffers(2,
				mVertexAttributeBuffersId,
				0);
	}

	public void copyFrom(final FloatBuffer pFloatBuffer) {
		bind();
		getGL().glBufferData(GL.GL_ARRAY_BUFFER,
				pFloatBuffer.remaining() * (Float.SIZE / 8),
				pFloatBuffer,
				GL.GL_STATIC_DRAW);
	}

	public void bind() {
		mGLAttribute.getGL().glBindBuffer(GL.GL_ARRAY_BUFFER,
				mVertexAttributeBuffersId[0]);
	}

	public void unbind() {
		mGLAttribute.getGL().glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
	}

	@Override
	public GL getGL() {
		return mGLAttribute.getGL();
	}

	@Override
	public int getId() {
		return mVertexAttributeBuffersId[0];
	}

	public int getId(final int index) {
		return mVertexAttributeBuffersId[index];
	}

	public GLAttribute getAttribute() {
		return mGLAttribute;
	}

	public int getElementsPerIndex() {
		return mElementsPerIndex;
	}

	@Override
	public String toString() {
		return "GLVertexAttributeArray [mGLAttribute=" + mGLAttribute
				+ ", mVertexAttributeBuffersId="
				+ Arrays.toString(mVertexAttributeBuffersId)
				+ ", mElementsPerIndex="
				+ mElementsPerIndex
				+ "]";
	}

}
