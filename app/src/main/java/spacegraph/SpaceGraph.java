package spacegraph;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import nars.$;
import nars.gui.ConceptBagInput;
import nars.nar.Default;
import nars.term.Termed;
import nars.util.data.list.FasterList;
import nars.util.experiment.DeductiveMeshTest;
import org.infinispan.commons.util.WeakValueHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.layout.FastOrganicLayout;
import spacegraph.phys.collision.dispatch.CollisionObject;
import spacegraph.render.JoglPhysics;

import javax.vecmath.Vector3f;
import java.util.List;
import java.util.function.Function;

/**
 * Created by me on 6/20/16.
 */
public class SpaceGraph<O> extends JoglPhysics<Spatial<O>> {

    public static void main(String[] args) {

        Default n = new Default(1024, 8, 6, 8);
        n.conceptActivation.setValue(0.25f);
        //n.nal(4);


        new DeductiveMeshTest(n, new int[]{5,5}, 16384);

        final int maxNodes = 128;
        final int maxEdges = 10;

        new SpaceGraph<Termed>(
            new ConceptBagInput(n, maxNodes, maxEdges)
        ).with(
            //new Spiral()
            new FastOrganicLayout()
        ).show(900, 900);

        n.loop(35f);

    }

    public SpaceGraph with(SpaceTransform<O>... t) {
        for (SpaceTransform g : t)
            this.transforms.add(g);
        return this;
    }


    final List<Facial> facials = new FasterList<>(1);

    final List<SpaceInput<O,?>> inputs = new FasterList<>(1);

    private Function<O, Spatial<O>> materialize = x -> (Spatial<O>)x;

    final WeakValueHashMap<O, Spatial<O>> atoms = new WeakValueHashMap<>(1024);


    final List<SpaceTransform<O>> transforms = $.newArrayList();

    public SpaceGraph() {
        super();
    }

    public SpaceGraph(Function<O, Spatial<O>> materializer, O... c) {
        this(materializer, new ListInput<>(c));
    }

    public SpaceGraph(SpaceInput<O,?> c) {
        this(null, c);
    }

    public SpaceGraph(Function<O, Spatial<O>> defaultMaterializer, SpaceInput<O, ?>... cc) {
        super();

        this.materialize = defaultMaterializer;

        for (SpaceInput c : cc)
            add(c);
    }

    public void add(Facial c) {
        if (this.facials.add(c))
            c.start(this);
    }

    public void add(SpaceInput<O,?> c) {
        if (inputs.add(c))
            c.start(this);
    }

    public void remove(SpaceInput<O,?> c) {
        if (inputs.remove(c)) {
            c.stop();
        }
    }

    public @NotNull Spatial update(int order, O instance) {
        return update(order, getOrAdd(instance));
    }
    public @NotNull Spatial<O> update(int order, Function<? super O, Spatial<O>> materializer, O instance) {
        return update(order, getOrAdd(instance, materializer));
    }
    public @NotNull Spatial<O> update(int order, Spatial<O> t) {
        t.activate((short) order);
        return t;
    }


    public @NotNull Spatial getOrAdd(O t) {
        return atoms.computeIfAbsent(t, materialize);
    }
    public @NotNull Spatial<O> getOrAdd(O t, Function<? super O, ? extends Spatial<O>> materializer) {
        return atoms.computeIfAbsent(t, materializer);
    }

    public @Nullable Spatial getIfActive(O t) {
        Spatial v = atoms.get(t);
        return v != null && v.active() ? v : null;
    }



    public void setGravity(Vector3f v) {
        dyn.setGravity(v);
    }


    static float r(float range) {
        return (-0.5f + (float)Math.random()*range)*2f;
    }


    public void init(GL2 gl) {
        super.init(gl);

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



    @Override protected final boolean valid(CollisionObject<Spatial<O>> c) {
        Spatial vd = c.getUserPointer();
        if (vd!=null && !vd.active()) {
            vd.body.setUserPointer(null); //remove reference so vd can be GC
            vd.body = null;
            return false;
        }
        return true;
    }

    public synchronized void display(GLAutoDrawable drawable) {

        List<SpaceInput<O,?>> ss = this.inputs;

        ss.forEach( this::update );

        super.display(drawable);

        ss.forEach( SpaceInput::ready );

        renderHUD();
    }



    protected void renderHUD() {
        ortho();

        GL2 gl = this.gl;
        for (int i = 0, facialsSize = facials.size(); i < facialsSize; i++) {
            facials.get(i).render(gl);
        }

        //gl.glColor4f(1f,1f,1f, 1f);
        //gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        //terminal.render(gl);
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



    public final void update(SpaceInput s) {

        float dt = s.setBusy();

        List<Spatial<O>> toDraw = s.visible();
        for (int i = 0, toDrawSize = toDraw.size(); i < toDrawSize; i++) {
            toDraw.get(i).update(this);
        }

        List<SpaceTransform<O>> ll = this.transforms;
        for (int i1 = 0, layoutSize = ll.size(); i1 < layoutSize; i1++) {
            ll.get(i1).update(this, toDraw, dt);
        }

    }


    public static float h(float p) {
        return p * 0.9f + 0.1f;
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
