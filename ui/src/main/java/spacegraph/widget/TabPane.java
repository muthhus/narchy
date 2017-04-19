package spacegraph.widget;

import jcog.list.FasterList;
import spacegraph.Surface;
import spacegraph.layout.Grid;
import spacegraph.layout.VSplit;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.ToggleButton;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

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
        header = Grid.grid();

        this.toggles = new FasterList();
        this.builder = builder;
        this.built = new ConcurrentHashMap();

        final List<Surface> togglesShown = new FasterList();
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

        header.set(togglesShown);

        update();
    }

    protected void update() {
        List<Surface> newContent = new FasterList();
        for (ToggleButton c : toggles) {
            if (c.on()) {
                newContent.add(built.computeIfAbsent(((CheckBox)c).text, k -> {
                    return new Wrapped(c, builder.get(k).get());
                }));
            }
        }
        set(header, Grid.grid(newContent), 0.1f);
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
