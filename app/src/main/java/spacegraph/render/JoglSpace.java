package spacegraph.render;

import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.gl2.GLUT;
import nars.util.Util;
import org.infinispan.commons.util.concurrent.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Set;


public abstract class JoglSpace implements GLEventListener, WindowListener {

    final static int DEFAULT_FPS = 25;
    public static final int MIN_FPS = 3;
    private static final MyFPSAnimator a = new MyFPSAnimator(JoglSpace.DEFAULT_FPS, MIN_FPS, 50);

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
        animate(w);

        return w;
    }

    public static final Set<GLWindow> windows = new ConcurrentHashSet<>();

    public static final Logger logger = LoggerFactory.getLogger(JoglSpace.class);

    private static void animate(GLWindow w) {
        synchronized (a) {
            boolean wasEmpty = windows.isEmpty();

            if (!windows.add(w))
                return;

            if (wasEmpty) {
                if (!a.isStarted()) {
                    a.start(Thread.MIN_PRIORITY);
                    logger.info("START {}", a);
                } else {
                    a.resume();
                    logger.info("RESUME {}", a);
                }
            }

            a.add(w);
        }

        w.addWindowListener(new WindowAdapter() {

            @Override
            public void windowDestroyed(WindowEvent e) {
                windows.remove(w);

                boolean nowEmpty = windows.isEmpty();
                synchronized (a) {
                    a.remove(w);
                    if (nowEmpty) {
                        a.pause();
                        logger.info("PAUSE {}", a);
                    }
                }

                super.windowDestroyed(e);
            }
        });
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
                GLProfile.getMinimum(true)
                //GLProfile.getDefault()
                //GLProfile.getMaximum(true)

        );

//        config.setBackgroundOpaque(false);
//        config.setTransparentRedValue(-1);
//        config.setTransparentGreenValue(-1);
//        config.setTransparentBlueValue(-1);
//        config.setTransparentAlphaValue(-1);


        config.setHardwareAccelerated(true);


//        config.setAlphaBits(8);
//        config.setAccumAlphaBits(8);
//        config.setAccumRedBits(8);
//        config.setAccumGreenBits(8);
//        config.setAccumBlueBits(8);
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

    private static class MyFPSAnimator extends FPSAnimator {

        int idealFPS, minFPS;
        float lagTolerancePercentFPS = 0.07f;

        public MyFPSAnimator(int idealFPS, int minFPS, int updateEveryNFrames) {
            super(idealFPS);



            this.idealFPS = idealFPS;
            this.minFPS = minFPS;

            setUpdateFPSFrames(updateEveryNFrames, new PrintStream(new OutputStream() {

                @Override
                public void write(int b) throws IOException {
                }

                long lastUpdate = 0;
                @Override
                public void flush() throws IOException {
                    long l = getLastFPSUpdateTime();
                    if (lastUpdate==l)
                        return;
                    updateFPS();
                    lastUpdate = l;
                }

            }, true));

        }


        protected synchronized void updateFPS() {
            //logger.info("{}", MyFPSAnimator.this);

            int currentFPS = getFPS();
            float lag = currentFPS - getLastFPS();
            if (lag < 1f)
                return; //the fps can only be adjusted in integers

            float error = lag/currentFPS;

            float nextFPS = Float.NaN;

            if (error > lagTolerancePercentFPS) {
                if (currentFPS > minFPS)  {
                    //decrease fps
                    nextFPS = Util.lerp(minFPS, currentFPS, 0.25f);
                }
            } else {
                if (currentFPS < idealFPS) {
                    //increase fps
                    nextFPS = Util.lerp(idealFPS, currentFPS, 0.25f);
                }
            }

            int inextFPS = Math.round(nextFPS);
            if (nextFPS==nextFPS && inextFPS!=currentFPS) {
                //stop();
                logger.warn("animator rate change from {} to {} fps", currentFPS, inextFPS);

                Thread x = animThread; //HACK to make it think it's stopped when we just want to change the FPS value ffs!
                animThread = null;

                setFPS(inextFPS);
                animThread = x;

                //start();
            }

        }

        public synchronized void start(int threadPriority) {
            start();
            animThread.setPriority(threadPriority);
        }
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
