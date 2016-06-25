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

package bulletphys.ui;

import bulletphys.collision.broadphase.BroadphaseInterface;
import bulletphys.collision.broadphase.SimpleBroadphase;
import bulletphys.collision.dispatch.CollisionDispatcher;
import bulletphys.collision.dispatch.CollisionObject;
import bulletphys.collision.dispatch.CollisionWorld;
import bulletphys.collision.dispatch.DefaultCollisionConfiguration;
import bulletphys.collision.shapes.CollisionShape;
import bulletphys.dynamics.DiscreteDynamicsWorld;
import bulletphys.dynamics.DynamicsWorld;
import bulletphys.dynamics.RigidBody;
import bulletphys.dynamics.RigidBodyConstructionInfo;
import bulletphys.dynamics.constraintsolver.ConstraintSolver;
import bulletphys.dynamics.constraintsolver.Point2PointConstraint;
import bulletphys.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import bulletphys.dynamics.constraintsolver.TypedConstraint;
import bulletphys.linearmath.*;
import bulletphys.util.BulletStack;
import bulletphys.util.Motion;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.math.FloatUtil;
import nars.gui.graph.Atomatter;
import nars.util.JoglSpace;
import org.jetbrains.annotations.NotNull;

import javax.vecmath.Color3f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import java.util.function.BiConsumer;

import static com.jogamp.opengl.math.FloatUtil.makeFrustum;

/**
 * @author jezek2
 */

public class JoglPhysics<X extends Atomatter> extends JoglSpace implements MouseListener, GLEventListener, KeyListener {


    private boolean simulating = true;


    /**
     * activate/deactivate the simulation; by default it is enabled
     */
    public void setSimulating(boolean simulating) {
        this.simulating = simulating;
    }

    protected final BulletStack stack = BulletStack.get();


    public RigidBody pickedBody = null; // for deactivation state


    protected final Clock clock = new Clock();

    // this is the most important class
    protected final @NotNull DynamicsWorld<X> dyn;

    // constraint for mouse picking
    protected TypedConstraint pickConstraint = null;
    protected RigidBody directDrag;


    protected int debug = 0;

    protected float ele = 20f;
    protected float azi = -180f;

    protected final Vector3f camPos = v(0f, 0f, 0f);
    protected final Vector3f camPosTarget = v(0f, 0f, 0f); // look at
    protected float cameraDistance = 55f;

    float top, bottom, nearPlane, tanFovV, tanFovH, fov, farPlane, left, right;


    protected final Vector3f camUp = v(0f, 1f, 0f);
    protected int forwardAxis = 2;

    protected int screenWidth = 0;
    protected int screenHeight = 0;


    protected boolean stepping = true;
    protected int lastKey;

    protected GLSRT glsrt = null;

    protected boolean useLight0 = true;
    protected boolean useLight1 = true;

    private int mouseDragPrevX, mouseDragPrevY;

    public JoglPhysics() {
        super();

        debug |= DebugDrawModes.NO_HELP_TEXT;

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

        dyn = new DiscreteDynamicsWorld<>(dispatcher, overlappingPairCache, constraintSolver, collision_config) {
            @Override
            protected boolean valid(CollisionObject<X> c) {
                return JoglPhysics.this.valid(c);
            }
        };

        //dyn =new SimpleDynamicsWorld(dispatcher, overlappingPairCache, constraintSolver, collision_config);
        dyn.setGravity(v(0, 0, 0));

    }

    /**
     * return false to remove this object during the beginning of the physics frame
     */
    protected boolean valid(CollisionObject<X> c) {
        return true;
    }

    @Override
    protected void init(GL2 gl2) {

        screenWidth = window.getWidth();
        screenHeight = window.getHeight();

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
        gl.glShadeModel(GL2.GL_LINE_SMOOTH); // Enable Smooth Shading
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
        gl.glEnable(gl.GL_COLOR_MATERIAL);

        gl.glEnable(gl.GL_DEPTH_TEST);
        gl.glDepthFunc(gl.GL_LESS);


        // JAU
        gl.glEnable(gl.GL_CULL_FACE);
        gl.glCullFace(gl.GL_BACK);

    }


