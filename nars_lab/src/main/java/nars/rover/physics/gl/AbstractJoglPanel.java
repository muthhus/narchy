package nars.rover.physics.gl;

import automenta.spacegraph.Space2D;
import automenta.spacegraph.demo.spacegraph.DemoTextButton;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.sun.prism.impl.BufferUtil;
import nars.rover.physics.PhysicsController;
import nars.rover.physics.TestbedPanel;
import nars.rover.physics.TestbedState;
import nars.rover.physics.j2d.AWTPanelHelper;
import org.jbox2d.callbacks.DebugDraw;
import org.jbox2d.common.Timer;
import org.jbox2d.dynamics.World;

import java.awt.*;
import java.awt.event.*;
import java.nio.FloatBuffer;

import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static com.jogamp.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

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
public abstract class AbstractJoglPanel extends GLCanvas implements TestbedPanel, GLEventListener {
    private static final long serialVersionUID = 1L;

    //public static final int SCREEN_DRAG_BUTTON = 3;

    public static final int INIT_WIDTH = 600;
    public static final int INIT_HEIGHT = 600;

    public final World world;
    //private Timer timer;
    //LightEngine light = new LightEngine();

    public final TestbedState model;
    private Point center;



    // model can be null
    // if it is null world and debugDraw can be null, because they are retrived from model
    public AbstractJoglPanel(final World world, final PhysicsController controller, TestbedState model, GLCapabilitiesImmutable config) {
        super(config);
        setSize(INIT_WIDTH, INIT_HEIGHT);
        //(new Dimension(600, 600));
        //setAutoSwapBufferMode(true);
        addGLEventListener(this);
        enableInputMethods(true);

//        if (model != null && controller != null) {
//            //AWTPanelHelper.addHelpAndPanelListeners(this, model, controller, SCREEN_DRAG_BUTTON);
//            AWTPanelHelper.addHelpAndPanelListeners(this, model, controller, SCREEN_DRAG_BUTTON);
//        }

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                //setsSize(getWidth(), getHeight());
                //dbImage = null;
            }
        });

        this.world = world;

        this.model = model;
    }

    @Override
    public void grabFocus() {

    }

    @Override
    public boolean render() {
        return true;
    }

    @Override
    public void paintScreen() {
        display();

    }

    @Override
    public void display(GLAutoDrawable arg0) {
        repainter();
    }

    protected void repainter() {

        GL2 gl = getGL().getGL2();

        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);


        //getGL().getGL2().glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        getGL().glClearColor(0.0f, 0.0f, 0.0f, 0.8f);
        getGL().glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_STENCIL_BUFFER_BIT);


        //gl.glClearAccum(0, 0, 0, 1f);
        //gl.glClear(GL2.GL_COLOR_BUFFER_BIT);


        gl.glAccum(GL2.GL_RETURN, 0.9f); //adding the current frame to the buffer



        float time = 0.0f; // what does this?
        if (model != null) {
            time = model.model.getTime();
        }

        game.render(gl, time);


        //https://www.opengl.org/sdk/docs/man2/xhtml/glAccum.xml

        //light.render(gl, drawer.getViewportTranform());


        //gl.glAccum(GL2.GL_LOAD, 0.95f); //Drawing last frame, saved in buffer
        //gl.glAccum(GL2.GL_MULT, 0.95f ); //make current frame in buffer dim


        getGL().glFlush();


        repaint();


    }

