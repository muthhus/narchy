package cleargl;

import java.nio.Buffer;
import java.util.Arrays;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GLException;

public class GLPixelBufferObject implements GLInterface, GLCloseable {
	private final GLInterface mGLInterface;
	private int[] mPixelBufferObjectId = new int[1];
	private final int mTextureWidth;
	private final int mTextureHeight;

	public GLPixelBufferObject(final GLInterface pGLInterface,
			final int pWidth,
			final int pHeight) {
		super();
		mGLInterface = pGLInterface;
		mTextureWidth = pWidth;
		mTextureHeight = pHeight;

		mGLInterface.getGL().glGenBuffers(1, mPixelBufferObjectId, 0);

	}

	public void bind() {
		mGLInterface.getGL().glBindBuffer(GL2ES3.GL_PIXEL_UNPACK_BUFFER,
				getId());
	}

	public void unbind() {
		mGLInterface.getGL().glBindBuffer(GL2ES3.GL_PIXEL_UNPACK_BUFFER,
				0);
	}

	public void copyFrom(final Buffer pBuffer) {
		bind();
		mGLInterface.getGL().glBufferData(GL2ES3.GL_PIXEL_UNPACK_BUFFER,
				mTextureWidth * mTextureHeight
						* 1
						* 4,
				null,
				GL.GL_DYNAMIC_DRAW);
		unbind();
	}

	@Override
	public void close() throws GLException {
		mGLInterface.getGL().glDeleteBuffers(1, mPixelBufferObjectId, 0);
		mPixelBufferObjectId = null;
	}

	@Override
	public GL getGL() {
		return mGLInterface.getGL();
	}

	@Override
	public int getId() {
		return mPixelBufferObjectId[0];
	}

	@Override
	public String toString() {
		return "GLPixelBufferObject [mGLInterface=" + mGLInterface
				+ ", mPixelBufferObjectId="
				+ Arrays.toString(mPixelBufferObjectId)
				+ ", mTextureWidth="
				+ mTextureWidth
				+ ", mTextureHeight="
				+ mTextureHeight
				+ "]";
	}

}