    public final void reshape(GLAutoDrawable drawable,
                              int xstart,
                              int ystart,
                              int width,
                              int height) {

        height = (height == 0) ? 1 : height;
        screenWidth = width;
        screenHeight = height;


        updateCamera();

    }

    public void display(GLAutoDrawable drawable) {

        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);

        if (simulating) {
            // NOTE: simple dynamics world doesn't handle fixed-time-stepping
            float ms = Math.max(clock.getTimeMicroseconds(), 1000000f / 60f);

            clock.reset();

            dyn.stepSimulation(ms / 1000000.f);
        }

        // optional but useful: debug drawing
        //dyn.debugDrawWorld(debug);

        renderWorld();

    }

    public void displayChanged() {
    }


    @Override
    public void mouseWheelMoved(MouseEvent e) {

    }

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

        mouseDragPrevX = mouseDragPrevY = -1; //HACK todo do this on a per-button basis
    }

    //
    // MouseMotionListener
    //
    public void mouseDragged(MouseEvent e) {

        mouseMotionFunc(e.getX(), e.getY(), e.getButtonsDown());
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


    public void updateCamera() {
        stack.vectors.push();
        stack.matrices.push();
        stack.quats.push();

        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        float rele = ele * 0.01745329251994329547f; // rads per deg
        float razi = azi * 0.01745329251994329547f; // rads per deg

        Quat4f rot = stack.quats.get();
        QuaternionUtil.setRotation(rot, camUp, razi);

        Vector3f eyePos = stack.vectors.get(0f, 0f, 0f);
        VectorUtil.setCoord(eyePos, forwardAxis, -cameraDistance);

        Vector3f forward = stack.vectors.get(eyePos.x, eyePos.y, eyePos.z);
        if (forward.lengthSquared() < ExtraGlobals.FLT_EPSILON) {
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

        //gl.glFrustumf(-1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 10000.0f);
        //glu.gluPerspective(45, (float) screenWidth / screenHeight, 4, 2000);
        perspective(0, true, 45 * FloatUtil.PI / 180.0f, (float) screenWidth / screenHeight, 4, 500);


        glu.gluLookAt(camPos.x, camPos.y, camPos.z,
                camPosTarget.x, camPosTarget.y, camPosTarget.z,
                camUp.x, camUp.y, camUp.z);


        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();
        stack.vectors.pop();
        stack.matrices.pop();
        stack.quats.pop();

    }

    private final float[] matTmp = new float[16];

    void perspective(final int m_off, final boolean initM,
                     final float fovy_rad, final float aspect, final float zNear, final float zFar) throws GLException {
        this.top = (float) Math.tan(fovy_rad / 2f) * zNear; // use tangent of half-fov !
        this.bottom = -1.0f * top;    //          -1f * fovhvTan.top * zNear
        left = aspect * bottom; // aspect * -1f * fovhvTan.top * zNear
        right = aspect * top;    // aspect * fovhvTan.top * zNear
        nearPlane = zNear;
        farPlane = zFar;
        tanFovV = (top - bottom) * 0.5f / zNear;
        tanFovH = (right - left) * 0.5f / zNear;
        fov = 2f * (float) Math.atan(tanFovV);
        gl.glMultMatrixf(
                makeFrustum(matTmp, m_off, initM, left, right, bottom, top, zNear, zFar),
                0
        );
    }

    private static final float STEPSIZE = 5;

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
                    ExtraGlobals.gDisableDeactivation = true;
                } else {
                    ExtraGlobals.gDisableDeactivation = false;
                }
                break;

            case 'o': {
                stepping = !stepping;
                break;
            }
            case 's':
                break;
            //    case ' ' : newRandom(); break;

            case '1': {
                if ((debug & DebugDrawModes.ENABLE_CCD) != 0) {
                    debug = debug & (~DebugDrawModes.ENABLE_CCD);
                } else {
                    debug |= DebugDrawModes.ENABLE_CCD;
                }
                break;
            }


            default:
                // std::cout << "unused key : " << key << std::endl;
                break;
        }

