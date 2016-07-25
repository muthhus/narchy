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

package spacegraph.render;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.math.FloatUtil;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import spacegraph.SimpleSpatial;
import spacegraph.Spatial;
import spacegraph.math.Color3f;
import spacegraph.math.Matrix3f;
import spacegraph.math.Quat4f;
import spacegraph.math.v3;
import spacegraph.phys.*;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.phys.collision.DefaultCollisionConfiguration;
import spacegraph.phys.collision.DefaultIntersecter;
import spacegraph.phys.collision.broad.Broadphase;
import spacegraph.phys.collision.broad.Intersecter;
import spacegraph.phys.collision.broad.SimpleBroadphase;
import spacegraph.phys.constraint.Point2PointConstraint;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.phys.math.*;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.solve.Constrainer;
import spacegraph.phys.solve.SequentialImpulseConstrainer;
import spacegraph.phys.util.AnimFloat;
import spacegraph.phys.util.AnimFloatAngle;
import spacegraph.phys.util.AnimVector3f;
import spacegraph.phys.util.Motion;

import java.util.function.BiConsumer;

import static com.jogamp.opengl.math.FloatUtil.makeFrustum;
import static spacegraph.math.v3.v;

/**
 * @author jezek2
 */

public class JoglPhysics<X extends Spatial> extends JoglSpace implements MouseListener, GLEventListener, KeyListener {


    private boolean simulating = true;
    private int mouseDragDX, mouseDragDY;


    /**
     * activate/deactivate the simulation; by default it is enabled
     */
    public void setSimulating(boolean simulating) {
        this.simulating = simulating;
    }

    //protected final BulletStack stack = BulletStack.get();


    public Dynamic pickedBody = null; // for deactivation state


    protected final Clock clock = new Clock();

    // this is the most important class
    public final @NotNull Dynamics<X> dyn;

    // constraint for mouse picking
    protected TypedConstraint pickConstraint = null;
    protected Dynamic directDrag;


    protected int debug = 0;


    protected final v3 camPos = new v3();
    protected final v3 camPosTarget;
    protected final v3 camDir = new v3();
    protected final v3 camUp;
    protected final MutableFloat cameraDistance;
    protected final MutableFloat ele;
    protected final MutableFloat azi;
    float top, bottom, nearPlane, tanFovV, tanFovH, fov, farPlane, left, right;

    int zNear = 1;
    int zFar = 500;


    final ClosestRay rayCallback = new ClosestRay(((short)(1 << 7)));

    protected int forwardAxis = 2;

    protected int screenWidth = 0;
    protected int screenHeight = 0;


    protected boolean stepping = true;
    protected int lastKey;

    protected GLSRT glsrt = null;

    protected boolean useLight0 = true;
    //protected boolean useLight1 = true;

    private int mouseDragPrevX, mouseDragPrevY;

    public JoglPhysics() {
        super();

        debug |= DebugDrawModes.NO_HELP_TEXT;

        // Setup the basic world
        DefaultCollisionConfiguration collision_config = new DefaultCollisionConfiguration();

        Intersecter dispatcher = new DefaultIntersecter(collision_config);

        //btPoint3 worldAabbMin(-10000,-10000,-10000);
        //btPoint3 worldAabbMax(10000,10000,10000);
        //btBroadphaseInterface* overlappingPairCache = new btAxisSweep3 (worldAabbMin, worldAabbMax);
        Broadphase overlappingPairCache = new SimpleBroadphase();


        Constrainer constrainer = new SequentialImpulseConstrainer();

        dyn = new DiscreteDynamics<>(dispatcher, overlappingPairCache, constrainer, collision_config);

        cameraDistance = new AnimFloat(55f, dyn, 4f);
        azi = new AnimFloatAngle(-180, dyn, 30f);
        ele = new AnimFloatAngle(20, dyn, 30f);

        camPosTarget = new AnimVector3f(0,0,0,dyn, 10f);
        camUp = new v3(0, 1, 0); //new AnimVector3f(0f, 1f, 0f, dyn, 1f);

        dyn.setGravity(v(0, 0, 0));

    }

    /**
     * return false to remove this object during the beginning of the physics frame
     */
    protected boolean valid(int nextID, Collidable<X> c) {
        return true;
    }

