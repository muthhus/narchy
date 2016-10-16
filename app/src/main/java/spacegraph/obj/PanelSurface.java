package spacegraph.obj;

import spacegraph.Surface;

public class PanelSurface extends VSurface  {

    public PanelSurface(String title, Surface content) {
        super(new LabelSurface(title), content, 0.9f);
    }

    public static PanelSurface of(String title, Surface content) {
        return new PanelSurface(title, content);
    }
}
