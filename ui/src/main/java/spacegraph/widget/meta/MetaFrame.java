package spacegraph.widget.meta;

import spacegraph.AspectAlign;
import spacegraph.Surface;
import spacegraph.SurfaceRoot;
import spacegraph.layout.Stacking;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.text.Label;
import spacegraph.widget.windo.Widget;

import static spacegraph.layout.Grid.grid;

/**
 * a dynamic frame for attaching to widgets providing access to context menus, controls, and display
 */
public class MetaFrame extends Stacking {

    private final Widget base;


    public MetaFrame(Widget base) {
        super();
        this.base = base;


        build();

        attach(base);


    }

    protected void attach(Widget base) {
        SurfaceRoot r = base.root();
        r.the("metaframe", this, () -> {
            close();
        });

        base.children.add(this);
        r.zoom(base.cx(), base.cy(), base.w(), base.h());
    }

    protected void build() {
        Surface m = grid(
            new PushButton("@"), //tag
            new PushButton("?"), //inspect
            new PushButton("X")  //hide
        );
        children.add(new AspectAlign(m, 1f, AspectAlign.Align.RightTop, 0.25f ));

        Surface n = grid(
            new Label(base.toString() )
        );
        children.add(new AspectAlign(n, 0.25f, AspectAlign.Align.LeftTop, 0.25f ));

    }

    public void close() {
        base.children.remove(this);
    }

    @Override
    public void doLayout() {
        pos(base.bounds);
        super.doLayout();
    }
}
