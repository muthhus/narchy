/*
 * Java port of Bullet (c) 2008 Martin Dvorak <jezek2@advel.cz>
 *
 * Bullet Continuous Collision Detection and Physics Library
 * Copyright (c) 2003-2007 Erwin Coumans  http://continuousphysics.com/Bullet/
 *
 * This software is provided 'as-is', without any express or implied warranty.
 * In no event will the authors be held liable for any damages arising from
 * the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose, 
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */

package nars.gui.test.bullet;

import com.bulletphysics.ContactAddedCallback;
import com.bulletphysics.ContactDestroyedCallback;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.SimpleBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.Point2PointConstraint;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.TypedConstraint;
import com.bulletphysics.linearmath.*;
import com.jogamp.newt.event.*;
import com.jogamp.opengl.*;
import nars.util.JoglSpace;

import javax.vecmath.Color3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.*;

/**
 * @author jezek2
 */

public class JoglPhysics extends JoglSpace implements MouseListener, GLEventListener, KeyListener {


    @Override
    public void mouseWheelMoved(MouseEvent e) {

    }


    @Override
    protected void init(GL2 gl2) {

        window.addMouseListener(this);
        window.addKeyListener(this);

        float[] light_ambient = new float[]{0.2f, 0.2f, 0.2f, 1.0f};
        float[] light_diffuse = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
        float[] light_specular = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
        /* light_position is NOT default value */
        float[] light_position0 = new float[]{1.0f, 10.0f, 1.0f, 0.0f};
        float[] light_position1 = new float[]{-1.0f, -10.0f, -1.0f, 0.0f};

//        if (gl.isGLES2()) {
//            //gl.enableFixedFunctionEmulationMode(GLES2.FIXED_EMULATION_VERTEXCOLORTEXTURE);
//
//        }

        printHardware();

        glsrt = new GLSRT(glu);
        if (useLight0) {
            gl.glLightfv(gl.GL_LIGHT0, gl.GL_AMBIENT, light_ambient, 0);
            gl.glLightfv(gl.GL_LIGHT0, gl.GL_DIFFUSE, light_diffuse, 0);
            gl.glLightfv(gl.GL_LIGHT0, gl.GL_SPECULAR, light_specular, 0);
            gl.glLightfv(gl.GL_LIGHT0, gl.GL_POSITION, light_position0, 0);
        }

        if (useLight1) {
            gl.glLightfv(gl.GL_LIGHT1, gl.GL_AMBIENT, light_ambient, 0);
            gl.glLightfv(gl.GL_LIGHT1, gl.GL_DIFFUSE, light_diffuse, 0);
            gl.glLightfv(gl.GL_LIGHT1, gl.GL_SPECULAR, light_specular, 0);
            gl.glLightfv(gl.GL_LIGHT1, gl.GL_POSITION, light_position1, 0);
        }

        if (useLight0 || useLight1) {
            gl.glEnable(gl.GL_LIGHTING);
        }
        if (useLight0) {
            gl.glEnable(gl.GL_LIGHT0);
        }
        if (useLight1) {
            gl.glEnable(gl.GL_LIGHT1);
        }

        gl.glShadeModel(gl.GL_SMOOTH);

        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glDepthFunc(gl.GL_LESS);

        gl.glClearColor(0.7f, 0.7f, 0.7f, 0f);

        // JAU
        gl.glEnable(gl.GL_CULL_FACE);
        gl.glCullFace(gl.GL_BACK);

    }




    protected final BulletStack stack = BulletStack.get();

    private static final float STEPSIZE = 5;

    //public static int numObjects = 0;
    public static final int maxNumObjects = 16384;
    public static final Transform[] startTransforms = new Transform[maxNumObjects];
    public static CollisionShape[] gShapePtr = new CollisionShape[maxNumObjects]; //1 rigidbody has 1 shape (no re-use of shapes)

    public static RigidBody pickedBody = null; // for deactivation state

    static {
        for (int i = 0; i < startTransforms.length; i++) {
            startTransforms[i] = new Transform();
        }
    }
    // TODO: class CProfileIterator* m_profileIterator;

    protected final Clock clock = new Clock();

    // this is the most important class
    protected DynamicsWorld dyn = null;

    // constraint for mouse picking
    protected TypedConstraint pickConstraint = null;

    protected CollisionShape shootBoxShape = null;

    protected float cameraDistance = 15f;
    protected int debug = 0;

    protected float ele = 20f;
    protected float azi = 0f;
    protected final Vector3f camPos = new Vector3f(0f, 0f, 0f);
    protected final Vector3f camPosTarget = new Vector3f(0f, 0f, 0f); // look at

    protected float scaleBottom = 0.5f;
    protected float scaleFactor = 2f;
    protected final Vector3f camUp = new Vector3f(0f, 1f, 0f);
    protected int forwardAxis = 2;

    protected int glutScreenWidth = 0;
    protected int glutScreenHeight = 0;

    protected float ShootBoxInitialSpeed = 40f;

    protected boolean stepping = true;
    protected boolean singleStep = false;
    protected boolean idle = false;
    protected int lastKey;


    protected GLSRT glsrt = null;

    protected boolean useLight0 = true;
    protected boolean useLight1 = true;

