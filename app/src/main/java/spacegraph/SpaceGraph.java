package spacegraph;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import nars.util.data.map.nbhm.NonBlockingHashMap;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.impl.map.mutable.primitive.IntBooleanHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import com.jogamp.newt.event.*;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import nars.$;
import nars.gui.ConceptWidget;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.Dynamic;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.phys.collision.broad.Broadphase;
import spacegraph.phys.constraint.Point2PointConstraint;
import spacegraph.phys.constraint.TypedConstraint;
import spacegraph.phys.math.MotionState;
import spacegraph.phys.math.Transform;
import spacegraph.phys.util.Motion;
import spacegraph.phys.util.OArrayList;
import spacegraph.render.JoglPhysics;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static com.jogamp.opengl.math.FloatUtil.sin;
import static java.lang.Math.cos;
import static spacegraph.math.v3.v;

/**
 * Created by me on 6/20/16.
 */
public class SpaceGraph<X> extends JoglPhysics<X> {


    final List<Facial> facials = new FasterList<>(1);

    final List<AbstractSpace<X,?>> inputs = new FasterList<>(1);

    final NonBlockingHashMap<X,Spatial> atoms;

    public SpaceGraph() {
        this(16 * 1024);
    }

    /**
     * number of items that will remain cached, some (ideally most)
     * will not be visible but once were and may become visible again
     */
    public SpaceGraph(int cacheCapacity) {
        super();


        this.atoms =
                new NonBlockingHashMap(cacheCapacity);
                //new ConcurrentHashMap(cacheCapacity);
                //Caffeine.newBuilder()
                //.softValues().build();
                //.removalListener(this::onEvicted)
                //.maximumSize(cacheCapacity)
                //.weakValues()
                //.build();


    }


    public SpaceGraph(AbstractSpace<X, ?>... cc) {
        this();

        for (AbstractSpace c : cc)
            add(c);
    }

    public SpaceGraph(Spatial<X>... cc) {
        this();

        add(cc);
    }


//    private void onEvicted(O k1, Spatial<O> v1, RemovalCause removalCause) {
//        //..
//    }

    final List<Facial> preAdd = $.newArrayList();



    public SpaceGraph add(Facial c) {
        if (window == null) {
            preAdd.add(c);
        } else {
            _add(c);
        }
        return this;
    }

    void _add(Facial c) {
        if (this.facials.add(c))
            c.start(this);
    }

    public void add(AbstractSpace<X,?> c) {
        if (inputs.add(c))
            c.start(this);
    }

    public void remove(AbstractSpace<X,?> c) {
        if (inputs.remove(c)) {
            c.stop();
        }
    }

    public <Y extends Spatial<?>> @NotNull Y update(X instance, Function<X, Y> materializer) {
        return getOrAdd(instance, materializer);
    }
    public @NotNull <Y extends Spatial> Y update(Y t) {
        t.preactivate(true);
        return t;
    }

    public @NotNull <Y extends Spatial> Y getOrAdd(X t, Function<X, Y> materializer) {
        return (Y) update(atoms.computeIfAbsent(t, materializer));
    }

    public @Nullable Spatial getIfActive(X t) {
        Spatial v = atoms.get(t);
        return v != null && v.active() ? v : null;
    }



    public void setGravity(v3 v) {
        dyn.setGravity(v);
        dyn.forEachCollidable((i,c)->{
            c.setGravity(v);
            c.setActivationState(1);
        });
    }


    public static float r(float range) {
        return (-0.5f + (float)Math.random())*2f*range;
    }


    public void init(GL2 gl) {
        super.init(gl);

        addMouseListener(new FPSLook(this));
        addMouseListener(new OrbMouse(this));
        addKeyListener(new KeyXYZ(this));


        for (Facial f : preAdd) {
            _add(f);
        }
        preAdd.clear();

        //gl.glEnable(GL2.GL_TEXTURE_2D); // Enable Texture Mapping

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0f); // Black Background
        gl.glClearDepth(1f); // Depth Buffer Setup