//        if (getDyn() != null && getDyn().debugDrawer != null) {
//            getDyn().debugDrawer.setDebugMode(debug);
//        }

        //LWJGL.postRedisplay();
    }

    public int getDebug() {
        return debug;
    }

    public void setDebug(int mode) {
        debug = mode;
//        if (getDyn() != null && getDyn().debugDrawer != null) {
//            getDyn().debugDrawer.setDebugMode(mode);
//        }
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
                    CollisionObject<X> obj = getDyn().objects().get(numObj - 1);

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

            default:
                // std::cout << "unused (special) key : " << key << std::endl;
                break;
        }
    }

//    public void shootBox(Vector3f destination) {
//        if (dyn != null) {
//            float mass = 10f;
//            Transform startTransform = new Transform();
//            startTransform.setIdentity();
//            Vector3f camPos = v(getCamPos());
//            startTransform.origin.set(camPos);
//
//            if (shootBoxShape == null) {
//                //#define TEST_UNIFORM_SCALING_SHAPE 1
//                //#ifdef TEST_UNIFORM_SCALING_SHAPE
//                //btConvexShape* childShape = new btBoxShape(btVector3(1.f,1.f,1.f));
//                //m_shootBoxShape = new btUniformScalingShape(childShape,0.5f);
//                //#else
//                shootBoxShape = new BoxShape(v(1f, 1f, 1f));
//                //#endif//
//            }
//
//            RigidBody body = this.newBody(mass, startTransform, shootBoxShape);
//
//            Vector3f linVel = v(destination.x - camPos.x, destination.y - camPos.y, destination.z - camPos.z);
//            linVel.normalize();
//            linVel.scale(ShootBoxInitialSpeed);
//
//            Transform ct = new Transform();
//            ct.origin.set(camPos);
//            ct.setRotation(new Quat4f(0f, 0f, 0f, 1f));
//            body.setWorldTransform(ct);
//
//            body.setLinearVelocity(linVel);
//            body.setAngularVelocity(v(0f, 0f, 0f));
//        }
//    }

    public Vector3f rayTo(int x, int y) {


        Vector3f rayFrom = v(getCamPos());
        Vector3f rayForward = v();
        rayForward.sub(getCamPosTarget(), getCamPos());
        rayForward.normalize();
        rayForward.scale(farPlane);

        //Vector3f rightOffset = new Vector3f();
        Vector3f vertical = v(camUp);

        Vector3f hor = v();
        // TODO: check: hor = rayForward.cross(vertical);
        hor.cross(rayForward, vertical);
        hor.normalize();
        // TODO: check: vertical = hor.cross(rayForward);
        vertical.cross(hor, rayForward);
        vertical.normalize();

        hor.scale(2f * farPlane * tanFovH);
        vertical.scale(2f * farPlane * tanFovV);

        Vector3f rayToCenter = v();
        rayToCenter.add(rayFrom, rayForward);

        Vector3f dHor = v(hor);
        dHor.scale(1f / (float) screenWidth);

        Vector3f dVert = v(vertical);
        dVert.scale(1.f / (float) screenHeight);

        Vector3f tmp1 = v();
        Vector3f tmp2 = v();
        tmp1.scale(0.5f, hor);
        tmp2.scale(0.5f, vertical);

        Vector3f rayTo = v();
        rayTo.sub(rayToCenter, tmp1);
        rayTo.add(tmp2);

        tmp1.scale(x, dHor);
        tmp2.scale(y, dVert);

        rayTo.add(tmp1);
        rayTo.sub(tmp2);
        return rayTo;
    }

    public static Vector3f v(Vector3f copied) {
        return new Vector3f(copied);
    }

    private void mouseClick(int button, int x, int y) {


        switch (button) {
            case MouseEvent.BUTTON3: {
                CollisionWorld.ClosestRayResultCallback c = mousePick(x, y);
                if (c.hasHit()) {
                    CollisionObject co = c.collisionObject;
                    System.out.println("zooming to " + co);
                    camPosTarget.set(co.getWorldOrigin());
                    cameraDistance = co.getCollisionShape().getBoundingRadius() * 1.5f + nearPlane;
                }
            }
            break;

//            case MouseEvent.BUTTON3: {
//                shootBox(rayTo);
//                break;
//            }
//            case MouseEvent.BUTTON2: {
//                // apply an impulse
//
//                Vector3f rayTo = v(rayTo(x, y));
//                CollisionWorld.ClosestRayResultCallback rayCallback = new CollisionWorld.ClosestRayResultCallback(camPos, rayTo);
//
//                dyn.rayTest(camPos, rayTo, rayCallback);
//                if (rayCallback.hasHit()) {
//                    RigidBody body = RigidBody.upcast(rayCallback.collisionObject);
//                    if (body != null) {
//                        body.setActivationState(CollisionObject.ACTIVE_TAG);
//                        Vector3f impulse = v(rayTo);
//                        impulse.normalize();
//                        float impulseStrength = 10f;
//                        impulse.scale(impulseStrength);
//                        Vector3f relPos = v();
//
//                        relPos.sub(rayCallback.hitPointWorld, body.getCenterOfMassPosition(v()));
//                        body.applyImpulse(impulse, relPos);
//                    }
//                }
//
//                break;
//            }
        }
    }

    private void pickConstrain(int button, int state, int x, int y) {

        switch (button) {
            case MouseEvent.BUTTON1: {

                if (state == 1) {
                    mouseGrabOn(x,y);
                } else {
                    mouseGrabOff();
                }
                break;
            }
            case MouseEvent.BUTTON2: {
                break;
            }
            case MouseEvent.BUTTON3: {
                break;
            }
        }
    }

    private void mouseGrabOff() {
        if (pickConstraint != null) {
            dyn.removeConstraint(pickConstraint);
            pickConstraint = null;

            pickedBody.forceActivationState(CollisionObject.ACTIVE_TAG);
            pickedBody.setDeactivationTime(0f);
            pickedBody = null;
        }

        if (directDrag != null) {
            Object u = directDrag.getUserPointer();

            System.out.println("UNDRAG: " + directDrag);

            if (u instanceof Atomatter) {
                ((Atomatter) u).motionLock(false);
            }

            directDrag = null;
        }
    }

    final Vector3f gOldPickingPos = v();
    float gOldPickingDist = 0.f;

    private void mouseGrabOn(int sx, int sy) {
        // add a point to point constraint for picking
        CollisionWorld.ClosestRayResultCallback rayCallback = mousePick(sx, sy);

        if (rayCallback.hasHit()) {
            RigidBody body = RigidBody.upcast(rayCallback.collisionObject);
            if (body != null) {

                body.setActivationState(CollisionObject.DISABLE_DEACTIVATION);
                Vector3f pickPos = v(rayCallback.hitPointWorld);

                Transform tmpTrans = body.getCenterOfMassTransform(new Transform());
                tmpTrans.inverse();
                Vector3f localPivot = v(pickPos);
                tmpTrans.transform(localPivot);
                // save mouse position for dragging
                gOldPickingPos.set(rayCallback.rayToWorld);
                Vector3f eyePos = v(camPos);
                Vector3f tmp = v();
                tmp.sub(pickPos, eyePos);
                gOldPickingDist = tmp.length();


                // other exclusions?
                if (!(body.isStaticObject() || body.isKinematicObject())) {
                    pickedBody = body;


                    Point2PointConstraint p2p = new Point2PointConstraint(body, localPivot);
                    dyn.addConstraint(p2p);
                    pickConstraint = p2p;

                    // very weak constraint for picking
                    p2p.setting.tau = 0.1f;
                } else {
                    if (directDrag == null) {
                        directDrag = body;

                    }
                }

            }
        }
        //}
    }

    public CollisionWorld.ClosestRayResultCallback mousePick(int sx, int sy) {
        return mousePick(v(rayTo(sx, sy)));
    }

    public CollisionWorld.ClosestRayResultCallback mousePick(Vector3f rayTo) {
        CollisionWorld.ClosestRayResultCallback rayCallback = new CollisionWorld.ClosestRayResultCallback(camPos, rayTo);

        rayCallback.collisionFilterGroup = (1 << 7);

        dyn.rayTest(camPos, rayTo, rayCallback);
        return rayCallback;
    }

    private void mouseMotionFunc(int x, int y, short[] buttons) {
        if ((pickConstraint != null) || (directDrag != null)) {

            // keep it at the same picking distance
            Vector3f newRayTo = v(rayTo(x, y));
            Vector3f eyePos = v(camPos);
            Vector3f dir = v();
            dir.sub(newRayTo, eyePos);
            dir.normalize();
            dir.scale(gOldPickingDist);

            Vector3f newPos = v();
            newPos.add(eyePos, dir);

            if (directDrag != null) {
                //directly move the 'static' object

                Object u = directDrag.getUserPointer();

                //System.out.println("DRAG: " + directDrag + " " + u + " -> " + newPos);

                if (u instanceof Atomatter) {
                    ((Atomatter) u).motionLock(true);
                }

                MotionState mm = directDrag.getMotionState();
                if (mm instanceof Motion) {
                    ((Motion) mm).center(newPos);
                }

            } else if (pickConstraint != null) {
                // move the constraint pivot
                Point2PointConstraint p2p = (Point2PointConstraint) pickConstraint;
                p2p.setPivotB(newPos);
            }
        } else {

            float dx, dy;
            if (mouseDragPrevX >= 0) {
                dx = (x) - mouseDragPrevX;
                dy = (y) - mouseDragPrevY;


                ///only if ALT key is pressed (Maya style)
                //            if (m_modifierKeys & BT_ACTIVE_ALT)
                //            {
                //                if (m_mouseButtons & 4) {
                //                    btVector3 hor = getRayTo(0, 0) - getRayTo(1, 0);
                //                    btVector3 vert = getRayTo(0, 0) - getRayTo(0, 1);
                //                    btScalar multiplierX = btScalar(0.001);
                //                    btScalar multiplierY = btScalar(0.001);
                //                    if (m_ortho) {
                //                        multiplierX = 1;
                //                        multiplierY = 1;
                //                    }
                //
                //
                //                    m_cameraTargetPosition += hor * dx * multiplierX;
                //                    m_cameraTargetPosition += vert * dy * multiplierY;
                //                }
                //            }
                {

                    for (short b : buttons) {
                        switch (b) {
                            case 3:
                                //right mouse
                                //                        nextAzi += dx * btScalar(0.2);
                                //                        nextAzi = fmodf(nextAzi, btScalar(360.f));
                                //                        nextEle += dy * btScalar(0.2);
                                //                        nextEle = fmodf(nextEle, btScalar(180.f));
                                azi += dx * 0.2f;
                                //nextAzi = fmodf(nextAzi, btScalar(360.f));
                                ele += dy * (0.2f);
                                //nextEle = fmodf(nextEle, btScalar(180.f));
                                break;
                            case 2:
                                //middle mouse
                                cameraDistance -= dy * 0.15f;
                                final float minCameraDistance = nearPlane;
                                if (cameraDistance < minCameraDistance)
                                    cameraDistance = minCameraDistance; //limit

                                //                        nextDist -= dy * btScalar(0.01f);
                                //                        if (nextDist < minDist)
                                //                            nextDist = minDist;
                                break;
                        }
                    }
                }
            }

            mouseDragPrevX = x;
            mouseDragPrevY = y;
        }
    }

    public static Vector3f v() {
        return new Vector3f();
    }

    /**
     * Bullet's global variables and constants.
     *
     * @author jezek2
     */
    public static class ExtraGlobals {

        public static final boolean DEBUG = true;


        public static final float FLT_EPSILON = 1.19209290e-07f;
        public static final float SIMD_EPSILON = FLT_EPSILON;

        public static final float SIMD_2_PI = 6.283185307179586232f;
        public static final float SIMD_PI = SIMD_2_PI * 0.5f;
        public static final float SIMD_HALF_PI = SIMD_2_PI * 0.25f;


        public static boolean gDisableDeactivation = false;


//        static {
//            if (ENABLE_PROFILE) {
//                Runtime.getRuntime().addShutdownHook(new Thread() {
//                    @Override
//                    public void run() {
//                        printProfiles();
//                    }
//                });
//            }
//        }

//        private static class ProfileBlock {
//            public String name;
//            public long startTime;
//        }

    }

    public RigidBody newBody(float mass, Transform startTransform, CollisionShape shape) {
        Motion myMotionState = new Motion(startTransform);

        boolean isDynamic = (mass != 0f);
        int collisionFilterGroup = isDynamic ? 1 : 2;
        int collisionFilterMask = isDynamic ? -1 : -3;

        return newBody(mass, shape, myMotionState, collisionFilterGroup, collisionFilterMask);
    }


    public RigidBody newBody(float mass, CollisionShape shape, MotionState motion, int group, int mask) {
        // rigidbody is dynamic if and only if mass is non zero, otherwise static
        boolean isDynamic = (mass != 0f);
        Vector3f localInertia = v(0, 0, 0);
        if (isDynamic) {
            shape.calculateLocalInertia(mass, localInertia);
        }

        RigidBodyConstructionInfo c = new RigidBodyConstructionInfo(mass, motion, shape, localInertia);

        RigidBody body = new RigidBody(c);

        ((DiscreteDynamicsWorld) dyn).addRigidBody(body, (short) group, (short) mask);


        return body;
    }

    public static Vector3f v(float a, float b, float c) {
        return new Vector3f(a, b, c);
    }

