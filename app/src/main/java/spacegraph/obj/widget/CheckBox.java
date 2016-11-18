package spacegraph.obj.widget;

import com.jogamp.opengl.GL2;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 11/12/16.
 */
public class CheckBox extends ToggleButton {

    private final String text;

    public CheckBox(String text) {
        this.text = text;
    }
    public CheckBox(String text, AtomicBoolean b) {
        this(text);
        set(b.get());
        on((button,value)->{
            System.out.println(b + " " + value);
            b.set(value);
        });
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