        // Quick And Dirty Lighting (Assumes Light0 Is Set Up)
        //gl.glEnable(GL2.GL_LIGHT0);

        //gl.glEnable(GL2.GL_LIGHTING); // Enable Lighting


        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glBlendEquation(GL2.GL_FUNC_ADD);
        gl.glEnable(GL2.GL_BLEND);



        //gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST); // Really Nice Perspective Calculations

        //loadGLTexture(gl);

//        gleem.start(Vec3f.Y_AXIS, window);
//        gleem.attach(new DefaultHandleBoxManip(gleem).translate(0, 0, 0));
    }




    @Override final public void forEachIntSpatial(IntObjectPredicate<Spatial<X>> each) {
        int n = 0;
        for (int i = 0, inputsSize = inputs.size(); i < inputsSize; i++) {
            AbstractSpace s = inputs.get(i);
            n += s.forEachIntSpatial(n, each);
        }
    }



    public synchronized final void display(GLAutoDrawable drawable) {

        this.inputs.forEach( this::update );

        super.display(drawable);

        renderHUD();

    }



    protected void renderHUD() {
        ortho();

        GL2 gl = this.gl;
        for (int i = 0, facialsSize = facials.size(); i < facialsSize; i++) {
            facials.get(i).render(gl);
        }
    }


    public final synchronized void update(AbstractSpace s) {

        //float dt = s.setBusy();

        s.update(this);

    }

    void print(AbstractSpace s) {
        System.out.println();
        //+ active.size() + " active, "
        System.out.println(s + ": "   + this.atoms.size() + " cached; "+ "\t" + dyn.summary());
        /*s.forEach(System.out::println);
        dyn.objects().forEach(x -> {
            System.out.println("\t" + x.getUserPointer());
        });*/
        System.out.println();
    }

    public ListSpace<X,?> add(Spatial<X>... s) {
        ListSpace<X, ?> l = new ListSpace(s);
        add(l);
        return l;
    }


    public static class ForceDirected<X> implements spacegraph.phys.constraint.BroadConstraint<X> {

        public static final int clusters = 1;

        public float repelSpeed = 3f;
        public float attractSpeed = 5f;

        private float minRepelDist = 0f;
        private float maxRepelDist = 350f;
        private float attractDist = 1f;

//        public static class Edge<X> extends MutablePair<X,X> {
//            public final X a, b;
//            public Object aData;
//            public Object bData;
//
//            public Edge(X a, X b) {
//                super(a, b);
//                this.a = a;
//                this.b = b;
//            }
//        }
//
//        final SimpleGraph<X,Edge> graph = new SimpleGraph((a,b)->new Edge(a,b));
//
//        public Edge get(X x, X y) {
//            graph.addVertex(x);
//            graph.addVertex(y);
//            graph.getEdge(x, y);
//        }

        @Override
        public void solve(Broadphase b, OArrayList<Collidable<X>> objects, float timeStep) {

            //System.out.print("Force direct " + objects.size() + ": ");
            //final int[] count = {0};
            b.forEach(objects.size()/ clusters, objects, (l) -> {
                batch(l);
                //count[0] += l.size();
                //System.out.print(l.size() + "  ");
            });
            //System.out.println(" total=" + count[0]);

            for (Collidable c : objects) {

                Spatial A = ((Spatial) c.data());
                if (A instanceof ConceptWidget) {
                    for (EDraw e : ((ConceptWidget) A).edges) {

                        SimpleSpatial B = e.target;

                        if ((B !=null) && (B !=A) && (B.body!=null)) {

                            float ew = e.width;
                            float attractStrength = ew * ew;
                            attract(c, B.body, attractSpeed * attractStrength, attractDist);
                        }
                    }
                }

            }

        }

        protected void batch(List<Collidable<X>> l) {


            for (int i = 0, lSize = l.size(); i < lSize; i++) {
                Collidable x = l.get(i);
                for (int i1 = i+1, lSize1 = l.size(); i1 < lSize1; i1++) {
                    Collidable y = l.get(i1);

                    repel(x, y, repelSpeed, minRepelDist, maxRepelDist);
                }
            }
        }

        private void attract(Collidable x, Collidable y, float speed, float idealDist) {
            SimpleSpatial xp = ((SimpleSpatial) x.data());
            SimpleSpatial yp = ((SimpleSpatial) y.data());

            v3 delta = v();
            delta.sub(xp.transform(), yp.transform());


            float len = delta.normalize();
            if (len <= 0)
                return;

            len -= (xp.radius + yp.radius);

            if (len > idealDist) {
                //float dd = (len - idealDist);
                float dd = 0; //no attenuation over distance

                delta.scale((-(speed*speed) / (1f+dd)) / 2f);

                ((Dynamic) x).impulse(delta);
                delta.negate();
                ((Dynamic) y).impulse(delta);

            }

        }

        private void repel(Collidable x, Collidable y, float speed, float minDist, float maxDist) {
            SimpleSpatial xp = ((SimpleSpatial) x.data());
            SimpleSpatial yp = ((SimpleSpatial) y.data());

            v3 delta = v();
            delta.sub(xp.transform(), yp.transform());

            float len = delta.normalize();
            len -= ( xp.radius + yp.radius );

            if (len <= minDist)
                return;

            delta.scale(((speed*speed)/(1+len*len))/2f);

            //experimental
//            if (len > maxDist) {
//                delta.negate(); //attract
//            }

            ((Dynamic)x).impulse(delta);
            //xp.moveDelta(delta, 0.5f);
            delta.negate();
            ((Dynamic)y).impulse(delta);
            //yp.moveDelta(delta, 0.5f);

        }


    }

    public abstract static class SpaceMouse extends MouseAdapter {

        public final JoglPhysics space;
        public SpaceMouse(JoglPhysics g) {
            this.space = g;
        }
    }
    public abstract static class SpaceKeys extends KeyAdapter implements FrameListener {

        public final JoglPhysics space;

        //TODO merge these into one Map
        final IntBooleanHashMap keyState = new IntBooleanHashMap();
        final IntObjectHashMap<FloatProcedure> keyPressed = new IntObjectHashMap();
        final IntObjectHashMap<FloatProcedure> keyReleased = new IntObjectHashMap();

        public SpaceKeys(JoglPhysics g) {
            this.space = g;


            g.addFrameListener(this);
        }

        @Override
        public void onFrame(JoglPhysics j) {
            float dt = j.getLastFrameTime();
            keyState.forEachKeyValue((k,s)->{
                FloatProcedure f = (s) ? keyPressed.get(k) : keyReleased.get(k);
                if (f!=null) {
                    f.value(dt);
                }
            });
        }

        protected void watch(int keyCode, FloatProcedure ifPressed, FloatProcedure ifReleased) {
            keyState.put(keyCode, false); //initialized
            if (ifPressed!=null)
                keyPressed.put(keyCode, ifPressed);
            if (ifReleased!=null)
                keyReleased.put(keyCode, ifReleased);
        }

        //TODO unwatch

        @Override
        public void keyReleased(KeyEvent e) {
            setKey((int) e.getKeyCode(), false);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            setKey((int) e.getKeyCode(), true);
        }

        protected void setKey(int c, boolean state) {
            if (keyState.containsKey(c)) {
                keyState.put(c, state);
            }
        }
    }

    /** simple XYZ control using keys (ex: numeric keypad) */
    public static class KeyXYZ extends SpaceKeys {

        public KeyXYZ(JoglPhysics g) {
            super(g);


            float speed = 2f;
            watch(KeyEvent.VK_NUMPAD4, (dt)-> {
                moveX(speed);
            }, null);
            watch(KeyEvent.VK_NUMPAD6, (dt)-> {
                moveX(-speed);
            }, null);
            watch(KeyEvent.VK_NUMPAD8, (dt)-> {
                moveY(speed);
            }, null);
            watch(KeyEvent.VK_NUMPAD2, (dt)-> {
                moveY(-speed);
            }, null);
            watch(KeyEvent.VK_NUMPAD5, (dt)-> {
                moveZ(speed);
            }, null);
            watch(KeyEvent.VK_NUMPAD0, (dt)-> {
                moveZ(-speed);
            }, null);

        }

        void moveX(float speed) {
            v3 x = v(space.camFwd);
            //System.out.println("x " + x);
            x.cross(x, space.camUp);
            x.normalize();
            x.scale(-speed);
            space.camPos.add(x);
        }
        void moveY(float speed) {
            v3 y = v(space.camUp);
            y.normalize();
            y.scale(speed);
            //System.out.println("y " + y);
            space.camPos.add(y);
        }
        void moveZ(float speed) {
            v3 z = v(space.camFwd);
            //System.out.println("z " + z);
            z.scale(speed);
            space.camPos.add(z);
        }

        @Override
        public void keyPressed(KeyEvent e) {
            super.keyPressed(e);
        }

    }