    public JoglPhysics() {
        super();

        // debugMode |= DebugDrawModes.DRAW_WIREFRAME;
        debug |= DebugDrawModes.NO_HELP_TEXT;
        //debugMode |= DebugDrawModes.DISABLE_BULLET_LCP;

//        for(int i=args.length-1; i>=0; i--) {
//            if(args[i].equals("-nolight0")) {
//                useLight0=false;
//            } else if(args[i].equals("-nolight1")) {
//                useLight1=false;
//            }
//        }

        // Setup the basic world
        DefaultCollisionConfiguration collision_config = new DefaultCollisionConfiguration();

        CollisionDispatcher dispatcher = new CollisionDispatcher(collision_config);

        //btPoint3 worldAabbMin(-10000,-10000,-10000);
        //btPoint3 worldAabbMax(10000,10000,10000);
        //btBroadphaseInterface* overlappingPairCache = new btAxisSweep3 (worldAabbMin, worldAabbMax);
        BroadphaseInterface overlappingPairCache = new SimpleBroadphase();

        //#ifdef USE_ODE_QUICKSTEP
        //btConstraintSolver* constraintSolver = new OdeConstraintSolver();
        //#else
        ConstraintSolver constraintSolver = new SequentialImpulseConstraintSolver();
        //#endif

        dyn = new DiscreteDynamicsWorld(dispatcher, overlappingPairCache, constraintSolver, collision_config);

        dyn.setGravity(new Vector3f(0f, -30f, 0f));

        // Setup a big ground box
        CollisionShape groundShape = new BoxShape(new Vector3f(200f, 10f, 200f));
        Transform groundTransform = new Transform();
        groundTransform.setIdentity();
        groundTransform.origin.set(0f, -15f, 0f);
        localCreateRigidBody(0f, groundTransform, groundShape);

    }


    public void destroy() {
        // TODO: CProfileManager::Release_Iterator(m_profileIterator);
        //if (m_shootBoxShape)
        //	delete m_shootBoxShape;
    }

    //
    // GLEventListener
    //


    public void dispose(GLAutoDrawable drawable) {
        gl = null;
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        gl = drawable.getGL().getGL2();
        glutScreenWidth = width;
        glutScreenHeight = height;

        //gl.glViewport(x, y, width, height);
        updateCamera();

        //System.out.println("DemoApplication RESHAPE");
    }

    public void display(GLAutoDrawable drawable) {

        gl = drawable.getGL().getGL2();

        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);


        if (!isIdle()) {
            // simple dynamics world doesn't handle fixed-time-stepping
            float ms = clock.getTimeMicroseconds();
            clock.reset();
            float minFPS = 1000000f / 60f;
            if (ms > minFPS) {
                ms = minFPS;
            }
            if (dyn != null) {
                dyn.stepSimulation(ms / 1000000.f);
                updateCamera();
            }
        }

        if (dyn != null) {
            // optional but useful: debug drawing
            dyn.debugDrawWorld();
        }

        renderWorld();