    @Override
    protected void init(GL2 gl2) {

        screenWidth = window.getWidth();
        screenHeight = window.getHeight();

        window.addMouseListener(this);
        window.addKeyListener(this);

        gl.glLightModelf(GL2.GL_LIGHT_MODEL_AMBIENT, 0.6f);

        float[] light_ambient = new float[]{0.3f, 0.3f, 0.3f, 1.0f};
        float[] light_diffuse = new float[]{0.5f, 0.5f, 0.5f, 0.5f};
        //float[] light_specular = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
        float[] light_specular = new float[]{0.5f, 0.5f, 0.5f, 0.5f};
        /* light_position is NOT default value */

        float distance = 25f;
        float[] light_position0 = new float[]{0f, 0f, distance, 0.0f};

        //float[] light_position1 = new float[]{-1.0f, -10.0f, -1.0f, 0.0f};

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


//        if (useLight1) {
//            gl.glLightfv(gl.GL_LIGHT1, gl.GL_AMBIENT, light_ambient, 0);
//            gl.glLightfv(gl.GL_LIGHT1, gl.GL_DIFFUSE, light_diffuse, 0);
//            gl.glLightfv(gl.GL_LIGHT1, gl.GL_SPECULAR, light_specular, 0);
//            gl.glLightfv(gl.GL_LIGHT1, gl.GL_POSITION, light_position1, 0);
//        }

        if (useLight0) {
            gl.glEnable(gl.GL_LIGHTING);
        }
        if (useLight0) {
            gl.glEnable(gl.GL_LIGHT0);
        }
//        if (useLight1) {
//            gl.glEnable(gl.GL_LIGHT1);
//        }

        gl.glShadeModel(gl.GL_SMOOTH);
        gl.glShadeModel(GL2.GL_LINE_SMOOTH); // Enable Smooth Shading
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);

        //https://www.sjbaker.org/steve/omniv/opengl_lighting.html
        gl.glColorMaterial ( GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE ) ;
        gl.glEnable(gl.GL_COLOR_MATERIAL);

        //gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, new float[] { 1, 1, 1, 1 }, 0);
        //gl.glMaterialfv( GL2.GL_FRONT_AND_BACK, GL2.GL_EMISSION, new float[] { 0, 0, 0, 0 }, 0);

        gl.glEnable(gl.GL_DEPTH_TEST);
        //gl.glDepthFunc(gl.GL_LESS);
        gl.glDepthFunc(gl.GL_LEQUAL);


