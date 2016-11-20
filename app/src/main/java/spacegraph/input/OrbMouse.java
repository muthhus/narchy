package spacegraph.input;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import spacegraph.SimpleSpatial;
import spacegraph.Spatial;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.phys.collision.narrow.VoronoiSimplexSolver;
import spacegraph.phys.constraint.Point2PointConstraint;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.phys.math.MotionState;
import spacegraph.phys.math.Transform;
import spacegraph.phys.util.Motion;
import spacegraph.render.JoglPhysics;

import static spacegraph.math.v3.v;

/**
 * Created by me on 11/20/16.
 */
public class OrbMouse extends SpaceMouse implements KeyListener {

    protected final ClosestRay rayCallback = new ClosestRay(((short) (1 << 7)));
    // constraint for mouse picking
    private int mouseDragPrevX, mouseDragPrevY;
    private int mouseDragDX, mouseDragDY;
    final v3 gOldPickingPos = v();
    float gOldPickingDist = 0.f;

    protected TypedConstraint pickConstraint = null;
    protected Dynamic directDrag;
    public Dynamic pickedBody = null; // for deactivation state
    private Spatial pickedSpatial;
    private Collidable picked;
    private v3 hitPoint;
    protected final VoronoiSimplexSolver simplexSolver = new VoronoiSimplexSolver();


    public OrbMouse(JoglPhysics g) {

        super(g);
        g.addKeyListener(this);

    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        //System.out.println("wheel=" + Arrays.toString(e.getRotati on()));
        float y = e.getRotation()[1];
        if (y != 0) {
            //space.setCameraDistance( space.cameraDistance.floatValue() + 0.1f * y );
        }
    }

    private boolean mouseClick(int button, int x, int y) {

        switch (button) {
            case MouseEvent.BUTTON3: {
                ClosestRay c = mousePick(x, y);
                if (c.hasHit()) {
                    Collidable co = c.collidable;
                    //System.out.println("zooming to " + co);

                    //TODO compute new azi and ele that match the current viewing angle values by backcomputing the vector delta


                    v3 objTarget = co.getWorldOrigin();

                    space.camera(objTarget, co.shape().getBoundingRadius() * 1.25f + space.zNear * 1.25f);

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
                    mouseGrabOn(x, y);
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
            space.dyn.removeConstraint(pickConstraint);
            pickConstraint = null;

            pickedBody.forceActivationState(Collidable.ACTIVE_TAG);
            pickedBody.setDeactivationTime(0f);
            pickedBody = null;
        }

        if (directDrag != null) {
            Object u = directDrag.data();

            if (u instanceof SimpleSpatial) {
                ((SimpleSpatial) u).motionLock(false);
            }

            directDrag = null;
        }
    }


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
                v3 eyePos = v(space.camPos);
                v3 tmp = v();
                tmp.sub(pickPos, eyePos);
                gOldPickingDist = tmp.length();


                // other exclusions?
                if (!(body.isStaticObject() || body.isKinematicObject())) {
                    pickedBody = body;


                    Point2PointConstraint p2p = new Point2PointConstraint(body, localPivot);
                    space.dyn.addConstraint(p2p);
                    pickConstraint = p2p;

                    // very weak constraint for picking
                    p2p.tau = 0.02f;
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
        return mousePick(space.rayTo(sx, sy));
    }

    public ClosestRay mousePick(v3 rayTo) {
        ClosestRay r = this.rayCallback;
        v3 camPos = space.camPos;

        space.dyn.rayTest(camPos, rayTo, r.set(camPos, rayTo), simplexSolver);
        return r;
    }

    @Deprecated /* TODO probably rewrite */ private boolean mouseMotionFunc(int x, int y, short[] buttons) {

        v3 ray = space.rayTo(x, y);

        //if (mouseDragDX == 0) { //if not already dragging somewhere "outside"

        ClosestRay cray = mousePick(ray);

        /*System.out.println(mouseTouch.collisionObject + " touched with " +
            Arrays.toString(buttons) + " at " + mouseTouch.hitPointWorld
        );*/

        picked = cray.collidable;
        if (picked != null) {
            Object t = picked.data();
            if (t instanceof Spatial) {
                pickedSpatial = ((Spatial) t);
                hitPoint = cray.hitPointWorld;
                if (pickedSpatial.onTouch(picked, cray, buttons) != null) {
                    //absorbed
                    clearDrag();
                    return true;
                }
            } else {
                pickedSpatial = null;
            }
        } else {
            pickedSpatial = null;
        }

        //}

        if ((pickConstraint != null) || (directDrag != null)) {

            // keep it at the same picking distance
            v3 eyePos = v(space.camPos);
            v3 dir = v();
            dir.sub(ray, eyePos);
            dir.normalize();
            dir.scale(gOldPickingDist);

            v3 newPos = v();
            newPos.add(eyePos, dir);

            if (directDrag != null) {
                //directly move the 'static' object

                Object u = directDrag.data();

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
                                //space.azi.setValue(space.azi.floatValue() + mouseDragDX * 0.2f );
                                //nextAzi = fmodf(nextAzi, btScalar(360.f));
                                //space.ele.setValue(space.ele.floatValue() + mouseDragDY * (0.2f));
                                //nextEle = fmodf(nextEle, btScalar(180.f));
                                return true;
                            case 2:
                                //middle mouse drag = zoom

                                //space.setCameraDistance( space.cameraDistance.floatValue() - mouseDragDY * 0.5f );
                                return true;
                        }
                    }
                }
            }

        }

        return false;

    }

    public void mouseClicked(MouseEvent e) {
        //overriden: called by mouseRelease
    }

    public void mouseEntered(MouseEvent e) {
    }

//        public void mouseExited(MouseEvent e) {
//            clearDrag();
//        }

    @Deprecated
    public void clearDrag() {
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

        clearDrag();
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

    @Override
    public void keyPressed(KeyEvent e) {
        if (pickedSpatial != null) {
            pickedSpatial.onKey(picked, hitPoint, e.getKeyChar(), true);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (pickedSpatial != null) {
            pickedSpatial.onKey(picked, hitPoint, e.getKeyChar(), false);
        }
    }
}