//    protected World getWorld() {
//        return model != null ? model.getCurrTest().getWorld() : world;
//    }



    @Override
    public void dispose(GLAutoDrawable arg0) {
    }


    //TODO rename this as 'Camera'
    public class Game {

        private GL gl;
        private int dx, dy;
        private long lastTime;
        private Point mouseCenter = new Point();
        private float ford, strafe;

        public Game(GL gl) {
            this.gl = gl;
        }

        public void setMouseCenter(Point center) {
            if (center != null)
                this.mouseCenter.setLocation(center);
        }

        float linSpeed = 10f;
        float angleSpeed = 0.0001f;

        public void forward(float v) {
            ford = v;
        }

        public void strafe(float v) {
            strafe = v;
        }

        public void click() {

        }

        /*

 * If polled events are true, we will instruct the Player class to act upon

 * them.

 */

        private void pollEvents() {

 /*

 * Polled keyboard events are either true of false. However, we want to

 * send the Player class an amount to move. Sending through a static

 * amount like 0.5 eg would not be a good idea, because fast machines

 * would update the player's position 0.5 every loop and so to would

 * slow machines... the speed of the machine would affect the speed of

 * the player. So what we do is gab the amount from the nano timer.

 * This means that fast machines send out smaller amounts more often,

 * and slower machines bigger amounts less often. We also 'multiply' the

 * lastTime to nowTime by a suitable factor so we do not require

 * a further 'multiplication' later.

 */

            //long now=System.nanoTime();

            //float period=(float)((now-lastTime)*0.000005);
            float period = 1f;

            //lastTime=now;

            dx = MouseInfo.getPointerInfo().getLocation().x;

            dy = MouseInfo.getPointerInfo().getLocation().y;

            //System.out.println(x + " " + y + " " + z);

            float head = mouseCenter.x - dx;


 /*

 * At each loop the Player class creates the correct ModelViewMatrix that

defines

 * where the player should be, and in what direction they should be heading.

 * This matrix is later loaded into GL instead of the identity matrix.

 * The following methods activate the player to setup relevant parts of the

 * matrix.

 */

            if (head != 0) updateHeading(head);

            float pit = mouseCenter.y - dy;

            if (pit != 0) updatePitch(pit);
//
            if (ford != 0) updateForward(ford * (float) period);
            if (strafe != 0) updateStrafe(strafe * (float) period);

            matrix.put(0, cosc * cosb - sinc * sina * sinb);

            matrix.put(1, sinc * cosb + cosc * sina * sinb);

            matrix.put(2, -cosa * sinb);

            matrix.put(4, -sinc * cosa);

            matrix.put(5, cosc * cosa);

            matrix.put(6, sina);

            matrix.put(8, cosc * sinb + sinc * sina * cosb);

            matrix.put(9, sinc * sinb - cosc * sina * cosb);

            matrix.put(10, cosa * cosb);

            matrix.put(12, matrix.get(0) * x + matrix.get(4) * y + matrix.get(8) * z);

            matrix.put(13, matrix.get(1) * x + matrix.get(5) * y + matrix.get(9) * z);

            matrix.put(14, matrix.get(2) * x + matrix.get(6) * y + matrix.get(10) * z);

        }

        private final float _90 = (float) Math.toRadians(90);

        private final float _maxPitch = (float) Math.toRadians(85);

        private float heading = 0.0f;

        private float pitch = 0.0f;


 /*

 * cosa/sina deterine pitch. cosb/sinb determine strafe vector. cosz/sinz are

 * 90 degrees off cosb/sinb and determine forward/back. cosc/sinc determine

 * roll (lean) which we are not altering so we set identity values for them *

 */

        private float cosa, cosb, cosz, sina, sinb, sinz;

        private float cosc = 1.0f;

        private float sinc = 0.0f;

        private float x, y, z;//player position

 /*

 * OpenGL matrices are coloumn major. To envisage this take the normal

 * row major matrix and imagine rotating it 90 degrees, now mirror it about the y

axis.

 * Being an identity matrix, this one will still appear the same, except

 * that the idices 0, 1, 2, for example, will accees the first three

 * values down the first coloumn. Read Songho's article to see what I mean.

 */

        private float[] mat = {1, 0, 0, 0,

                0, 1, 0, 0,

                0, 0, 1, 0,

                0, 0, 0, 1};

        private FloatBuffer matrix = BufferUtil.newFloatBuffer(mat.length);

        {
            matrix.put(mat);

            x = z = 1.0f;

            //there is no floor collider so we set a static y value
            y = -2.2f;

        }

 /*

 * Heading is an angle. Given that 0 deg is East cos and sin values of

 * heading are calculated relative to East and are therefore relevant

 * for determining strafe movements. We need to add 90 deg to heading

 * to get cos and sin values relevant for moving the player forward or back

 */

        public void updateHeading(float amount) {

            heading -= amount * angleSpeed;

            cosb = (float) Math.toRadians(Math.cos(heading));

            sinb = (float) Math.toRadians(Math.sin(heading));

            cosz = (float) Math.toRadians(Math.cos(heading + _90));

            sinz = (float) Math.toRadians(Math.sin(heading + _90));

        }

 /*

 * We don't want the player doing summersalts so we limit the

 * angles through which they can pitch

 */

        public void updatePitch(float amount) {

            pitch -= amount * angleSpeed;

            if (pitch > _maxPitch) pitch = _maxPitch;

            if (pitch < -_maxPitch) pitch = -_maxPitch;

            cosa = (float) Math.cos(pitch);

            sina = (float) Math.sin(pitch);

        }

 /*

 * what we need to do is construct a vector that we can add to the player's

 * current position to give us the players new position. cosz/sinz give us

 * the direction of the vector, amount gives the scaling factor.

 */


        public void updateStrafe(float v) {
            x += cosb * v * linSpeed;
            z += sinb * v * linSpeed;
        }

        public void updateForward(float v) {
            x += cosz * v * linSpeed;
            z += sinz * v * linSpeed;
        }

 /*

 * Here is where the matrix for each loop is constructed. This calculation

 * is specific for z, x, y order rotations. See Songho for explanation.

 * Basically, any rotation about an axis alters all other axes due the

 * fact that all axes must remain orthogonal (at right angles to each other),

 * thus different orders will give different results.

 */

        /*

 * Before loading the new matrix we draw a quad which represents a weapon.

 * Because it is drawn before matrix is loaded the vertices are not altered

 * by the matrix. The Floor, which is drawn after the matrix is loaded will

 * alter every matrix change, the weapon will not.

 */


        public void render(GL2 gl, float dt) {

            pollEvents();

            //if(robot!=null)

            //robot.mouseMove(mouseCenter.x, mouseCenter.y);

            //shoot=0;

            //use=0;

 /*

 * Everything drawn after the ModelViewMatrix has been sent to GL will

 * be effected by that matrix. As we will see in the Player class, weapons

 * that move with the player are drawn before the matrix is loaded,

 * but the floor is not.

 */

//            player.draw(gl);
//
//            floor.draw(gl);

            //gl.glLoadIdentity();

            gl.glLoadMatrixf(matrix);

            for (int a = -8; a < 8; a++) {
                for (int b = -8; b < 4; b++) {
                    for (int c = -4; c < 4; c++) {
                        gl.glPushMatrix();

                        gl.glTranslatef(
                                a * 10f, b * 0.1f, c * 10f
                        );

                        gl.glColor4f(100f + (float)Math.sin(a*3f) * 75f, 100+0.5f, 0.5f, 0.2f);

                        gl.glBegin(gl.GL_QUADS);

                        gl.glVertex3f(1.0f, 1.0f, -1.0f);

                        gl.glVertex3f(1.0f, 1.0f, 1.0f);

                        gl.glVertex3f(-1.0f, 1.0f, 1.0f);

                        gl.glVertex3f(-1.0f, 1.0f, -1f);

                        gl.glEnd();

                        gl.glPopMatrix();
                    }
                }
            }

            draw(gl, dt);

            matrix.rewind();


        }


    }

    abstract protected void draw(GL2 gl, float dt);

    Game game;
    private int w = 1024, h = 768;

    private int forward = KeyEvent.VK_W;
    private int backward = KeyEvent.VK_S;
    private int strafeL = KeyEvent.VK_A;
    private int strafeR = KeyEvent.VK_D;
    private int shoot = InputEvent.BUTTON1_MASK;
    private int use = InputEvent.BUTTON3_MASK;

    public void init(GLAutoDrawable drawable) {


        GL2 gl = (GL2) drawable.getGL();

        initEffects(gl);

        game = new Game(gl);

        game.setMouseCenter(new Point());

        GLU glu = new GLU();

        gl.glViewport(0, 0, w, h);

        gl.glMatrixMode(GL_PROJECTION);

        glu.gluPerspective(75.0f, ((float) w / (float) h), 0.1f, 25.0f);

        gl.glMatrixMode(GL_MODELVIEW);

        gl.glShadeModel(gl.GL_SMOOTH);

        //gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        gl.glClearDepth(1.0f);

        gl.glEnable(gl.GL_DEPTH_TEST);

        gl.glDepthFunc(gl.GL_LEQUAL);

        //gl.glHint(gl.GL_PERSPECTIVE_CORRECTION_HINT, gl.GL_NICEST);


        this.addKeyListener(new KeyAdapter() {

            @Override

            public void keyPressed(KeyEvent e) {

                int k = e.getKeyCode();
                if (k == KeyEvent.VK_ESCAPE) {

                    new Thread(new Runnable() {

                        public void run() {

                            System.exit(0);

                        }

                    }).start();

                }

                //TODO switch/table
                if (k == forward) game.forward(+1f);

                else if (k == backward) game.forward(-1);

                else if (k == strafeL) game.strafe(+1);

                else if (k == strafeR) game.strafe(-1);

            }

            @Override

            public void keyReleased(KeyEvent e) {

                int k = e.getKeyCode();
                if (k == forward || k == backward) game.forward(0);
                else if (k == strafeL || k == strafeR) game.strafe(0);

            }

        });

        this.addMouseListener(new MouseAdapter() {

            @Override

            public void mouseClicked(MouseEvent e) {

                if ((e.getModifiers() & shoot) != 0)

                    game.click();

//                if((e.getModifiers() & use)!=0)
//
//                    game.setUse();

            }

        });

    }


