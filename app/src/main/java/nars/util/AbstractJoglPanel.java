package nars.util;

import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.Animator;

/*******************************************************************************
 * Copyright (c) 2013, Daniel Murphy
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

/**
 *
 */
public abstract class AbstractJoglPanel extends GLWindow implements GLEventListener, WindowListener {

    //public static final int SCREEN_DRAG_BUTTON = 3;

    public static final int INIT_WIDTH = 1200;
    public static final int INIT_HEIGHT = 900;

    //private Timer timer;
    //LightEngine light = new LightEngine();


    public AbstractJoglPanel() {
        this(newDefaultConfig());
    }

    // model can be null
    // if it is null world and debugDraw can be null, because they are retrived from model
    public AbstractJoglPanel(GLCapabilitiesImmutable config) {
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


    @Deprecated  abstract protected void draw(GL2 gl, float dt);

    public AbstractJoglPanel show(int w, int h) {
        setSurfaceSize(w, h);
        setVisible(true);
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
