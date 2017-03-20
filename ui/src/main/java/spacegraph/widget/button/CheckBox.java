package spacegraph.widget.button;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import spacegraph.widget.Label;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 11/12/16.
 */
public class CheckBox extends ToggleButton {

    public String text;
    private final Label label;

    public CheckBox(String text) {
        this.text = text;
        set(label = new Label(""));
        set(false);
    }

    public CheckBox(String text, BooleanProcedure b) {
        this(text);
        on((a,e)->b.value(e));
    }

    public CheckBox(String text, ToggleAction on) {
        this(text);
        on(on);
    }

    public CheckBox(String text, AtomicBoolean b) {
        this(text);
        set(b.get());
        on((button,value)->b.set(value));
    }
    public CheckBox(String text, MutableBoolean b) {
        this(text);
        set(b.booleanValue());
        on((button,value)->b.setValue(value));
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
