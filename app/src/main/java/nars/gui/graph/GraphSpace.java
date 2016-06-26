package nars.gui.graph;

import bulletphys.collision.dispatch.CollisionObject;
import bulletphys.ui.GLConsole;
import bulletphys.ui.JoglPhysics;
import com.google.common.collect.Lists;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import nars.gui.graph.matter.concept.ConceptBagInput;
import nars.gui.graph.layout.FastOrganicLayout;
import nars.gui.graph.matter.concept.ConceptMaterializer;
import nars.nar.Default;
import nars.term.Termed;
import nars.util.data.list.FasterList;
import nars.util.experiment.DeductiveMeshTest;
import org.infinispan.commons.util.WeakValueHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static nars.gui.test.Lesson14.renderString;

/**
 * Created by me on 6/20/16.
 */
public class GraphSpace<O, X extends Atomatter<O>> extends JoglPhysics<X> {

    public static void main(String[] args) {

        Default n = new Default(1024, 8, 6, 8);
        //n.nal(4);


        new DeductiveMeshTest(n, new int[]{5,5}, 16384);

        final int maxNodes = 64;
        final int maxEdges = 8;

        new GraphSpace<Termed,Atomatter<Termed>>(
            new ConceptBagInput(n, maxNodes, maxEdges)
        ).show(900, 900);

        n.loop(35f);

    }


    final FasterList<ConceptBagInput> inputs = new FasterList<>(1);
    private Function<O, ? extends X> materialize;

    final WeakValueHashMap<O, X> atoms;


    List<GraphTransform<O>> transforms = Lists.newArrayList(
        //new Spiral()
        new FastOrganicLayout()
    );

    public GraphSpace(ConceptBagInput c) {
        this(c, null);
    }

    public GraphSpace(ConceptBagInput c, Function<O, ? extends X> defaultMaterializer) {
        super();

        atoms = new WeakValueHashMap<>(1024);

        this.materialize = defaultMaterializer;

        enable(c);
    }

    public void enable(ConceptBagInput c) {
        inputs.add(c);
        c.start(this);
    }

    public @NotNull X update(int order, O instance) {
        return update(order, getOrAdd(instance), instance);
    }
    public @NotNull X update(int order, Function<? super O, X> materializer, O instance) {
        return update(order, getOrAdd(instance, materializer), instance);
    }
    public @NotNull X update(int order, X t, O instance) {
        t.activate(this, (short) order, instance);
        return t;
    }


    public @NotNull X getOrAdd(O t) {
        return atoms.computeIfAbsent(t, materialize);
    }
    public @NotNull X getOrAdd(O t, Function<? super O, ? extends X> materializer) {
        return atoms.computeIfAbsent(t, materializer);
    }

    public @Nullable X getIfActive(O t) {
        X v = atoms.get(t);
        return v != null && v.active() ? v : null;
    }

    /**
     * get the latest info into the draw object
     */
    protected @NotNull X pre(int i, X v, O b) {
        v.activate(this, (short)i, b);
        return v;
    }




    public static final class EDraw {
        public Atomatter key;
        public float width, r, g, b, a;

        public void set(Atomatter x, float width, float r, float g, float b, float a) {
            this.key = x;
            this.width = width;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        public void clear() {
            key = null;
        }
    }


    static float r(float range) {
        return (-0.5f + (float)Math.random()*range)*2f;
    }


    public void init(GL2 gl) {
        super.init(gl);

        //gl.glEnable(GL2.GL_TEXTURE_2D); // Enable Texture Mapping

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.01f); // Black Background
        //gl.glClearDepth(1f); // Depth Buffer Setup

        // Quick And Dirty Lighting (Assumes Light0 Is Set Up)
        //gl.glEnable(GL2.GL_LIGHT0);

        //gl.glEnable(GL2.GL_LIGHTING); // Enable Lighting

        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);


        //gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST); // Really Nice Perspective Calculations