//    // See http://www.lighthouse3d.com/opengl/glut/index.php?bmpfontortho
//    public void setOrthographicProjection() {
//        // switch to projection mode
//        gl.glMatrixMode(gl.GL_PROJECTION);
//        // save previous matrix which contains the
//        //settings for the perspective projection
//        // gl.glPushMatrix();
//        // reset matrix
//        gl.glLoadIdentity();
//        // set a 2D orthographic projection
//        glu.gluOrtho2D(0f, screenWidth, 0f, screenHeight);
//        // invert the y axis, down is positive
//        gl.glScalef(1f, -1f, 1f);
//        // mover the origin from the bottom left corner
//        // to the upper left corner
//        gl.glTranslatef(0f, -screenHeight, 0f);
//        gl.glMatrixMode(gl.GL_MODELVIEW);
//    }




    public void renderWorld() {
        updateCamera();
        dyn.objects().forEach(this::render);
    }

    public static final BiConsumer<GL2,RigidBody> defaultRenderer = (gl, body) -> {

        gl.glPushMatrix();
        ShapeDrawer.transform(gl, body.transform());
        ShapeDrawer.draw(gl, body);
        gl.glPopMatrix();

/*
//                if (body != null && body.getMotionState() != null) {
//                    Motion myMotionState = (Motion) body.getMotionState();
//                    m.set(myMotionState.t);
//                } else {
//                    body.getWorldTransform(m);
//                }
 */

    };

    public final void render(CollisionObject<X> c) {
        RigidBody<X> body = RigidBody.upcast(c);
        if (body != null) {
            BiConsumer<GL2,RigidBody> r = body.renderer();
            if (r != null)
                r.accept(gl, body);
        }
    }

