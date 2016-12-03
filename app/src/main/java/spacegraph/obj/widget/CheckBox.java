package spacegraph.obj.widget;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 11/12/16.
 */
public class CheckBox extends ToggleButton {

    public String text;
    private final Label label;

    public CheckBox(String text) {
        this.text = text;
        setChildren(label = new Label(""));
        set(false);
    }

    public CheckBox(String text, AtomicBoolean b) {
        this(text);
        set(b.get());
        on((button,value)->{
            //System.out.println(b + " " + value);
            b.set(value);
        });
    }

    @Override
    public ToggleButton set(boolean on) {
        label.set((on ? "[X] " : "[ ] ") + text);
        return super.set(on);
    }

    public void setText(String s) {
        this.text = s;
    }

}