        //loadGLTexture(gl);

//        gleem.start(Vec3f.Y_AXIS, window);
//        gleem.attach(new DefaultHandleBoxManip(gleem).translate(0, 0, 0));
    }



    @Override protected final boolean valid(CollisionObject<X> c) {
        X vd = c.getUserPointer();
        if (vd!=null && !vd.active()) {
            vd.body.setUserPointer(null); //remove reference so vd can be GC
            vd.body = null;
            return false;
        }
        return true;
    }

    public synchronized void display(GLAutoDrawable drawable) {

        List<ConceptBagInput> ss = this.inputs;

        ss.forEach( this::update );

        super.display(drawable);

        ss.forEach( ConceptBagInput::ready );

        renderHUD();
    }

    final GLConsole terminal = new GLConsole(50, 20, 0.05f);

    protected void renderHUD() {
        ortho();
        gl.glColor4f(1f,1f,1f, 1f);
        //gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        terminal.render(gl);
    }

    /*public void clear(GL2 gl) {
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
    }*/

//    public void render(GL2 gl, ConceptsSource s) {
//
//        @Deprecated float dt = Math.max(0.001f /* non-zero */, s.dt());
//
//        List<VDraw> toDraw = s.visible;
//
//        s.busy.set(true);
//
//        update(toDraw, dt);
//
//        for (int i1 = 0, toDrawSize = toDraw.size(); i1 < toDrawSize; i1++) {
//
//            render(gl, toDraw.get(i1));
//
//        }
//
//        s.busy.set(false);
//    }

//    public void render(GL2 gl, VDraw v) {
//
//        gl.glPushMatrix();
//
//        gl.glTranslatef(v.x(), v.y(), v.z());
//
//        v.render(gl);
//
//        gl.glPopMatrix();
//
//
//    }



