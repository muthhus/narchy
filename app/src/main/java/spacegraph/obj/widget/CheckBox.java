package spacegraph.obj.widget;

import com.jogamp.opengl.GL2;

/**
 * Created by me on 11/12/16.
 */
public class CheckBox extends ToggleButton {

    private final String text;

    public CheckBox(String text) {
        this.text = text;
    }

    @Override
    public void paintContent(GL2 gl) {
        if (this.text != null) {
            label(gl,
                    (on() ? "[X] " : "[ ] ") +
                            text);
        }

    }
}