        // JAU
        //gl.glEnable(gl.GL_CULL_FACE);
        //gl.glCullFace(gl.GL_BACK);

    }


    public final void reshape(GLAutoDrawable drawable,
                              int xstart,
                              int ystart,
                              int width,
                              int height) {

        height = (height == 0) ? 1 : height;
        screenWidth = width;
        screenHeight = height;


        //updateCamera();

    }

    public void display(GLAutoDrawable drawable) {


        gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
        gl.glEnable(GL2.GL_DEPTH_TEST);

        if (simulating) {
            // NOTE: SimpleDynamics world doesn't handle fixed-time-stepping
            dyn.stepSimulation(
                    Math.max(clock.getTimeThenReset(), 1000000f / 60f) / 1000000.f
                    //clock.getTimeThenReset()
            );
        }

        updateCamera();
        dyn.objects().forEach(this::render);
    }


    public void mouseClicked(MouseEvent e) {
        //overriden: called by mouseRelease
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        mouseDragDX = mouseDragDY = 0; //clear drag
        mouseDragPrevX = mouseDragPrevY = -1;
    }

    public void mousePressed(MouseEvent e) {
        mouseDragDX = mouseDragDY = 0;

        int x = e.getX();
        int y = e.getY();
        if (!mouseMotionFunc(x, y, e.getButtonsDown())) {
            pickConstrain(e.getButton(), 1, x, y);
        }
    }

    public void mouseReleased(MouseEvent e) {
        int dragThresh = 1;
        if (Math.abs(mouseDragDX) < dragThresh && mouseClick(e.getButton(), e.getX(), e.getY()))
            return;

        pickConstrain(e.getButton(), 0, e.getX(), e.getY());

        mouseDragPrevX = mouseDragPrevY = -1; //HACK todo do this on a per-button basis
    }

    //
    // MouseMotionListener
    //
    public void mouseDragged(MouseEvent e) {

        int x = e.getX();
        int y = e.getY();

        if (mouseDragPrevX >= 0) {
            mouseDragDX = (x) - mouseDragPrevX;
            mouseDragDY = (y) - mouseDragPrevY;
        }

        mouseMotionFunc(x, y, e.getButtonsDown());

        mouseDragPrevX = x;
        mouseDragPrevY = y;
    }

    public void mouseMoved(MouseEvent e) {
        mouseMotionFunc(e.getX(), e.getY(), e.getButtonsDown());
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
        final float minCameraDistance = nearPlane;
        cameraDistance.setValue(Math.max(minCameraDistance, dist));
    }

    final Matrix3f tmpMat1 = new Matrix3f(); //stack.matrices.get();
    final Matrix3f tmpMat2 = new Matrix3f(); //stack.matrices.get();
    final Quat4f roll = new Quat4f(); //stack.quats.get();
    final Quat4f rot = new Quat4f(); //stack.quats.get();

    public synchronized void updateCamera() {
//        stack.vectors.push();
//        stack.matrices.push();
//        stack.quats.push();

        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        float rele = ele.floatValue() * 0.01745329251994329547f; // rads per deg
        float razi = azi.floatValue() * 0.01745329251994329547f; // rads per deg

        QuaternionUtil.setRotation(rot, camUp, razi);

        v3 eyePos = v();
        VectorUtil.setCoord(eyePos, forwardAxis, -cameraDistance.floatValue());

        v3 forward = v(eyePos.x, eyePos.y, eyePos.z);
        if (forward.lengthSquared() < ExtraGlobals.FLT_EPSILON) {
            forward.set(1f, 0f, 0f);
        }

        v3 camRight = v();
        camRight.cross(camUp, forward);
        camRight.normalize();
        QuaternionUtil.setRotation(roll, camRight, -rele);


        tmpMat1.set(rot);
        tmpMat2.set(roll);
        tmpMat1.mul(tmpMat2);
        tmpMat1.transform(eyePos);

        camPos.set(eyePos);

        //gl.glFrustumf(-1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 10000.0f);
        //glu.gluPerspective(45, (float) screenWidth / screenHeight, 4, 2000);
        perspective(0, true, 45 * FloatUtil.PI / 180.0f, (float) screenWidth / screenHeight, zNear, zFar);


        camDir.sub(camPosTarget, camPos);
        camDir.normalize();

        glu.gluLookAt(camPos.x, camPos.y, camPos.z,
                camPosTarget.x, camPosTarget.y, camPosTarget.z,
                camUp.x, camUp.y, camUp.z);


        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();
//        stack.vectors.pop();
//        stack.matrices.pop();
//        stack.quats.pop();

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
//                int numObj = getDyn().getNumCollisionObjects();
//                if (numObj != 0) {
//                    CollisionObject<X> obj = getDyn().objects().get(numObj - 1);
//
//                    getDyn().removeCollisionObject(obj);
//                    RigidBody body = RigidBody.upcast(obj);
//                    if (body != null && body.getMotionState() != null) {
//                        //delete body->getMotionState();
//                    }
//                    //delete obj;
//                }
                break;
            }
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

    public v3 rayTo(int x, int y) {


        v3 rayFrom = v(camPos);
        v3 rayForward = v();
        rayForward.sub(camPosTarget, camPos);
        rayForward.normalize();
        rayForward.scale(farPlane);

        //Vector3f rightOffset = new Vector3f();
        v3 vertical = v(camUp);

        v3 hor = v();
        // TODO: check: hor = rayForward.cross(vertical);
        hor.cross(rayForward, vertical);
        hor.normalize();
        // TODO: check: vertical = hor.cross(rayForward);
        vertical.cross(hor, rayForward);
        vertical.normalize();

        hor.scale(2f * farPlane * tanFovH);
        vertical.scale(2f * farPlane * tanFovV);

        v3 rayToCenter = v();
        rayToCenter.add(rayFrom, rayForward);

        v3 dHor = v(hor);
        dHor.scale(1f / (float) screenWidth);

        v3 dVert = v(vertical);
        dVert.scale(1.f / (float) screenHeight);

        v3 tmp1 = v();
        v3 tmp2 = v();
        tmp1.scale(0.5f, hor);
        tmp2.scale(0.5f, vertical);

        v3 rayTo = v();
        rayTo.sub(rayToCenter, tmp1);
        rayTo.add(tmp2);

        tmp1.scale(x, dHor);
        tmp2.scale(y, dVert);

        rayTo.add(tmp1);
        rayTo.sub(tmp2);
        return rayTo;
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        //System.out.println("wheel=" + Arrays.toString(e.getRotation()));
        float y = e.getRotation()[1];
        if (y!=0) {
            setCameraDistance( cameraDistance.floatValue() + 0.1f * y );
        }
    }

    private boolean mouseClick(int button, int x, int y) {

        switch (button) {
            case MouseEvent.BUTTON3: {
                ClosestRay c = mousePick(x, y);
                if (c.hasHit()) {
                    Collidable co = c.collidable;
                    System.out.println("zooming to " + co);

                    //TODO compute new azi and ele that match the current viewing angle values by backcomputing the vector delta


                    v3 objTarget = co.getWorldOrigin();
                    camPosTarget.set(objTarget);

                    setCameraDistance(
                        co.shape().getBoundingRadius() * 1.25f + nearPlane * 1.25f
                    );


                }
            }
            return true;


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
        return false;
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

            pickedBody.forceActivationState(Collidable.ACTIVE_TAG);
            pickedBody.setDeactivationTime(0f);
            pickedBody = null;
        }

        if (directDrag != null) {
            Object u = directDrag.getUserPointer();

            //System.out.println("UNDRAG: " + directDrag);

            if (u instanceof SimpleSpatial) {
                ((SimpleSpatial) u).motionLock(false);
            }

            directDrag = null;
        }
    }

    final v3 gOldPickingPos = v();
    float gOldPickingDist = 0.f;

    private void mouseGrabOn(int sx, int sy) {
        // add a point to point constraint for picking
        ClosestRay rayCallback = mousePick(sx, sy);

        if (rayCallback.hasHit()) {
            Dynamic body = Dynamic.ifDynamic(rayCallback.collidable);
            if (body != null) {

                body.setActivationState(Collidable.DISABLE_DEACTIVATION);
                v3 pickPos = v(rayCallback.hitPointWorld);

                Transform tmpTrans = body.getCenterOfMassTransform(new Transform());
                tmpTrans.inverse();
                v3 localPivot = v(pickPos);
                tmpTrans.transform(localPivot);
                // save mouse position for dragging
                gOldPickingPos.set(rayCallback.rayToWorld);
                v3 eyePos = v(camPos);
                v3 tmp = v();
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

    public ClosestRay mousePick(int sx, int sy) {
        return mousePick(v(rayTo(sx, sy)));
    }

    public ClosestRay mousePick(v3 rayTo) {
        ClosestRay r = this.rayCallback;
        v3 camPos = this.camPos;
        dyn.rayTest(camPos, rayTo, r.set(camPos, rayTo));
        return r;
    }

    private boolean mouseMotionFunc(int x, int y, short[] buttons) {

        v3 ray = v(rayTo(x, y));

        //if (mouseDragDX == 0) { //if not already dragging somewhere "outside"

            ClosestRay cray = mousePick(ray);

            /*System.out.println(mouseTouch.collisionObject + " touched with " +
                Arrays.toString(buttons) + " at " + mouseTouch.hitPointWorld
            );*/

            if (cray.collidable != null) {
                Object t = cray.collidable.getUserPointer();
                if (t instanceof Spatial) {
                    Spatial a = ((Spatial) t);
                    if (a.onTouch(cray.collidable, cray, buttons)) {
                        //absorbed
                        mouseDragDX = mouseDragDY = 0;
                        mouseDragPrevX = mouseDragPrevY = -1; //cancel any drag being enabled
                        return true;
                    }
                }
            }
        //}

        if ((pickConstraint != null) || (directDrag != null)) {

            // keep it at the same picking distance
            v3 eyePos = v(camPos);
            v3 dir = v();
            dir.sub(ray, eyePos);
            dir.normalize();
            dir.scale(gOldPickingDist);

            v3 newPos = v();
            newPos.add(eyePos, dir);

            if (directDrag != null) {
                //directly move the 'static' object

                Object u = directDrag.getUserPointer();

                //System.out.println("DRAG: " + directDrag + " " + u + " -> " + newPos);

                if (u instanceof SimpleSpatial) {
                    ((SimpleSpatial) u).motionLock(true);
                }

                MotionState mm = directDrag.getMotionState();
                if (mm instanceof Motion) {
                    ((Motion) mm).center(newPos);
                }

                return true;
            } else if (pickConstraint != null) {
                // move the constraint pivot
                Point2PointConstraint p2p = (Point2PointConstraint) pickConstraint;
                p2p.setPivotB(newPos);
                return true;
            }

        } else {

            if (mouseDragPrevX >= 0) {


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
                            case 1:
                                ClosestRay m = mousePick(x, y);
                                if (!m.hasHit()) {
                                    //drag on background space
                                    float px = mouseDragDX * 0.1f;
                                    float py = mouseDragDY * 0.1f;

                                    //TODO finish:

                                    //Vector3f vx = v().cross(camDir, camUp);
                                    //vx.normalize();
                                    //System.out.println(px + " " + py + " " + camDir + " x " + camUp + " " + vx);

                                    //camPosTarget.scaleAdd(px, vx);
                                    //camPosTarget.scaleAdd(py, camUp);
                                }
                                return true;
                            case 3:
                                //right mouse drag = rotate
                                //                        nextAzi += dx * btScalar(0.2);
                                //                        nextAzi = fmodf(nextAzi, btScalar(360.f));
                                //                        nextEle += dy * btScalar(0.2);
                                //                        nextEle = fmodf(nextEle, btScalar(180.f));
                                azi.setValue(azi.floatValue() + mouseDragDX * 0.2f );
                                //nextAzi = fmodf(nextAzi, btScalar(360.f));
                                ele.setValue(ele.floatValue() + mouseDragDY * (0.2f));
                                //nextEle = fmodf(nextEle, btScalar(180.f));
                                return true;
                            case 2:
                                //middle mouse drag = zoom

                                setCameraDistance( cameraDistance.floatValue() - mouseDragDY * 0.5f );
                                return true;
                        }
                    }
                }
            }

        }

        return false;

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

    public Dynamic newBody(float mass, Transform startTransform, CollisionShape shape) {

        boolean isDynamic = (mass != 0f);
        int collisionFilterGroup = isDynamic ? 1 : 2;
        int collisionFilterMask = isDynamic ? -1 : -3;

        return newBody(mass, shape, new Motion(startTransform), collisionFilterGroup, collisionFilterMask);
    }


    public Dynamic newBody(float mass, CollisionShape shape, MotionState motion, int group, int mask) {
        // rigidbody is dynamic if and only if mass is non zero, otherwise static
        boolean isDynamic = (mass != 0f);
        v3 localInertia = v(0, 0, 0);
        if (isDynamic) {
            shape.calculateLocalInertia(mass, localInertia);
        }

        RigidBodyBuilder c = new RigidBodyBuilder(mass, motion, shape, localInertia);

        Dynamic body = new Dynamic(c);

        return body;
    }


        // See http://www.lighthouse3d.com/opengl/glut/index.php?bmpfontortho
    public void ortho() {
        gl.glViewport(0, 0, screenWidth, screenHeight);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        //gl.glOrtho(-2.0, 2.0, -2.0, 2.0, -1.5, 1.5);
        gl.glOrtho(0, screenWidth, 0, screenHeight, -1.5, 1.5);

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
//        //gl.glScalef(1f, -1f, 1f);
//        // mover the origin from the bottom left corner
//        // to the upper left corner
//        //gl.glTranslatef(0f, -screenHeight, 0f);
        gl.glMatrixMode(gl.GL_MODELVIEW);
        //gl.glLoadIdentity();

        gl.glDisable(GL2.GL_DEPTH_TEST);
    }


    public static final BiConsumer<GL2,Dynamic> defaultRenderer = (gl, body) -> {

        gl.glPushMatrix();
        Draw.transform(gl, body.transform());
        Draw.draw(gl, body.shape());
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

    public final void render(Collidable<X> c) {
        if (c instanceof Dynamic) {
            ((Dynamic)c).renderer(gl);
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

    public Dynamics<X> getDyn() {
        return dyn;
    }

    public void setCamUp(v3 camUp) {
        this.camUp.set(camUp);
    }

    public void setCameraForwardAxis(int axis) {
        forwardAxis = axis;
    }

    public v3 getCamPos() {
        return camPos;
    }

    public v3 getCamPosTarget() {
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
