package spacegraph;

import com.jogamp.opengl.GL2;
import nars.$;
import nars.util.list.FasterList;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.FPSLook;
import spacegraph.input.KeyXYZ;
import spacegraph.input.OrbMouse;
import spacegraph.math.v3;
import spacegraph.phys.constraint.BroadConstraint;
import spacegraph.render.JoglPhysics;
import spacegraph.source.ListSpace;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static spacegraph.math.v3.v;

/**
 * Created by me on 6/20/16.
 */
public class SpaceGraph<X> extends JoglPhysics<X> {


    final List<Ortho> orthos = new FasterList<>(1);

    final List<AbstractSpace<X,Spatial<X>>> inputs = new FasterList<>(1);

    final Map<X, Spatial<X>> atoms;

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
                //new NonBlockingHashMap(cacheCapacity);
                new ConcurrentHashMap<>(cacheCapacity);
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

    public SpaceGraph(Ortho o) {
        this();
        add(o);
    }


//    private void onEvicted(O k1, Spatial<O> v1, RemovalCause removalCause) {
//        //..
//    }

    final List<Ortho> preAdd = $.newArrayList();



    public SpaceGraph add(Ortho c) {
        if (window == null) {
            preAdd.add(c);
        } else {
            _add(c);
        }
        return this;
    }

    private void _add(Ortho c) {
        if (this.orthos.add(c))
            c.start(this);
    }

    public SpaceGraph add(AbstractSpace<X,Spatial<X>> c) {
        if (inputs.add(c))
            c.start(this);
        return this;
    }

    public void remove(AbstractSpace<X,?> c) {
        if (inputs.remove(c)) {
            c.stop();
        }
    }

    public @NotNull <Y extends Spatial<X>>  Y update(X instance, Function<X, Y> materializer) {
        return getOrAdd(instance, materializer);
    }



    public @NotNull <Y extends Spatial<X>> Y getOrAdd(X x, Function<X, ? extends Y> materializer) {
        Spatial<X> y = atoms.computeIfAbsent(x, materializer);
        y.reactivate(true);
        return (Y) y;
    }

    public @Nullable Spatial getIfActive(X t) {
        Spatial v = atoms.get(t);
        return v != null && v.active() ? v : null;
    }



    public SpaceGraph setGravity(v3 v) {
        dyn.setGravity(v);
        dyn.forEachCollidable((i,c)->{
            c.setGravity(v);
            c.setActivationState(1);
        });
        return this;
    }


    public static float r(float range) {
        return (-0.5f + (float)Math.random())*2f*range;
    }


    public void init(GL2 gl) {
        super.init(gl);


        for (Ortho f : preAdd) {
            _add(f);
        }
        preAdd.clear();


        initInput();

    }



    protected void initInput() {

        //default 3D input controls
        addMouseListener(new FPSLook(this));
        addMouseListener(new OrbMouse(this));
        addKeyListener(new KeyXYZ(this));

    }


    @Override final public void forEachIntSpatial(IntObjectPredicate<Spatial<X>> each) {
        int n = 0;
        for (int i = 0, inputsSize = inputs.size(); i < inputsSize; i++) {
            n += inputs.get(i).forEachIntSpatial(n, each);
        }
    }

    @Override
    protected void render() {
        super.render();
        renderHUD();
    }

    @Override
    protected void update() {

        this.inputs.forEach( this::update );

        super.update();
    }

    protected void renderHUD() {

        int facialsSize = orthos.size();
        if (facialsSize > 0) {

            ortho();

            gl.glDisable(GL2.GL_DEPTH_TEST);

            GL2 gl = this.gl;
            for (int i = 0; i < facialsSize; i++) {
                orthos.get(i).render(gl);
            }

            gl.glEnable(GL2.GL_DEPTH_TEST);
        }
    }


    final void update(AbstractSpace<X,Spatial<X>> s) {

        s.forEach(x -> x.update(this));

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
        ListSpace<X, Spatial<X>> l = new ListSpace<>(s);
        add(l);
        return l;
    }

    public static SpaceGraph window(Surface s, int w, int h) {
        SpaceGraph win = new SpaceGraph(new ZoomOrtho( s )
            //.scale(Math.min(w,h))
                .maximize()
        );
        win.show(w, h);
        return win;
    }

    public static SpaceGraph window(Spatial s, int w, int h) {
        return window(w, h, s);
    }

    public static SpaceGraph window(int w, int h, Spatial... s) {
        SpaceGraph win = new SpaceGraph(s);
        win.show(w, h);
        return win;
    }

    @Deprecated public SpaceGraph with(BroadConstraint b) {
        dyn.addBroadConstraint(b);
        return this;
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

}
