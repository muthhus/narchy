package spacegraph.obj.widget;

import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Created by me on 11/11/16.
 */
public class PushButton extends AbstractButton {

    private String text;

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

    public PushButton(String s, @Nullable Consumer<PushButton> onClick) {
        this(s);
        setOnClick(onClick);
    }

    public void setOnClick(@Nullable Consumer<PushButton> onClick) {
        this.onClick = onClick;
    }

    public void setText(String s) {
        this.text = s;
    }


    @Override
    public void paintContent(GL2 gl) {
        if (text!=null) {
            label(gl, text);
        }
    }

    @Override
    protected void onClick() {
        if (onClick!=null)
            onClick.accept(this);
    }

}
