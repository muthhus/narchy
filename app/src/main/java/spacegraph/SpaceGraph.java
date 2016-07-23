package spacegraph;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import nars.$;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.phys.collision.dispatch.Collidable;
import spacegraph.phys.dynamics.RigidBody;
import spacegraph.render.JoglPhysics;

import javax.vecmath.Vector3f;
import java.util.List;
import java.util.function.Function;

/**
 * Created by me on 6/20/16.
 */
public class SpaceGraph<O> extends JoglPhysics<Spatial<O>> {

    public SpaceGraph with(SpaceTransform<O>... t) {
        for (SpaceTransform g : t)
            this.transforms.add(g);
        return this;
    }


    final List<Facial> facials = new FasterList<>(1);

    final List<SpaceInput<O,?>> inputs = new FasterList<>(1);

    private Function<O, Spatial<O>> materialize = x -> (Spatial<O>)x;

    //final WeakValueHashMap<O, Spatial<O>> atoms = new WeakValueHashMap<>(1024);
    final Cache<O, Spatial<O>> atoms = Caffeine.newBuilder()
            .softValues().build();
            //.weakValues().build();


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

    final List<Facial> preAdd = $.newArrayList();



    public void add(Facial c) {
        if (window == null) {
            preAdd.add(c);
        } else {
            _add(c);
        }
    }

    void _add(Facial c) {
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

    public @NotNull Spatial update(O instance) {
        return getOrAdd(instance);
    }
    public @NotNull Spatial<O> update(Function<? super O, Spatial<O>> materializer, O instance) {
        return getOrAdd(instance, materializer);
    }
    public @NotNull Spatial<O> update(Spatial<O> t) {
        t.preactivate(true);
        return t;
    }


    public @NotNull Spatial getOrAdd(O t) {
        return getOrAdd(t, materialize);
    }

    public @NotNull Spatial<O> getOrAdd(O t, Function<? super O, ? extends Spatial<O>> materializer) {
        return update(atoms.get(t, materializer));
    }

    public @Nullable Spatial getIfActive(O t) {
        Spatial v = atoms.getIfPresent(t);
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

    @Override protected final boolean valid(int nextID, Collidable<Spatial<O>> c) {

        Spatial vd = c.getUserPointer();
        if (vd!=null) {
            if (vd.active()) {
                vd.activate((short)nextID);
            } else {
                vd.stop(this);
                return false; //remove
            }
        }
        return true;
    }

    public void display(GLAutoDrawable drawable) {

        List<SpaceInput<O,?>> ss = this.inputs;

        ss.forEach( this::update );

        super.display(drawable);

        //ss.forEach(this::print);

        ss.forEach( SpaceInput::ready );

        renderHUD();
    }



    protected void renderHUD() {
        ortho();

        GL2 gl = this.gl;
        for (int i = 0, facialsSize = facials.size(); i < facialsSize; i++) {
            facials.get(i).render(gl);
        }
    }


    public void add(Spatial s) {

        if (s.body == null) {
            s.start(this);
        }

    }

    public final synchronized void update(SpaceInput s) {

        float dt = s.setBusy();

        s.update(this);

        List<SpaceTransform<O>> ll = this.transforms;
        for (int i1 = 0, layoutSize = ll.size(); i1 < layoutSize; i1++) {
            ll.get(i1).update(this, ((ListInput)s).active, dt);
        }


    }

    void print(SpaceInput s) {
        System.out.println();
        //+ active.size() + " active, "
        System.out.println(s + ": "   + this.atoms.estimatedSize() + " cached; "+ "\t" + dyn.summary());
        /*s.forEach(System.out::println);
        dyn.objects().forEach(x -> {
            System.out.println("\t" + x.getUserPointer());
        });*/
        System.out.println();
    }

}
