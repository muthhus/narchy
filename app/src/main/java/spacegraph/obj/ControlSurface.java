package spacegraph.obj;

import nars.$;
import nars.util.data.list.FasterList;
import ognl.OgnlRuntime;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

import static org.reflections.ReflectionUtils.withParametersAssignableTo;
import static org.reflections.ReflectionUtils.withTypeAssignableTo;

/**
 * Generic widget control panel for arbitrary POJOs
 */
public class ControlSurface extends GridSurface {

    /** the object being controlled */
    public Object o;
    private Map fields;
    private Map methods;
    final Set<Object> built = new HashSet();

    public static void newControlWindow(Object o) {
        SpaceGraph<?> s = new SpaceGraph();
        s.add( new RectWidget(
                new ControlSurface(o), 8f /* width */, 16f /* height */
        ) );

        //s.add(new Facial(new ConsoleSurface(new ConsoleSurface.DummyTerminal(80, 25))).scale(500f, 400f));
        s.add(new Facial(new CrosshairSurface(s)));
        s.show(800, 800);
    }

    public ControlSurface(Object o) {
        this("", o);
    }

    public ControlSurface(String label, Object o) {
        super(VERTICAL);

        reset(label, o);
    }

    private synchronized void reset(String label, Object o) {
        this.o = o;

        built.clear();
        setChildren(build(label, o));
    }

    private synchronized Surface build(Object K, Object V) {
        if (!built.add(V))
            return null;



        FasterList<Surface> w = $.newArrayList();

        ConsoleSurface vc = new ConsoleSurface(24, 4);
        try {
            vc.term.putLine(K + " = " + V);
        } catch (IOException e) {

        }
        w.add(vc);


        fields = OgnlRuntime.getFields(o.getClass());
        fields.forEach((k,v) -> {
            w.addIfNotNull(build(k,v));
        });
        methods = OgnlRuntime.getMethods(o.getClass(), false);

        methods.forEach((k, v) -> {
            w.addIfNotNull(build(k, v));
        });

        GridSurface g = new GridSurface(w, Math.random() < 0.5 ? VERTICAL : HORIZONTAL);
        return g;
    }

}
