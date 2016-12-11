package cleargl;

import com.jogamp.opengl.GLEventListener;

public interface ClearGLEventListener extends GLEventListener {

	void setClearGLWindow(ClearGLWindow pClearGLWindow);

	ClearGLDisplayable getClearGLWindow();

}