        //glFlush();
        //glutSwapBuffers();
    }

    public void displayChanged() {
    }



    //
    // MouseListener
    //
    public void mouseClicked(MouseEvent e) {
        mouseClick(e.getButton(), e.getX(), e.getY());
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        pickConstrain(e.getButton(), 1, e.getX(), e.getY());
    }

    public void mouseReleased(MouseEvent e) {
        pickConstrain(e.getButton(), 0, e.getX(), e.getY());
    }

    //
    // MouseMotionListener
    //
    public void mouseDragged(MouseEvent e) {
        mouseMotionFunc(e.getX(), e.getY());
    }

    public void mouseMoved(MouseEvent e) {
    }

    //
    // KeyListener
    //
    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
        if (e.isActionKey()) {
            specialKeyboard(e.getKeyCode());
        } else {
            keyboardCallback(e.getKeyChar());
        }
    }

    //
    //
    //

    public void setCameraDistance(float dist) {
        cameraDistance = dist;
    }

    public float getCameraDistance() {
        return cameraDistance;
    }

    public void toggleIdle() {
        if (idle) {
            idle = false;
        } else {
            idle = true;
        }
    }

    public void updateCamera() {
        stack.vectors.push();
        stack.matrices.push();
        stack.quats.push();
        try {
            gl.glMatrixMode(gl.GL_PROJECTION);
            gl.glLoadIdentity();
            float rele = ele * 0.01745329251994329547f; // rads per deg
            float razi = azi * 0.01745329251994329547f; // rads per deg

            Quat4f rot = stack.quats.get();
            QuaternionUtil.setRotation(rot, camUp, razi);

            Vector3f eyePos = stack.vectors.get(0f, 0f, 0f);
            VectorUtil.setCoord(eyePos, forwardAxis, -cameraDistance);

            Vector3f forward = stack.vectors.get(eyePos.x, eyePos.y, eyePos.z);
            if (forward.lengthSquared() < BulletGlobals.FLT_EPSILON) {
                forward.set(1f, 0f, 0f);
            }
            Vector3f right = stack.vectors.get();
            right.cross(camUp, forward);
            Quat4f roll = stack.quats.get();
            QuaternionUtil.setRotation(roll, right, -rele);

            Matrix3f tmpMat1 = stack.matrices.get();
            Matrix3f tmpMat2 = stack.matrices.get();
            tmpMat1.set(rot);
            tmpMat2.set(roll);
            tmpMat1.mul(tmpMat2);
            tmpMat1.transform(eyePos);

            camPos.set(eyePos);

            gl.glFrustumf(-1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 10000.0f);
            glu.gluLookAt(camPos.x, camPos.y, camPos.z,
                    camPosTarget.x, camPosTarget.y, camPosTarget.z,
                    camUp.x, camUp.y, camUp.z);
            gl.glMatrixMode(gl.GL_MODELVIEW);
            gl.glLoadIdentity();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stack.vectors.pop();
            stack.matrices.pop();
            stack.quats.pop();
        }
    }

    public void stepLeft() {
        azi -= STEPSIZE;
        if (azi < 0) {
            azi += 360;
        }
    }

    public void stepRight() {
        azi += STEPSIZE;
        if (azi >= 360) {
            azi -= 360;
        }
    }

    public void stepFront() {
        ele += STEPSIZE;
        if (ele >= 360) {
            ele -= 360;
        }
    }

    public void stepBack() {
        ele -= STEPSIZE;
        if (ele < 0) {
            ele += 360;
        }
    }

    public void zoomIn() {
        cameraDistance -= 0.4f;
        if (cameraDistance < 0.1f) {
            cameraDistance = 0.1f;
        }
    }

    public void zoomOut() {
        cameraDistance += 0.4f;
    }

    public void keyboardCallback(char key) {
        lastKey = 0;

        if (key >= 0x31 && key < 0x37) {
            int child = key - 0x31;
            // TODO: m_profileIterator->Enter_Child(child);
        }
        if (key == 0x30) {
            // TODO: m_profileIterator->Enter_Parent();
        }

        switch (key) {
            case 'l':
                stepLeft();
                break;
            case 'r':
                stepRight();
                break;
            case 'f':
                stepFront();
                break;
            case 'b':
                stepBack();
                break;
            case 'z':
                zoomIn();
                break;
            case 'x':
                zoomOut();
                break;
            case 'i':
                toggleIdle();
                break;
            case 'h':
                if ((debug & DebugDrawModes.NO_HELP_TEXT) != 0) {
                    debug = debug & (~DebugDrawModes.NO_HELP_TEXT);
                } else {
                    debug |= DebugDrawModes.NO_HELP_TEXT;
                }
                break;

            case 'w':
                if ((debug & DebugDrawModes.DRAW_WIREFRAME) != 0) {
                    debug = debug & (~DebugDrawModes.DRAW_WIREFRAME);
                } else {
                    debug |= DebugDrawModes.DRAW_WIREFRAME;
                }
                break;

            case 'p':
                if ((debug & DebugDrawModes.PROFILE_TIMINGS) != 0) {
                    debug = debug & (~DebugDrawModes.PROFILE_TIMINGS);
                } else {
                    debug |= DebugDrawModes.PROFILE_TIMINGS;
                }
                break;

            case 'm':
                if ((debug & DebugDrawModes.ENABLE_SAT_COMPARISON) != 0) {
                    debug = debug & (~DebugDrawModes.ENABLE_SAT_COMPARISON);
                } else {
                    debug |= DebugDrawModes.ENABLE_SAT_COMPARISON;
                }
                break;

            case 'n':
                if ((debug & DebugDrawModes.DISABLE_BULLET_LCP) != 0) {
                    debug = debug & (~DebugDrawModes.DISABLE_BULLET_LCP);
                } else {
                    debug |= DebugDrawModes.DISABLE_BULLET_LCP;
                }
                break;

            case 't':
                if ((debug & DebugDrawModes.DRAW_TEXT) != 0) {
                    debug = debug & (~DebugDrawModes.DRAW_TEXT);
                } else {
                    debug |= DebugDrawModes.DRAW_TEXT;
                }
                break;
            case 'y':
                if ((debug & DebugDrawModes.DRAW_FEATURES_TEXT) != 0) {
                    debug = debug & (~DebugDrawModes.DRAW_FEATURES_TEXT);
                } else {
                    debug |= DebugDrawModes.DRAW_FEATURES_TEXT;
                }
                break;
            case 'a':
                if ((debug & DebugDrawModes.DRAW_AABB) != 0) {
                    debug = debug & (~DebugDrawModes.DRAW_AABB);
                } else {
                    debug |= DebugDrawModes.DRAW_AABB;
                }
                break;
            case 'c':
                if ((debug & DebugDrawModes.DRAW_CONTACT_POINTS) != 0) {
                    debug = debug & (~DebugDrawModes.DRAW_CONTACT_POINTS);
                } else {
                    debug |= DebugDrawModes.DRAW_CONTACT_POINTS;
                }
                break;

            case 'd':
                if ((debug & DebugDrawModes.NO_DEACTIVATION) != 0) {
                    debug = debug & (~DebugDrawModes.NO_DEACTIVATION);
                } else {
                    debug |= DebugDrawModes.NO_DEACTIVATION;
                }
                if ((debug & DebugDrawModes.NO_DEACTIVATION) != 0) {
                    BulletGlobals.gDisableDeactivation = true;
                } else {
                    BulletGlobals.gDisableDeactivation = false;
                }
                break;

            case 'o': {
                stepping = !stepping;
                break;
            }
            case 's':
                break;
            //    case ' ' : newRandom(); break;
            case ' ':
                clientResetScene();
                break;
            case '1': {
                if ((debug & DebugDrawModes.ENABLE_CCD) != 0) {
                    debug = debug & (~DebugDrawModes.ENABLE_CCD);
                } else {
                    debug |= DebugDrawModes.ENABLE_CCD;
                }
                break;
            }

            case '.': {
                shootBox(getCamPosTarget());
                break;
            }

            case '+': {
                ShootBoxInitialSpeed += 10f;
                break;
            }
            case '-': {
                ShootBoxInitialSpeed -= 10f;
                break;
            }

            default:
                // std::cout << "unused key : " << key << std::endl;
                break;
        }

        if (getDyn() != null && getDyn().getDebugDrawer() != null) {
            getDyn().getDebugDrawer().setDebugMode(debug);
        }

        //LWJGL.postRedisplay();
    }

    public int getDebug() {
        return debug;
    }

    public void setDebug(int mode) {
        debug = mode;
        if (getDyn() != null && getDyn().getDebugDrawer() != null) {
            getDyn().getDebugDrawer().setDebugMode(mode);
        }
    }

    public void specialKeyboard(int keycode) {
        switch (keycode) {
            case KeyEvent.VK_F1: {
                break;
            }
            case KeyEvent.VK_F2: {
                break;
            }
            case KeyEvent.VK_END: {
                int numObj = getDyn().getNumCollisionObjects();
                if (numObj != 0) {
                    CollisionObject obj = getDyn().getCollisionObjectArray().get(numObj - 1);

                    getDyn().removeCollisionObject(obj);
                    RigidBody body = RigidBody.upcast(obj);
                    if (body != null && body.getMotionState() != null) {
                        //delete body->getMotionState();
                    }
                    //delete obj;
                }
                break;
            }
            case KeyEvent.VK_LEFT:
                stepLeft();
                break;
            case KeyEvent.VK_RIGHT:
                stepRight();
                break;
            case KeyEvent.VK_UP:
                stepFront();
                break;
            case KeyEvent.VK_DOWN:
                stepBack();
                break;
            /*
			case KeyEvent.VK_PRIOR:
				zoomIn();
				break;
			case KeyEvent.VK_NEXT:
				zoomOut();
				break;
            */
            case KeyEvent.VK_HOME:
                toggleIdle();
                break;
            default:
                // std::cout << "unused (special) key : " << key << std::endl;
                break;
        }
    }

    public void shootBox(Vector3f destination) {
        if (dyn != null) {
            float mass = 10f;
            Transform startTransform = new Transform();
            startTransform.setIdentity();
            Vector3f camPos = new Vector3f(getCamPos());
            startTransform.origin.set(camPos);

            if (shootBoxShape == null) {
                //#define TEST_UNIFORM_SCALING_SHAPE 1
                //#ifdef TEST_UNIFORM_SCALING_SHAPE
                //btConvexShape* childShape = new btBoxShape(btVector3(1.f,1.f,1.f));
                //m_shootBoxShape = new btUniformScalingShape(childShape,0.5f);
                //#else
                shootBoxShape = new BoxShape(new Vector3f(1f, 1f, 1f));
                //#endif//
            }

            RigidBody body = this.localCreateRigidBody(mass, startTransform, shootBoxShape);

            Vector3f linVel = new Vector3f(destination.x - camPos.x, destination.y - camPos.y, destination.z - camPos.z);
            linVel.normalize();
            linVel.scale(ShootBoxInitialSpeed);

            Transform ct = new Transform();
            ct.origin.set(camPos);
            ct.setRotation(new Quat4f(0f, 0f, 0f, 1f));
            body.setWorldTransform(ct);

            body.setLinearVelocity(linVel);
            body.setAngularVelocity(new Vector3f(0f, 0f, 0f));
        }
    }

    public Vector3f getRayTo(int x, int y) {
        float top = 1f;
        float bottom = -1f;
        float nearPlane = 1f;
        float tanFov = (top - bottom) * 0.5f / nearPlane;
        float fov = 2f * (float) Math.atan(tanFov);

        Vector3f rayFrom = new Vector3f(getCamPos());
        Vector3f rayForward = new Vector3f();
        rayForward.sub(getCamPosTarget(), getCamPos());
        rayForward.normalize();
        float farPlane = 600f;
        rayForward.scale(farPlane);

        Vector3f rightOffset = new Vector3f();
        Vector3f vertical = new Vector3f(camUp);

        Vector3f hor = new Vector3f();
        // TODO: check: hor = rayForward.cross(vertical);
        hor.cross(rayForward, vertical);
        hor.normalize();
        // TODO: check: vertical = hor.cross(rayForward);
        vertical.cross(hor, rayForward);
        vertical.normalize();

        float tanfov = (float) Math.tan(0.5f * fov);
        hor.scale(2f * farPlane * tanfov);
        vertical.scale(2f * farPlane * tanfov);
        Vector3f rayToCenter = new Vector3f();
        rayToCenter.add(rayFrom, rayForward);
        Vector3f dHor = new Vector3f(hor);
        dHor.scale(1f / (float) glutScreenWidth);
        Vector3f dVert = new Vector3f(vertical);
        dVert.scale(1.f / (float) glutScreenHeight);

        Vector3f tmp1 = new Vector3f();
        Vector3f tmp2 = new Vector3f();
        tmp1.scale(0.5f, hor);
        tmp2.scale(0.5f, vertical);

        Vector3f rayTo = new Vector3f();
        rayTo.sub(rayToCenter, tmp1);
        rayTo.add(tmp2);

        tmp1.scale(x, dHor);
        tmp2.scale(y, dVert);

        rayTo.add(tmp1);
        rayTo.sub(tmp2);
        return rayTo;
    }

    private void mouseClick(int button, int x, int y) {
        Vector3f rayTo = new Vector3f(getRayTo(x, y));

        switch (button) {
            case MouseEvent.BUTTON3: {
                shootBox(rayTo);
                break;
            }
            case MouseEvent.BUTTON2: {
                // apply an impulse
                if (dyn != null) {
                    CollisionWorld.ClosestRayResultCallback rayCallback = new CollisionWorld.ClosestRayResultCallback(camPos, rayTo);
                    dyn.rayTest(camPos, rayTo, rayCallback);
                    if (rayCallback.hasHit()) {
                        RigidBody body = RigidBody.upcast(rayCallback.collisionObject);
                        if (body != null) {
                            body.setActivationState(CollisionObject.ACTIVE_TAG);
                            Vector3f impulse = new Vector3f(rayTo);
                            impulse.normalize();
                            float impulseStrength = 10f;
                            impulse.scale(impulseStrength);
                            Vector3f relPos = new Vector3f();

                            relPos.sub(rayCallback.hitPointWorld, body.getCenterOfMassPosition(new Vector3f()));
                            body.applyImpulse(impulse, relPos);
                        }
                    }
                }
                break;
            }
        }
    }

    private void pickConstrain(int button, int state, int x, int y) {
        Vector3f rayTo = new Vector3f(getRayTo(x, y));

        switch (button) {
            case MouseEvent.BUTTON1: {
                if (state == 1) {
                    // add a point to point constraint for picking
                    if (dyn != null) {
                        CollisionWorld.ClosestRayResultCallback rayCallback = new CollisionWorld.ClosestRayResultCallback(camPos, rayTo);
                        dyn.rayTest(camPos, rayTo, rayCallback);
                        if (rayCallback.hasHit()) {
                            RigidBody body = RigidBody.upcast(rayCallback.collisionObject);
                            if (body != null) {
                                // other exclusions?
                                if (!(body.isStaticObject() || body.isKinematicObject())) {
                                    pickedBody = body;
                                    pickedBody.setActivationState(CollisionObject.DISABLE_DEACTIVATION);

                                    Vector3f pickPos = new Vector3f(rayCallback.hitPointWorld);

                                    Transform tmpTrans = body.getCenterOfMassTransform(new Transform());
                                    tmpTrans.inverse();
                                    Vector3f localPivot = new Vector3f(pickPos);
                                    tmpTrans.transform(localPivot);

                                    Point2PointConstraint p2p = new Point2PointConstraint(body, localPivot);
                                    dyn.addConstraint(p2p);
                                    pickConstraint = p2p;
                                    // save mouse position for dragging
                                    BulletGlobals.gOldPickingPos.set(rayTo);
                                    Vector3f eyePos = new Vector3f(camPos);
                                    Vector3f tmp = new Vector3f();
                                    tmp.sub(pickPos, eyePos);
                                    BulletGlobals.gOldPickingDist = tmp.length();
                                    // very weak constraint for picking
                                    p2p.setting.tau = 0.1f;
                                }
                            }
                        }
                    }

                } else {

                    if (pickConstraint != null && dyn != null) {
                        dyn.removeConstraint(pickConstraint);
                        // delete m_pickConstraint;
                        pickConstraint = null;
                        pickedBody.forceActivationState(CollisionObject.ACTIVE_TAG);
                        pickedBody.setDeactivationTime(0f);
                        pickedBody = null;
                    }
                }
                break;
            }
            default: {
            }
        }
    }

    private void mouseMotionFunc(int x, int y) {
        if (pickConstraint != null) {
            // move the constraint pivot
            Point2PointConstraint p2p = (Point2PointConstraint) pickConstraint;
            if (p2p != null) {
                // keep it at the same picking distance

                Vector3f newRayTo = new Vector3f(getRayTo(x, y));
                Vector3f eyePos = new Vector3f(camPos);
                Vector3f dir = new Vector3f();
                dir.sub(newRayTo, eyePos);
                dir.normalize();
                dir.scale(BulletGlobals.gOldPickingDist);

                Vector3f newPos = new Vector3f();
                newPos.add(eyePos, dir);
                p2p.setPivotB(newPos);
            }
        }
    }

    /**
     * Bullet's global variables and constants.
     *
     * @author jezek2
     */
    static class BulletGlobals {

        public static final boolean DEBUG = true;
        public static final boolean ENABLE_PROFILE = false;

        public static final float CONVEX_DISTANCE_MARGIN = 0.04f;
        public static final float FLT_EPSILON = 1.19209290e-07f;
        public static final float SIMD_EPSILON = FLT_EPSILON;

        public static final float SIMD_2_PI = 6.283185307179586232f;
        public static final float SIMD_PI = SIMD_2_PI * 0.5f;
        public static final float SIMD_HALF_PI = SIMD_2_PI * 0.25f;
        public static final float SIMD_RADS_PER_DEG = SIMD_2_PI / 360f;
        public static final float SIMD_DEGS_PER_RAD = 360f / SIMD_2_PI;
        public static final float SIMD_INFINITY = Float.MAX_VALUE;

        public static ContactDestroyedCallback gContactDestroyedCallback;
        public static ContactAddedCallback gContactAddedCallback;
        public static float gContactBreakingThreshold = 0.02f;

        // RigidBody
        public static float gDeactivationTime = 2f;
        public static boolean gDisableDeactivation = false;

        public static int gTotalContactPoints;

        // GjkPairDetector
        // temp globals, to improve GJK/EPA/penetration calculations
        public static int gNumDeepPenetrationChecks = 0;
        public static int gNumGjkChecks = 0;

        public static int gNumAlignedAllocs;
        public static int gNumAlignedFree;
        public static int gTotalBytesAlignedAllocs;

        public static int gPickingConstraintId = 0;
        public static final Vector3f gOldPickingPos = new Vector3f();
        public static float gOldPickingDist = 0.f;

        public static int gOverlappingPairs = 0;
        public static int gRemovePairs = 0;
        public static int gAddedPairs = 0;
        public static int gFindPairs = 0;

        public static final Vector3f ZERO_VECTOR3 = new Vector3f(0f, 0f, 0f);

        private static final List<ProfileBlock> profileStack = new ArrayList<ProfileBlock>();
        private static final Map<String, Long> profiles = new HashMap<String, Long>();

        // JAVA NOTE: added for statistics in applet demo
        public static long stepSimulationTime;
        public static long updateTime;

        public static void pushProfile(String name) {
            if (!ENABLE_PROFILE) return;

            ProfileBlock block = new ProfileBlock();
            block.name = name;
            block.startTime = System.currentTimeMillis();
            profileStack.add(block);
        }

        public static void popProfile() {
            if (!ENABLE_PROFILE) return;

            ProfileBlock block = profileStack.remove(profileStack.size() - 1);
            long time = System.currentTimeMillis();

            Long totalTime = profiles.get(block.name);
            if (totalTime == null) totalTime = 0L;
            totalTime += (time - block.startTime);
            profiles.put(block.name, totalTime);
        }

        public static void printProfiles() {
            ArrayList<Map.Entry<String, Long>> list = new ArrayList<Map.Entry<String, Long>>(profiles.entrySet());
            Collections.sort(list, new Comparator<Map.Entry<String, Long>>() {
                public int compare(Map.Entry<String, Long> e1, Map.Entry<String, Long> e2) {
                    return e1.getValue().compareTo(e2.getValue());
                }
            });

            for (Map.Entry<String, Long> e : /*profiles.entrySet()*/list) {
                System.out.println(e.getKey() + " = " + e.getValue() + " ms");
            }
        }

        static {
            if (ENABLE_PROFILE) {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        printProfiles();
                    }
                });
            }
        }

        private static class ProfileBlock {
            public String name;
            public long startTime;
        }

    }

    public RigidBody localCreateRigidBody(float mass, Transform startTransform, CollisionShape shape) {
        // rigidbody is dynamic if and only if mass is non zero, otherwise static
        boolean isDynamic = (mass != 0f);

        Vector3f localInertia = new Vector3f(0f, 0f, 0f);
        if (isDynamic) {
            shape.calculateLocalInertia(mass, localInertia);
        }

        // using motionstate is recommended, it provides interpolation capabilities, and only synchronizes 'active' objects

        //#define USE_MOTIONSTATE 1
        //#ifdef USE_MOTIONSTATE
        DefaultMotionState myMotionState = new DefaultMotionState(startTransform);
        RigidBody body = new RigidBody(new RigidBodyConstructionInfo(mass, myMotionState, shape, localInertia));
        //#else
        //btRigidBody* body = new btRigidBody(mass,0,shape,localInertia);
        //body->setWorldTransform(startTransform);
        //#endif//
        dyn.addRigidBody(body);

        return body;
    }

    // See http://www.lighthouse3d.com/opengl/glut/index.php?bmpfontortho
    public void setOrthographicProjection() {
        // switch to projection mode
        gl.glMatrixMode(gl.GL_PROJECTION);
        // save previous matrix which contains the
        //settings for the perspective projection
        // gl.glPushMatrix();
        // reset matrix
        gl.glLoadIdentity();
        // set a 2D orthographic projection
        glu.gluOrtho2D(0f, glutScreenWidth, 0f, glutScreenHeight);
        // invert the y axis, down is positive
        gl.glScalef(1f, -1f, 1f);
        // mover the origin from the bottom left corner
        // to the upper left corner
        gl.glTranslatef(0f, -glutScreenHeight, 0f);
        gl.glMatrixMode(gl.GL_MODELVIEW);
    }

    /**
     * public void resetPerspectiveProjection() {
     * gl.glMatrixMode(gl.GL_PROJECTION);
     * gl.glPopMatrix();
     * gl.glMatrixMode(gl.GL_MODELVIEW);
     * }
     * <p>
     * private void displayProfileString(int xOffset, int yStart, String message) {
     * //glRasterPos3f(xOffset, yStart, 0);
     * // TODO: BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),message);
     * }
     * <p>
     * // TODO: protected void showProfileInfo(float& xOffset,float& yStart, float yIncr);
     */

    private final Transform m = new Transform();
    private final Vector3f wireColor = new Vector3f();
    private Color3f TEXT_COLOR = new Color3f(0f, 0f, 0f);
    // private StringBuilder buf = new StringBuilder();

    public void renderWorld() {
        // JAU updateCamera();

        if (dyn != null) {
            int numObjects = dyn.getNumCollisionObjects();
            wireColor.set(1f, 0f, 0f);
            for (int i = 0; i < numObjects; i++) {
                CollisionObject colObj = dyn.getCollisionObjectArray().get(i);
                RigidBody body = RigidBody.upcast(colObj);

                if (body != null && body.getMotionState() != null) {
                    DefaultMotionState myMotionState = (DefaultMotionState) body.getMotionState();
                    m.set(myMotionState.graphicsWorldTrans);
                } else {
                    colObj.getWorldTransform(m);
                }

                if (0 == i) {
                    wireColor.set(0.5f, 1f, 0.5f); // wants deactivation
                } else {
                    wireColor.set(1f, 1f, 0.5f); // wants deactivation
                }
                if ((i & 1) != 0) {
                    wireColor.set(0f, 0f, 1f);
                }

                // color differently for active, sleeping, wantsdeactivation states
                if (colObj.getActivationState() == 1) // active
                {
                    if ((i & 1) != 0) {
                        //wireColor.add(new Vector3f(1f, 0f, 0f));
                        wireColor.x += 1f;
                    } else {
                        //wireColor.add(new Vector3f(0.5f, 0f, 0f));
                        wireColor.x += 0.5f;
                    }
                }
                if (colObj.getActivationState() == 2) // ISLAND_SLEEPING
                {
                    if ((i & 1) != 0) {
                        //wireColor.add(new Vector3f(0f, 1f, 0f));
                        wireColor.y += 1f;
                    } else {
                        //wireColor.add(new Vector3f(0f, 0.5f, 0f));
                        wireColor.y += 0.5f;
                    }
                }

                // draw (saves the matrix already ..)
                GLShapeDrawer.drawOpenGL(glsrt, gl, m, colObj.getCollisionShape(), wireColor, getDebug());
            }
            GLShapeDrawer.drawCoordSystem(gl);
            if (false) {
                System.err.println("++++++++++++++++++++++++++++++++");
                System.err.println("++++++++++++++++++++++++++++++++");
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                }
            }

            float xOffset = 10f;
            float yStart = 20f;
            float yIncr = 20f;

            // gl.glDisable(gl.GL_LIGHTING);
            // JAU gl.glColor4f(0f, 0f, 0f, 0f);

/*
			if ((debugMode & DebugDrawModes.NO_HELP_TEXT) == 0) {
				setOrthographicProjection();

				// TODO: showProfileInfo(xOffset,yStart,yIncr);

//					#ifdef USE_QUICKPROF
//					if ( getDebugMode() & btIDebugDraw::DBG_ProfileTimings)
//					{
//						static int counter = 0;
//						counter++;
//						std::map<std::string, hidden::ProfileBlock*>::iterator iter;
//						for (iter = btProfiler::mProfileBlocks.begin(); iter != btProfiler::mProfileBlocks.end(); ++iter)
//						{
//							char blockTime[128];
//							sprintf(blockTime, "%s: %lf",&((*iter).first[0]),btProfiler::getBlockTime((*iter).first, btProfiler::BLOCK_CYCLE_SECONDS));//BLOCK_TOTAL_PERCENT));
//							glRasterPos3f(xOffset,yStart,0);
//							BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),blockTime);
//							yStart += yIncr;
//
//						}
//					}
//					#endif //USE_QUICKPROF


				String s = "mouse to interact";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				// JAVA NOTE: added
				s = "LMB=shoot, RMB=drag, MIDDLE=apply impulse";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "space to reset";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "cursor keys and z,x to navigate";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "i to toggle simulation, s single step";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "q to quit";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = ". to shoot box";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				// not yet hooked up again after refactoring...

				s = "d to toggle deactivation";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "g to toggle mesh animation (ConcaveDemo)";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				// JAVA NOTE: added
				s = "e to spawn new body (GenericJointDemo)";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				s = "h to toggle help text";
				drawString(s, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				//buf = "p to toggle profiling (+results to file)";
				//drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				//bool useBulletLCP = !(getDebugMode() & btIDebugDraw::DBG_DisableBulletLCP);
				//bool useCCD = (getDebugMode() & btIDebugDraw::DBG_EnableCCD);
				//glRasterPos3f(xOffset,yStart,0);
				//sprintf(buf,"1 CCD mode (adhoc) = %i",useCCD);
				//BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
				//yStart += yIncr;

				//glRasterPos3f(xOffset, yStart, 0);
				//buf = String.format(%10.2f", ShootBoxInitialSpeed);
				buf.setLength(0);
				buf.append("+- shooting speed = ");
				FastFormat.append(buf, ShootBoxInitialSpeed);
				drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				//#ifdef SHOW_NUM_DEEP_PENETRATIONS
				buf.setLength(0);
				buf.append("gNumDeepPenetrationChecks = ");
				FastFormat.append(buf, BulletGlobals.gNumDeepPenetrationChecks);
				drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				buf.setLength(0);
				buf.append("gNumGjkChecks = ");
				FastFormat.append(buf, BulletGlobals.gNumGjkChecks);
				drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				//buf = String.format("gNumAlignedAllocs = %d", BulletGlobals.gNumAlignedAllocs);
				// TODO: BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
				//yStart += yIncr;

				//buf = String.format("gNumAlignedFree= %d", BulletGlobals.gNumAlignedFree);
				// TODO: BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
				//yStart += yIncr;

				//buf = String.format("# alloc-free = %d", BulletGlobals.gNumAlignedAllocs - BulletGlobals.gNumAlignedFree);
				// TODO: BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
				//yStart += yIncr;

				//enable BT_DEBUG_MEMORY_ALLOCATIONS define in Bullet/src/LinearMath/btAlignedAllocator.h for memory leak detection
				//#ifdef BT_DEBUG_MEMORY_ALLOCATIONS
				//glRasterPos3f(xOffset,yStart,0);
				//sprintf(buf,"gTotalBytesAlignedAllocs = %d",gTotalBytesAlignedAllocs);
				//BMF_DrawString(BMF_GetFont(BMF_kHelvetica10),buf);
				//yStart += yIncr;
				//#endif //BT_DEBUG_MEMORY_ALLOCATIONS

				if (getDynamicsWorld() != null) {
					buf.setLength(0);
					buf.append("# objects = ");
					FastFormat.append(buf, getDynamicsWorld().getNumCollisionObjects());
					drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
					yStart += yIncr;

					buf.setLength(0);
					buf.append("# pairs = ");
					FastFormat.append(buf, getDynamicsWorld().getBroadphase().getOverlappingPairCache().getNumOverlappingPairs());
					drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
					yStart += yIncr;

				}
				//#endif //SHOW_NUM_DEEP_PENETRATIONS

				// JAVA NOTE: added
				int free = (int)Runtime.getRuntime().freeMemory();
				int total = (int)Runtime.getRuntime().totalMemory();
				buf.setLength(0);
				buf.append("heap = ");
				FastFormat.append(buf, (float)(total - free) / (1024*1024));
				buf.append(" / ");
				FastFormat.append(buf, (float)(total) / (1024*1024));
				buf.append(" MB");
				drawString(buf, Math.round(xOffset), Math.round(yStart), TEXT_COLOR);
				yStart += yIncr;

				resetPerspectiveProjection();
			} */

            // gl.glEnable(gl.GL_LIGHTING);
        }
    }

    public void clientResetScene() {
        //#ifdef SHOW_NUM_DEEP_PENETRATIONS
//		BulletGlobals.gNumDeepPenetrationChecks = 0;
//		BulletGlobals.gNumGjkChecks = 0;
        //#endif //SHOW_NUM_DEEP_PENETRATIONS

        int numObjects = 0;
        if (dyn != null) {
            dyn.stepSimulation(1f / 60f, 0);
            numObjects = dyn.getNumCollisionObjects();
        }

        for (int i = 0; i < numObjects; i++) {
            CollisionObject colObj = dyn.getCollisionObjectArray().get(i);
            RigidBody body = RigidBody.upcast(colObj);
            if (body != null) {
                if (body.getMotionState() != null) {
                    DefaultMotionState myMotionState = (DefaultMotionState) body.getMotionState();
                    myMotionState.graphicsWorldTrans.set(myMotionState.startWorldTrans);
                    colObj.setWorldTransform(myMotionState.graphicsWorldTrans);
                    colObj.setInterpolationWorldTransform(myMotionState.startWorldTrans);
                    colObj.activate();
                }
                // removed cached contact points
                dyn.getBroadphase().getOverlappingPairCache().cleanProxyFromPairs(colObj.getBroadphaseHandle(), getDyn().getDispatcher());

                body = RigidBody.upcast(colObj);
                if (body != null && !body.isStaticObject()) {
                    RigidBody.upcast(colObj).setLinearVelocity(new Vector3f(0f, 0f, 0f));
                    RigidBody.upcast(colObj).setAngularVelocity(new Vector3f(0f, 0f, 0f));
                }
            }

			/*
			//quickly search some issue at a certain simulation frame, pressing space to reset
			int fixed=18;
			for (int i=0;i<fixed;i++)
			{
			getDynamicsWorld()->stepSimulation(1./60.f,1);
			}
			*/
        }
    }

    public DynamicsWorld getDyn() {
        return dyn;
    }

    public void setCamUp(Vector3f camUp) {
        this.camUp.set(camUp);
    }

    public void setCameraForwardAxis(int axis) {
        forwardAxis = axis;
    }

    public Vector3f getCamPos() {
        return camPos;
    }

    public Vector3f getCamPosTarget() {
        return camPosTarget;
    }

    public boolean isIdle() {
        return idle;
    }

    public void setIdle(boolean idle) {
        this.idle = idle;
    }

    public void drawString(CharSequence s, int x, int y, Color3f color) {
        System.out.println(s); //HACK temporary
        glsrt.drawString();
    }

}