//    public void clientResetScene() {
//        //#ifdef SHOW_NUM_DEEP_PENETRATIONS
////		BulletGlobals.gNumDeepPenetrationChecks = 0;
////		BulletGlobals.gNumGjkChecks = 0;
//        //#endif //SHOW_NUM_DEEP_PENETRATIONS
//
//        int numObjects = 0;
//        if (dyn != null) {
//            dyn.stepSimulation(1f / 60f, 0);
//            numObjects = dyn.getNumCollisionObjects();
//        }
//
//        for (int i = 0; i < numObjects; i++) {
//            CollisionObject colObj = dyn.getCollisionObjectArray().get(i);
//            RigidBody body = RigidBody.upcast(colObj);
//            if (body != null) {
//                if (body.getMotionState() != null) {
//                    Motion myMotionState = (Motion) body.getMotionState();
//                    myMotionState.graphicsWorldTrans.set(myMotionState.startWorldTrans);
//                    colObj.setWorldTransform(myMotionState.graphicsWorldTrans);
//                    colObj.setInterpolationWorldTransform(myMotionState.startWorldTrans);
//                    colObj.activate();
//                }
//                // removed cached contact points
//                dyn.getBroadphase().getOverlappingPairCache().cleanProxyFromPairs(colObj.getBroadphaseHandle(), getDyn().getDispatcher());
//
//                body = RigidBody.upcast(colObj);
//                if (body != null && !body.isStaticObject()) {
//                    RigidBody.upcast(colObj).setLinearVelocity(v(0f, 0f, 0f));
//                    RigidBody.upcast(colObj).setAngularVelocity(v(0f, 0f, 0f));
//                }
//            }
//
//			/*
//            //quickly search some issue at a certain simulation frame, pressing space to reset
//			int fixed=18;
//			for (int i=0;i<fixed;i++)
//			{
//			getDynamicsWorld()->stepSimulation(1./60.f,1);
//			}
//			*/
//        }
//    }

    public DynamicsWorld<X> getDyn() {
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


    public void drawString(CharSequence s, int x, int y, Color3f color) {
        System.out.println(s); //HACK temporary
        glsrt.drawString();
    }

}