//    public static class PickDragMouse extends SpaceMouse {
//
//        public PickDragMouse(JoglPhysics g) {
//            super(g);
//        }
//    }
//    public static class PickZoom extends SpaceMouse {
//
//        public PickZoom(JoglPhysics g) {
//            super(g);
//        }
//    }

    public static class FPSLook extends SpaceMouse {

        boolean dragging = false;
        private int prevX, prevY;
        float h = (float)Math.PI; //angle
        float v = 0; //angle

        public FPSLook(JoglPhysics g) {
            super(g);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            dragging = false;
        }


        @Override
        public void mouseDragged(MouseEvent e) {
            short[] bd = e.getButtonsDown();
            if (bd.length > 0 && bd[0] == 3 /* RIGHT */) {
                if (!dragging) {
                    prevX = e.getX();
                    prevY = e.getY();
                    dragging = true;
                }

                int x = e.getX();
                int y = e.getY();

                int dx = x - prevX;
                int dy = y - prevY;

                float angleSpeed = 0.001f;
                h += -dx * angleSpeed;
                v += -dy * angleSpeed;

                v3 direction = v(
                        (float)(cos(this.v) * sin(h)),
                        (float)sin(this.v),
                        (float)(cos(this.v) * cos(h))
                );

                //System.out.println("set direction: " + direction);

                space.camFwd.set(direction);

                prevX = x;
                prevY = y;
            }
        }

    }

    public static class OrbMouse extends SpaceMouse implements KeyListener {

        final ClosestRay rayCallback = new ClosestRay(((short)(1 << 7)));
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


        public OrbMouse(JoglPhysics g) {

            super(g);
            g.addKeyListener(this);

        }

        @Override
        public void mouseWheelMoved(MouseEvent e) {
            //System.out.println("wheel=" + Arrays.toString(e.getRotati on()));
            float y = e.getRotation()[1];
            if (y!=0) {
                //space.setCameraDistance( space.cameraDistance.floatValue() + 0.1f * y );
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

                        space.camera(objTarget, co.shape().getBoundingRadius() * 1.25f + space.nearPlane * 1.25f);

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
            return mousePick(v(space.rayTo(sx, sy)));
        }

        public ClosestRay mousePick(v3 rayTo) {
            ClosestRay r = this.rayCallback;
            v3 camPos = space.camPos;
            space.dyn.rayTest(camPos, rayTo, r.set(camPos, rayTo));
            return r;
        }

        private boolean mouseMotionFunc(int x, int y, short[] buttons) {

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
                    if (pickedSpatial.onTouch(picked, cray, buttons)) {
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
            if (pickedSpatial !=null) {
                pickedSpatial.onKey(picked, hitPoint, e.getKeyChar(), true);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (pickedSpatial !=null) {
                pickedSpatial.onKey(picked, hitPoint, e.getKeyChar(), false);
            }
        }
    }
}