//    public void init2D(GLAutoDrawable arg0) {
//        GL gl2 = getGL();
//
//
//        //getGL().getGL2().glClearColor(0f, 0f, 0f, 1f);
//
//
//        new FPSAnimator(this, 25);
//    }

    public void initEffects(GL gl2) {
        gl2.glLineWidth(2f);

        gl2.glEnable(GL.GL_LINE_SMOOTH);
        gl2.glEnable(GL.GL_LINE_WIDTH);
        gl2.glEnable(GL2.GL_BLEND);
    }


    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        GL2 gl = (GL2) drawable.getGL();

        GLU glu = new GLU();

        if (height <= 0) {
            height = 1;
        }

        final float h = (float) width / (float) height;

        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL_PROJECTION);

        gl.glLoadIdentity();

        glu.gluPerspective(45.0f, h, 1.0, 20.0);

        gl.glMatrixMode(GL_MODELVIEW);

        gl.glLoadIdentity();

        center = new Point(x + width / 2, y + height / 2);

        game.setMouseCenter(center);

    }


    public void reshape2D(GLAutoDrawable arg0, int arg1, int arg2, int arg3, int arg4) {
        float width = getWidth();
        float height = getHeight();

        GL2 gl2 = arg0.getGL().getGL2();

        gl2.glMatrixMode(GL_PROJECTION);
        gl2.glLoadIdentity();

        // coordinate system origin at lower left with width and height same as the window
        GLU glu = new GLU();
        glu.gluOrtho2D(0.0f, width, 0.0f, height);


        gl2.glMatrixMode(GL_MODELVIEW);
        gl2.glLoadIdentity();

        gl2.glViewport(0, 0, getWidth(), getHeight());

    }
}
