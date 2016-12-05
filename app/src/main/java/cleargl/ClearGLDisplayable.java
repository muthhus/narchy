package cleargl;

import java.awt.Component;
import java.io.PrintStream;
import com.jogamp.nativewindow.WindowClosingProtocol.WindowClosingMode;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.opengl.GLException;

public interface ClearGLDisplayable extends GLCloseable {

	@Override
	public abstract void close() throws GLException;

	public abstract void setWindowTitle(String pTitleString);

	public abstract void setVisible(boolean pIsVisible);

	public abstract void toggleFullScreen();

	// TODO remove
	public abstract void setPerspectiveProjectionMatrix(float fov,
                                                        float ratio,
                                                        float nearP,
                                                        float farP);

	// TODO remove
	public abstract void setOrthoProjectionMatrix(float left,
                                                  float right,
                                                  float bottom,
                                                  float top,
                                                  float zNear,
                                                  float zFar);

	// TODO remove
	public abstract void lookAt(float pPosX,
                                float pPosY,
                                float pPosZ,
                                float pLookAtX,
                                float pLookAtY,
                                float pLookAtZ,
                                float pUpX,
                                float pUpY,
                                float pUpZ);

	// TODO remove
	public abstract GLMatrix getProjectionMatrix();

	// TODO remove
	public abstract GLMatrix getViewMatrix();

	public abstract String getWindowTitle();

	public abstract void disableClose();

	public abstract boolean isFullscreen();

	public abstract void setFullscreen(boolean pFullScreen);

	// TODO remove
	public abstract void display();

	public abstract WindowClosingMode setDefaultCloseOperation(WindowClosingMode pWindowClosingMode);

	public abstract void setSize(int pWindowWidth, int pWindowHeight);

	public abstract int getHeight();

	public abstract int getWidth();

	public abstract boolean isVisible();

	public abstract void addMouseListener(MouseListener pMouseListener);

	public abstract void addKeyListener(KeyListener pKeyListener);

	public abstract void addWindowListener(WindowAdapter pWindowAdapter);

	public abstract void setUpdateFPSFrames(int pFramesPerSecond,
                                            PrintStream pPrintStream);

	public abstract float getLastFPS();

	// TODO remove
	public abstract float getAspectRatio();


	public abstract float[] getBounds();

}
