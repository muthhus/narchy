package cleargl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLPipelineFactory;

public abstract class ClearGLDefaultEventListener implements
		ClearGLEventListener {
	private boolean mDebugMode = false;
	private boolean mAlreadyInDebugMode = false;
	private long mNextFPSUpdate;

	@Override
	public void init(final GLAutoDrawable pDrawable) {
		getClearGLWindow().setUpdateFPSFrames(60, null);
		setDebugPipeline(pDrawable);
	}

	@Override
	public void dispose(final GLAutoDrawable pDrawable) {
		setDebugPipeline(pDrawable);

	}

	@Override
	public void display(final GLAutoDrawable pDrawable) {
		setDebugPipeline(pDrawable);
		/*
		 * if (System.nanoTime() > mNextFPSUpdate) { final String lWindowTitle =
		 * getClearGLWindow().getWindowTitle(); final float lLastFPS =
		 * getClearGLWindow().getLastFPS(); final String lTitleWithFPS =
		 * String.format( "%s (%.0f fps) ", lWindowTitle, lLastFPS);
		 * getClearGLWindow().setWindowTitle(lTitleWithFPS);
		 * 
		 * mNextFPSUpdate = System.nanoTime() + 1000 * 1000 * 1000; }/
		 **/

	}

	@Override
	public void reshape(final GLAutoDrawable pDrawable,
			final int pX,
			final int pY,
			final int pWidth,
			final int pHeight) {
		setDebugPipeline(pDrawable);

	}

	private void setDebugPipeline(final GLAutoDrawable pDrawable) {
		if (mAlreadyInDebugMode || !isDebugMode())
			return;

		final GL lGL = pDrawable.getGL();
		lGL.getContext()
				.setGL(GLPipelineFactory.create("com.jogamp.opengl.Debug",
						null,
						lGL,
						null));


		mAlreadyInDebugMode = true;
	}

	@Override
	public abstract void setClearGLWindow(ClearGLWindow pClearGLWindow);

	@Override
	public abstract ClearGLDisplayable getClearGLWindow();

	public boolean isDebugMode() {
		return mDebugMode;
	}

	public void setDebugMode(final boolean pDebugMode) {
		mDebugMode = pDebugMode;
	}

}
