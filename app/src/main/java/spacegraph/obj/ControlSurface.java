package spacegraph.obj;

import nars.$;
import nars.util.data.list.FasterList;
import ognl.OgnlException;
import ognl.OgnlRuntime;
import org.infinispan.cdi.common.util.Reflections;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Generic widget control panel for arbitrary POJOs
 */
public class ControlSurface extends GridSurface {

    private static final int DEFAULT_DEPTH = 3;
    /** the object being controlled */
    public Object o;


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
        super(HORIZONTAL);

        this.built = built == null ?  new IdentityHashMap() : built;
        reset(label, o, maxDepth);
    }


    private synchronized void reset(Object label, Object o, int maxDepth) {
        this.o = o;

        built.clear();
        built.put(o, o);

        FasterList<Surface> subs = $.newArrayList();
        children(o, maxDepth, built, subs);

        Surface content = build(label, o, maxDepth);
        if (subs.isEmpty()) {
            setChildren(content);
        } else {
            setChildren(content, new GridSurface(subs));
        }
    }

    protected Surface build(Object k, Object v, int remainingDepth) {


//        ConsoleSurface vc = new ConsoleSurface(24, 4);
//        try {
//            vc.term.putLine(k + "\n  " + v);
//        } catch (IOException e) {
//
//        }
        if (v instanceof Surface) {
            return ((Surface) v);
        } //else if (v instanceof String) {
            return new LabelSurface(k.toString());
         /*else {
            return new ControlSurface(k, v, remainingDepth, built);
        }*/



        //return new GridSurface(
                //vc.term.putLine(k + "\n  " + v);
                //vc );

    }

    private void children(Object V, int remainingDepth, IdentityHashMap built, FasterList<Surface> w) {





        Class<?> aClass = o.getClass();
//        Map fields = OgnlRuntime.getFields(aClass);
//        try {
//
//            Iterator z = OgnlRuntime.getElementsAccessor(aClass).getElements(o).asIterator();
//            while (z.hasNext()) {
//                System.out.println(z.next());
//            }
//
//        } catch (OgnlException e) {
//            e.printStackTrace();
//        }
//        fields.forEach((k,v) -> {
//            w.addIfNotNull(field(remainingDepth-1, k, v, built));
//        });

        Set<Field> fields = Reflections.getAllDeclaredFields(aClass);
        for (Field f : fields) {
            try {
                w.addIfNotNull(field(remainingDepth-1, f, f.get(o), built));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        Map methods = OgnlRuntime.getMethods(aClass, false);
        methods.forEach((k, v) -> {
            w.addIfNotNull(method(remainingDepth-1, k, v, built));
        });


    }

    private Surface field(int remainingDepth, Object k, Object v, IdentityHashMap built) {
        if (alreadyAdded(remainingDepth, v, built)) return null;
        return build(k, v, remainingDepth);
    }


    private Surface method(int remainingDepth, Object k, Object v, IdentityHashMap built) {
        if (alreadyAdded(remainingDepth, v, built)) return null;
        return build(k, v, remainingDepth);
    }


    private synchronized boolean alreadyAdded(int remainingDepth, Object v, IdentityHashMap built) {
        if (remainingDepth <= 0)
            return true;
        if (built.putIfAbsent(v,v)!=null)
            return true;
        return false;
    }

}
