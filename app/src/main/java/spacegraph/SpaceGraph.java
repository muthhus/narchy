package spacegraph;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.gs.collections.api.block.predicate.primitive.IntObjectPredicate;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import nars.$;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.math.v3;
import spacegraph.render.JoglPhysics;

import java.util.List;
import java.util.function.Function;

/**
 * Created by me on 6/20/16.
 */
public class SpaceGraph<X> extends JoglPhysics<X> {


    final List<Facial> facials = new FasterList<>(1);

    final List<AbstractSpace<X,?>> inputs = new FasterList<>(1);

    final Cache<X, Spatial> atoms;

    public SpaceGraph() {
        this(64 * 1024);
    }

    /**
     * number of items that will remain cached, some (ideally most)
     * will not be visible but once were and may become visible again
     */
    public SpaceGraph(int cacheCapacity) {
        super();

        this.atoms = Caffeine.newBuilder()
                //.softValues().build();
                //.removalListener(this::onEvicted)
                .maximumSize(cacheCapacity)
                .weakValues()
                .build();

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
        return (Y) update(atoms.get(t, materializer));
    }

    public @Nullable Spatial getIfActive(X t) {
        Spatial v = atoms.getIfPresent(t);
        return v != null && v.active() ? v : null;
    }



    public void setGravity(v3 v) {
        dyn.setGravity(v);
    }


    public static float r(float range) {
        return (-0.5f + (float)Math.random())*2f*range;
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




    @Override final public void forEachIntSpatial(IntObjectPredicate<Spatial<X>> each) {
        int n = 0;
        for (int i = 0, inputsSize = inputs.size(); i < inputsSize; i++) {
            AbstractSpace s = inputs.get(i);
            n += s.forEachIntSpatial(n, each);
        }
    }



    public void display(GLAutoDrawable drawable) {

        List<AbstractSpace<X,?>> ss = this.inputs;

        ss.forEach( this::update );

        super.display(drawable);

        //ss.forEach(this::print);

        ss.forEach( AbstractSpace::ready );

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

        float dt = s.setBusy();

        s.update(this);

    }

    void print(AbstractSpace s) {
        System.out.println();
        //+ active.size() + " active, "
        System.out.println(s + ": "   + this.atoms.estimatedSize() + " cached; "+ "\t" + dyn.summary());
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
}
