package spacegraph.widget.button;

import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.AspectAlign;
import spacegraph.render.Draw;
import spacegraph.widget.text.Label;

import java.util.function.Consumer;

/**
 * Created by me on 11/11/16.
 */
public class PushButton extends AbstractButton {

    private final Label label;
    private final AspectAlign labelWrapper;

    @Nullable private Consumer<PushButton> onClick;

    public PushButton() {
        this("");
    }

    public PushButton(String s) {
        super();
        label = new Label(s);
        labelWrapper = new AspectAlign(label, 1f, AspectAlign.Align.Center, 1);
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

    public void setLabel(String s) {
        this.label.set(s);
    }

    @Override
    protected void paintContent(GL2 gl, float x, float y, float w, float h) {
        Draw.bounds(gl, this, labelWrapper::render);
        //labelWrapper.render(gl);
    }

    @Override
    public void doLayout() {
        //labelWrapper.pos(bounds);
        //label.pos(bounds);
        labelWrapper.layout();
    }

    @Override
    protected void onClick() {
        if (onClick!=null)
            onClick.accept(this);
    }

}