//    public void renderVertexBase(GL2 gl, float dt, VDraw v) {
//
//        gl.glPushMatrix();
//
//
//        //gl.glRotatef(45.0f - (2.0f * yloop) + xrot, 1.0f, 0.0f, 0.0f);
//        //gl.glRotatef(45.0f + yrot, 0.0f, 1.0f, 0.0f);
//
//
//        float pri = v.pri;
//
//
//        float r = v.radius;
//        gl.glScalef(r, r, r);
//
//        final float activationPeriods = 4f;
//        gl.glColor4f(h(pri),
//                pri * Math.min(1f, 1f / (1f + (v.lag / (activationPeriods * dt)))),
//                h(v.budget.dur()),
//                v.budget.qua() * 0.25f + 0.25f);
//        gl.glCallList(box);
//        //glut.glutSolidTetrahedron();
//
//        gl.glPopMatrix();
//    }



    public final void update(GraphInput s) {

        float dt = s.setBusy();

        List<Atomatter<O>> toDraw = s.visible();
        for (int i = 0, toDrawSize = toDraw.size(); i < toDrawSize; i++) {
            toDraw.get(i).update(this);
        }

        List<GraphTransform<O>> ll = this.transforms;
        for (int i1 = 0, layoutSize = ll.size(); i1 < layoutSize; i1++) {
            ll.get(i1).update(this, toDraw, dt);
        }

    }


    public static float h(float p) {
        return p * 0.9f + 0.1f;
    }



    abstract public static class GraphInput<O, M extends Atomatter<O>> {

        protected List<M> visible = new FasterList(0);
        final AtomicBoolean busy = new AtomicBoolean(true);
        protected GraphSpace grapher;
        private float now;
        private float dt;



        public void start(GraphSpace grapher) {
            this.grapher = grapher;
        }

        public void stop() {
            this.grapher = null;
        }

        public final List<M> visible() {
            return visible;
        }

        public void ready() {
            busy.set(false);
        }

        public final float setBusy() {
            busy.set(true);
            return dt;
        }

        public boolean isBusy() {
            return busy.get();
        }

        public float dt() {
            return dt;
        }

        /** rewinds the buffer of visible items, when collecting a new batch */
        public List<M> rewind(int capacity) {
            visible.forEach(Atomatter::inactivate);
            return visible = new FasterList<>(capacity);
        }


        public void update() {
            float last = this.now;
            this.dt = (this.now = now()) - last;
            updateImpl();
        }

        /** for thread-safe usage */
        public void updateIfNotBusy() {
            if (!isBusy()) {
                update();
            }
        }

        abstract protected void updateImpl();

        abstract public float now();

    }

}
//    private void buildLists(GL2 gl) {
//        box = gl.glGenLists(2); // Generate 2 Different Lists
//        gl.glNewList(box, GL2.GL_COMPILE); // Start With The Box List
//
//        gl.glBegin(GL2.GL_QUADS);
//        gl.glNormal3f(0.0f, -1.0f, 0.0f);
//        //gl.glTexCoord2f(1.0f, 1.0f);
//        gl.glVertex3f(-1.0f, -1.0f, -1.0f); // Bottom Face
//        //gl.glTexCoord2f(0.0f, 1.0f);
//        gl.glVertex3f(1.0f, -1.0f, -1.0f);
//        //gl.glTexCoord2f(0.0f, 0.0f);
//        gl.glVertex3f(1.0f, -1.0f, 1.0f);
//        //gl.glTexCoord2f(1.0f, 0.0f);
//        gl.glVertex3f(-1.0f, -1.0f, 1.0f);
//
//        gl.glNormal3f(0.0f, 0.0f, 1.0f);
//        //gl.glTexCoord2f(0.0f, 0.0f);
//        gl.glVertex3f(-1.0f, -1.0f, 1.0f); // Front Face
//        //gl.glTexCoord2f(1.0f, 0.0f);
//        gl.glVertex3f(1.0f, -1.0f, 1.0f);
//        //gl.glTexCoord2f(1.0f, 1.0f);
//        gl.glVertex3f(1.0f, 1.0f, 1.0f);
//        //gl.glTexCoord2f(0.0f, 1.0f);
//        gl.glVertex3f(-1.0f, 1.0f, 1.0f);
//
//        gl.glNormal3f(0.0f, 0.0f, -1.0f);
//        //gl.glTexCoord2f(1.0f, 0.0f);
//        gl.glVertex3f(-1.0f, -1.0f, -1.0f); // Back Face
//        //gl.glTexCoord2f(1.0f, 1.0f);
//        gl.glVertex3f(-1.0f, 1.0f, -1.0f);
//        //gl.glTexCoord2f(0.0f, 1.0f);
//        gl.glVertex3f(1.0f, 1.0f, -1.0f);
//        //gl.glTexCoord2f(0.0f, 0.0f);
//        gl.glVertex3f(1.0f, -1.0f, -1.0f);
//
//        gl.glNormal3f(1.0f, 0.0f, 0.0f);
//        //gl.glTexCoord2f(1.0f, 0.0f);
//        gl.glVertex3f(1.0f, -1.0f, -1.0f); // Right face
//        //gl.glTexCoord2f(1.0f, 1.0f);
//        gl.glVertex3f(1.0f, 1.0f, -1.0f);
//        //gl.glTexCoord2f(0.0f, 1.0f);
//        gl.glVertex3f(1.0f, 1.0f, 1.0f);
//        //gl.glTexCoord2f(0.0f, 0.0f);
//        gl.glVertex3f(1.0f, -1.0f, 1.0f);
//
//        gl.glNormal3f(-1.0f, 0.0f, 0.0f);
//        //gl.glTexCoord2f(0.0f, 0.0f);
//        gl.glVertex3f(-1.0f, -1.0f, -1.0f); // Left Face
//        //gl.glTexCoord2f(1.0f, 0.0f);
//        gl.glVertex3f(-1.0f, -1.0f, 1.0f);
//        //gl.glTexCoord2f(1.0f, 1.0f);
//        gl.glVertex3f(-1.0f, 1.0f, 1.0f);
//        //gl.glTexCoord2f(0.0f, 1.0f);
//        gl.glVertex3f(-1.0f, 1.0f, -1.0f);
//        gl.glEnd();
//
//        gl.glEndList();
//
//        {
//            isoTri = box + 1; // Storage For "Top" Is "Box" Plus One
//            gl.glNewList(isoTri, GL2.GL_COMPILE); // Now The "Top" Display List
//
//            gl.glBegin(GL2.GL_TRIANGLES);
//            gl.glNormal3f(0.0f, 0f, 1.0f);
//
//            final float h = 0.5f;
//            gl.glVertex3f(0, h,  0f); //right base
//            gl.glVertex3f(0, -h, 0f); //left base
//            gl.glVertex3f(1,  0, 0f);  //midpoint on opposite end
//
//            gl.glEnd();
//            gl.glEndList();
//        }
//    }
