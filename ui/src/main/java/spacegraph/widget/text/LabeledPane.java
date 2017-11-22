package spacegraph.widget.text;

import spacegraph.Surface;
import spacegraph.layout.VSplit;

public class LabeledPane extends VSplit {

    public LabeledPane(String title, Surface content) {
        super(new Label(title), content, 0.1f);
    }

}
