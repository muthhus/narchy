package spacegraph.obj.widget;

import spacegraph.Surface;
import spacegraph.obj.layout.VSplit;

public class LabeledPane extends VSplit {

    public LabeledPane(String title, Surface content) {
        super(new Label(title), content, 0.1f);
    }

}
