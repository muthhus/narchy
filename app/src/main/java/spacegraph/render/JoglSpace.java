package spacegraph.render;

import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.gl2.GLUT;


public abstract class JoglSpace implements GLEventListener, WindowListener {

    final static int DEFAULT_FPS = 30;

    public static final GLU glu = new GLU();
    public static final GLUT glut = new GLUT();

    public GLWindow window;
    protected GL2 gl;

    public static GLWindow window(JoglSpace j) {
        return window(newDefaultConfig(), j);
    }

    public static GLWindow window(GLCapabilitiesImmutable config, JoglSpace j) {
        GLWindow w = GLWindow.create(config);
        w.addGLEventListener(j);
        w.addWindowListener(j);

        //TODO FPSAnimator
        FPSAnimator a = new FPSAnimator(DEFAULT_FPS);
        a.add(w);
        a.start();
        return w;
    }

    @Override
    public final void init(GLAutoDrawable drawable) {
        this.window = (GLWindow)drawable;
        init(this.gl = drawable.getGL().getGL2());
    }


    protected void init(GL2 gl2) {

    }


    public void printHardware() {
        //System.err.print("GL Profile: ");
        //System.err.println(GLProfile.getProfile());
        System.err.print("GL:");
        System.err.println(gl);
        System.err.print("GL_VERSION=");
        System.err.println(gl.glGetString(gl.GL_VERSION));
        System.err.print("GL_EXTENSIONS: ");
        System.err.println(gl.glGetString(gl.GL_EXTENSIONS));
    }

    public synchronized static GLCapabilitiesImmutable newDefaultConfig() {

        GLCapabilities config = new GLCapabilities(
                //GLProfile.getMaximum(true)
                GLProfile.getDefault()
        );

        config.setHardwareAccelerated(true);
//        config.setBackgroundOpaque(false);

        config.setAlphaBits(8);
        config.setAccumAlphaBits(8);
        config.setAccumRedBits(8);
        config.setAccumGreenBits(8);
        config.setAccumBlueBits(8);
        return config;
    }


//    protected World2D getWorld() {
//        return model != null ? model.getCurrTest().getWorld() : world;
//    }


    public int getWidth() {
        return window.getWidth();
    }
    public int getHeight() {
        return window.getHeight();
    }



    @Override
    public void dispose(GLAutoDrawable arg0) {
    }

    @Override
    public void windowResized(WindowEvent windowEvent) {

    }

    @Override
    public void windowMoved(WindowEvent windowEvent) {

    }

    @Override
    public void windowDestroyNotify(WindowEvent windowEvent) {

    }

    @Override
    public void windowDestroyed(WindowEvent windowEvent) {

    }

    @Override
    public void windowGainedFocus(WindowEvent windowEvent) {

    }

    @Override
    public void windowLostFocus(WindowEvent windowEvent) {

    }

    @Override
    public void windowRepaint(WindowUpdateEvent windowUpdateEvent) {

    }



    public GLWindow show(int w, int h) {
        return show("", w, h );
    }

    public GLWindow show(String title, int w, int h) {
        GLWindow g = window(this);
        g.setTitle(title);
        g.setSurfaceSize(w, h);
        g.setVisible(true);

        return g;
    }

    public void addMouseListener(MouseListener m) {
        window.addMouseListener(m);
    }
    public void addWindowListener(WindowListener m) {
        window.addWindowListener(m);
    }
    public void addKeyListener(KeyListener m) {
        window.addKeyListener(m);
    }

    public GL2 gl() {
        return gl;
    }


//
//    public void reshape2D(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {
//        float width = getWidth();
//        float height = getHeight();
//
//        GL2 gl2 = arg0.getGL().getGL2();
//
//        gl2.glMatrixMode(GL_PROJECTION);
//        gl2.glLoadIdentity();
//
//        // coordinate system origin at lower left with width and height same as the window
//        GLU glu = new GLU();
//        glu.gluOrtho2D(0.0f, width, 0.0f, height);
//
//
//        gl2.glMatrixMode(GL_MODELVIEW);
//        gl2.glLoadIdentity();
//
//        gl2.glViewport(0, 0, getWidth(), getHeight());
//
//    }
}