//GLShapeDrawer.drawCoordSystem(gl);

//            if (false) {
//                System.err.println("++++++++++++++++++++++++++++++++");
//                System.err.println("++++++++++++++++++++++++++++++++");
//                try {
//                    Thread.sleep(2000);
//                } catch (Exception e) {
//                }
//            }

//            float xOffset = 10f;
//            float yStart = 20f;
//            float yIncr = 20f;

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


//    public void renderObject(CollisionObject colObj) {

//        if (0 == i) {
//            wireColor.set(0.5f, 1f, 0.5f); // wants deactivation
//        } else {
//            wireColor.set(1f, 1f, 0.5f); // wants deactivation
//        }
//        if ((i & 1) != 0) {
//            wireColor.set(0f, 0f, 1f);
//        }

//        // color differently for active, sleeping, wantsdeactivation states
//        if (colObj.getActivationState() == 1) // active
//        {
//            if ((i & 1) != 0) {
//                //wireColor.add(new Vector3f(1f, 0f, 0f));
//                wireColor.x += 1f;
//            } else {
//                //wireColor.add(new Vector3f(0.5f, 0f, 0f));
//                wireColor.x += 0.5f;
//            }
//        }
//        if (colObj.getActivationState() == 2) // ISLAND_SLEEPING
//        {
//            if ((i & 1) != 0) {
//                //wireColor.add(new Vector3f(0f, 1f, 0f));
//                wireColor.y += 1f;
//            } else {
//                //wireColor.add(new Vector3f(0f, 0.5f, 0f));
//                wireColor.y += 0.5f;
//            }
//        }

//    }
