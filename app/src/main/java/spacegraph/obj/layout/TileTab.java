package spacegraph.obj.layout;

import nars.$;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.obj.widget.CheckBox;
import spacegraph.obj.widget.PushButton;
import spacegraph.obj.widget.Widget;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static spacegraph.obj.layout.Grid.grid;

/**
 * Created by me on 12/2/16.
 */
public class TileTab extends VSplit {
    private final List<CheckBox> toggles;
    private final Map<String, Supplier<Surface>> builder;
    private final Map<String, Surface> built;


    public TileTab(Map<String, Supplier<Surface>> builder) {
        super();

        this.toggles = $.newArrayList();
        this.builder = builder;
        this.built = new ConcurrentHashMap();


        builder.forEach((k, v) -> {
            CheckBox c = (CheckBox) new CheckBox(k).on((cb, a) -> {
                update(); //TODO safer asynch
            });
            toggles.add(c);
        });

        proportion = 0.1f;

        top(grid(toggles));
        update();
    }

    protected void update() {
        List<Surface> newContent = $.newArrayList();
        for (CheckBox c : toggles) {
            if (c.on()) {
                newContent.add(built.computeIfAbsent(c.text, k -> {
                    return new Wrapped(builder.get(k).get());
                }));
            }
        }
        bottom(grid(newContent));
        layout();
    }

    private final class Wrapped extends Widget {

        Surface hover;

        public Wrapped(Surface x) {
            super(x);
            hover = grid(new PushButton("x")).scale(0.5f,0.5f);
        }

//        @Override
//        public void touch(@Nullable Finger finger) {
//            super.touch(finger);
////            if (finger == null && children.size() > 1) {
////                System.out.println("???");
////            }
//        }

        @Override
        protected boolean onTouching(v2 hitPoint, short[] buttons) {
            synchronized (TileTab.this) {
                int cont = children.size();


                if (super.onTouching(hitPoint, buttons)) {
                    return true;
                }

                if (hitPoint != null && cont == 1) {
                    children().add(hover);
                } else if (cont == 2) {
                    if (hitPoint == null)
                        children().remove(cont - 1);
                }
            }

            return false;
        }
    }
}
