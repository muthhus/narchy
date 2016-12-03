package spacegraph.obj.layout;

import nars.$;
import spacegraph.Surface;
import spacegraph.obj.widget.CheckBox;
import spacegraph.obj.widget.ToggleButton;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static spacegraph.obj.layout.Grid.grid;

/**
 * Created by me on 12/2/16.
 */
public class TabPane extends VSplit {
    private final List<ToggleButton> toggles;
    private final Map<String, Supplier<Surface>> builder;
    private final Map<String, Surface> built;
    private final Grid header;


    public TabPane(Map<String, Supplier<Surface>> builder) {
        super();
        header = grid();

        this.toggles = $.newArrayList();
        this.builder = builder;
        this.built = new ConcurrentHashMap();

        final List<Surface> togglesShown = $.newArrayList();
        builder.forEach((k, v) -> {
            CheckBox c = (CheckBox) new CheckBox(k).on((cb, a) -> {
                List<Surface> ts = header.children;
                if (a) {
                    ts.remove( cb );
                } else {
                    ts.add( cb );
                }
                update(); //TODO safer asynch
            });
            toggles.add(c);
            togglesShown.add(c);
        });

        header.setChildren(togglesShown);

        update();
    }

    protected void update() {
        List<Surface> newContent = $.newArrayList();
        for (ToggleButton c : toggles) {
            if (c.on()) {
                newContent.add(built.computeIfAbsent(((CheckBox)c).text, k -> {
                    return new Wrapped(c, builder.get(k).get());
                }));
            }
        }
        set(header, grid(newContent), 0.1f);
    }

    private final class Wrapped extends VSplit {

        //Surface hover;

        public Wrapped(ToggleButton c, Surface x) {
            super();
            set(c, x, 0.1f);
        }

//        @Override
//        public void touch(@Nullable Finger finger) {
//            super.touch(finger);
////            if (finger == null && children.size() > 1) {
////                System.out.println("???");
////            }
//        }

//        @Override
//        protected boolean onTouching(v2 hitPoint, short[] buttons) {
//            synchronized (TileTab.this) {
//                int cont = children.size();
//
//
//                if (super.onTouching(hitPoint, buttons)) {
//                    return true;
//                }
//
//                if (hitPoint != null && cont == 1) {
//                    children().add(hover);
//                } else if (cont == 2) {
//                    if (hitPoint == null)
//                        children().remove(cont - 1);
//                }
//            }
//
//            return false;
//        }
    }
}
