package nars.util;

import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.Animator;


public abstract class AbstractJoglWindow extends GLWindow implements GLEventListener, WindowListener {

    //public static final int SCREEN_DRAG_BUTTON = 3;

    public static final int INIT_WIDTH = 1200;
    public static final int INIT_HEIGHT = 900;

    //private Timer timer;
    //LightEngine light = new LightEngine();


    public AbstractJoglWindow() {
        this(newDefaultConfig());
    }

    // model can be null
    // if it is null world and debugDraw can be null, because they are retrived from model
    public AbstractJoglWindow(GLCapabilitiesImmutable config) {
        //super(GLWindow.create(config));
        super(NewtFactory.createWindow(config));

        addGLEventListener(this);
        addWindowListener(this);

        Animator a = new Animator();
        a.add(this);
        a.start();

        setSize(INIT_WIDTH, INIT_HEIGHT);
        setVisible(true);




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



    public AbstractJoglWindow show(int w, int h) {
        return show("", w, h );
    }

    public AbstractJoglWindow show(String title, int w, int h) {
        setTitle(title);
        setVisible(true);
        setSurfaceSize(w, h);
        return this;
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
