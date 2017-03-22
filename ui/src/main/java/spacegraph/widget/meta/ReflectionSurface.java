package spacegraph.widget.meta;

import jcog.data.FloatParam;
import jcog.list.FasterList;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import spacegraph.Surface;
import spacegraph.layout.Grid;
import spacegraph.layout.VSplit;
import spacegraph.widget.Label;
import spacegraph.widget.LabeledPane;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.slider.FloatSlider;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 2/28/17.
 */
public class ReflectionSurface<X> extends Grid {

    private final X x;

    public ReflectionSurface(@NotNull X x) {
        super();
        this.x = x;

        List<Surface> l = new FasterList();


        Class cc = x.getClass();
        for (Field f : cc.getFields()) {
            //SuperReflect.fields(x, (String k, Class c, SuperReflect v) -> {

            try {
                //Class c = f.getType();
                Object y = f.get(x);

                if (y instanceof Surface) {
                    //l.add(col(new Label(k), (Surface)y));
                    l.add((Surface)y);
                } else {
                    String k = f.getName();

                    if (y instanceof FloatParam) {
                        FloatParam p = (FloatParam) y;
                        //l.add(new LabeledPane(k, new FloatSlider(p)));
                        l.add(new MyFloatSlider(p, k));
                    } else if (y instanceof AtomicBoolean) {
                        l.add(new CheckBox(k, (AtomicBoolean) y));
                    } else if (y instanceof MutableBoolean) {
                        l.add(new CheckBox(k, (MutableBoolean) y));
                    } else if (y instanceof Runnable) {
                        l.add(new PushButton(k, (Runnable)y));
                    }
                }

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        set(l);
    }

    private static class MyFloatSlider extends FloatSlider {
        private final String k;

        public MyFloatSlider(FloatParam p, String k) {
            super(p);
            this.k = k;
        }

        @Override
        public String labelText() {
            return k + "=" + super.labelText();
        }
    }
}
