package spacegraph.widget.button;

import org.jetbrains.annotations.Nullable;
import spacegraph.widget.AbstractButton;
import spacegraph.widget.Label;

import java.util.function.Consumer;

/**
 * Created by me on 11/11/16.
 */
public class PushButton extends AbstractButton {

    private Label text;

    @Nullable private Consumer<PushButton> onClick;

    public PushButton() {
    }

    public PushButton(String s) {
        this();
        setText(s);
    }

    public PushButton(Consumer<PushButton> onClick) {
        this();
        setOnClick(onClick);
    }

    public PushButton(String s, Runnable onClick) {
        this(s, (p) -> onClick.run());
    }

    public PushButton(String s, @Nullable Consumer<PushButton> onClick) {
        this(s);
        setOnClick(onClick);
    }

    public void setOnClick(@Nullable Consumer<PushButton> onClick) {
        this.onClick = onClick;
    }

    public void setText(String s) {
        set(text = new Label(s));
    }


    @Override
    protected void onClick() {
        if (onClick!=null)
            onClick.accept(this);
    }

}
