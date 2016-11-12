package spacegraph.obj.widget;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 11/12/16.
 */
public abstract class ToggleButton extends AbstractButton {

    final AtomicBoolean on = new AtomicBoolean(false);

    public interface ToggleAction {
        boolean onChange(spacegraph.obj.widget.ToggleButton t, boolean enabled);
    }

    @Nullable ToggleAction action = null;

    public ToggleButton() {

    }

    public void set(boolean on) {
        if (this.on.compareAndSet(!on, on)) {
            if (action != null)
                action.onChange(this, on);
        }
    }

    public spacegraph.obj.widget.ToggleButton on(ToggleAction a) {
        this.action = a;
        return this;
    }

    public boolean on() {
        return on.get();
    }

    @Override
    protected void onClick() {
        set(!on.get());
    }
}
