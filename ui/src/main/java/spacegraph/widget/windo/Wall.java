package spacegraph.widget.windo;

import spacegraph.Scale;
import spacegraph.Surface;
import spacegraph.layout.Stacking;
import spacegraph.widget.Windo;

/**
 * a wall (virtual surface) contains zero or more windows;
 * anchor region for Windo's to populate
 *
 * TODO move active window to top of child stack
 */
public class Wall extends Stacking {

    public Wall() {

        clipTouchBounds = false;

    }


    @Override
    public void doLayout() {
        //super.doLayout();
        children.forEach(Surface::layout);
    }

    public Windo addWindo() {
        Windo w = new Windo();
        children.add(w);
        return w;
    }

    public Windo addWindo(Surface content) {
        Windo w = addWindo();
        w.set(new Scale(content, 1f - Windo.resizeBorder));
        return w;
    }


}
