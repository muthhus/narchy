package spacegraph.space.widget;

import jcog.data.FloatParam;
import jcog.list.FasterList;
import org.jetbrains.annotations.NotNull;
import spacegraph.Surface;
import spacegraph.space.layout.Grid;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 2/28/17.
 */
public class ReflectionSurface<X> extends Grid {

    private final X x;

    public ReflectionSurface(@NotNull X x) {
        this.x = x;

        List<Surface> l = new FasterList();


        Class cc = x.getClass();
        for (Field f : cc.getFields()) {
            //SuperReflect.fields(x, (String k, Class c, SuperReflect v) -> {

            try {
                String k = f.getName();
                Class c = f.getType();

                if (c == FloatParam.class) {
                    FloatParam p = (FloatParam) f.get(x);
                    l.add(col(new Label(k), new FloatSlider(p)));
                } else if (c == AtomicBoolean.class) {
                    AtomicBoolean p = (AtomicBoolean) f.get(x);
                    l.add(new CheckBox(k, p));
                }
                /*else {
                    l.add(new PushButton(k));
                }*/
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        setChildren(l);
    }
}
