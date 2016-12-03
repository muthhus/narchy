package spacegraph.obj.layout;

import nars.$;
import spacegraph.Surface;
import spacegraph.obj.widget.CheckBox;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static spacegraph.obj.layout.Grid.grid;
import static spacegraph.obj.layout.Grid.row;

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

        top(row(toggles));
        update();
    }

    protected void update() {
        List<Surface> newContent = $.newArrayList();
        for (CheckBox c : toggles) {
            if (c.on()) {
                newContent.add(built.computeIfAbsent(c.text, k -> {
                    return builder.get(k).get();
                }));
            }
        }
        bottom(grid(newContent));
        layout();
    }

}
