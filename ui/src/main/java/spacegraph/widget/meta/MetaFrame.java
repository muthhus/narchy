package spacegraph.widget.meta;

import spacegraph.Surface;
import spacegraph.layout.Stacking;
import spacegraph.widget.button.PushButton;

import java.awt.*;

/** a dynamic frame for attaching to widgets providing access to context menus, controls, and display */
public class MetaFrame extends Stacking {

    private final Surface base;
    private final PushButton delete;

    public MetaFrame(Surface base) {
        super();
        this.base = base;

        delete = new PushButton("X");
    }

    @Override
    public void doLayout() {
        pos(base.bounds);
        delete.pos(cx(), cy(), w()/5, h()/5); //HACK
        super.doLayout();
    }
}
