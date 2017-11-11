package spacegraph.widget;

import spacegraph.Surface;
import spacegraph.layout.Stacking;

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
        w.set(content);
        return w;
    }


}
