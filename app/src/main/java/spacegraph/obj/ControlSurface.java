package spacegraph.obj;

import nars.$;
import nars.util.data.list.FasterList;
import ognl.OgnlRuntime;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;

import java.io.IOException;
import java.util.*;

/**
 * Generic widget control panel for arbitrary POJOs
 */
public class ControlSurface extends GridSurface {

    private static final int DEFAULT_DEPTH = 2;
    /** the object being controlled */
    public Object o;
    private Map fields;
    private Map methods;
    final IdentityHashMap built;

    public static void newControlWindow(Object o) {
        SpaceGraph<?> s = new SpaceGraph();
        s.add( new RectWidget(
                new ControlSurface(o), 16f /* width */, 16f /* height */
        ) );

        //s.add(new Facial(new ConsoleSurface(new ConsoleSurface.DummyTerminal(80, 25))).scale(500f, 400f));
        s.add(new Facial(new CrosshairSurface(s)));
        s.show(1200, 800);
    }

    public ControlSurface(Object o) {
        this(o.toString(), o, DEFAULT_DEPTH,  null);
    }

    public ControlSurface(Object label, Object o, int maxDepth, IdentityHashMap built) {
        super(0.5f);

        this.built = built == null ?  new IdentityHashMap() : built;
        reset(label, o, maxDepth);
    }


    private synchronized void reset(Object label, Object o, int maxDepth) {
        this.o = o;

        built.clear();
        setChildren(build(label, o, maxDepth, built));
    }

    private synchronized List<Surface> build(Object K, Object V, int remainingDepth, IdentityHashMap built) {


        FasterList<Surface> w = $.newArrayList();

        ConsoleSurface vc = new ConsoleSurface(24, 4);
        try {
            vc.term.putLine(K + " = " + V);
        } catch (IOException e) {

        }
        w.add(vc);


        Class<?> aClass = o.getClass();
        fields = OgnlRuntime.getFields(aClass);
        fields.forEach((k,v) -> {
            w.addIfNotNull(field(remainingDepth-1, k, v, built));
        });

        methods = OgnlRuntime.getMethods(aClass, false);
        methods.forEach((k, v) -> {
            w.addIfNotNull(method(remainingDepth-1, k, v, built));
        });

        return w;
    }

    private Surface field(int remainingDepth, Object k, Object v, IdentityHashMap built) {
        if (alreadyAdded(remainingDepth, v, built)) return null;
        return new ControlSurface(k, v, remainingDepth, built);
    }


    private Surface method(int remainingDepth, Object k, Object v, IdentityHashMap built) {
        if (alreadyAdded(remainingDepth, v, built)) return null;
        return new ControlSurface(k, v, remainingDepth, built);
    }


    private boolean alreadyAdded(int remainingDepth, Object v, IdentityHashMap built) {
        if (remainingDepth <= 0)
            return true;
        if (built.putIfAbsent(v,v)!=null)
            return true;
        return false;
    }

}
