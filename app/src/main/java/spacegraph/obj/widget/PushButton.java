package spacegraph.obj.widget;

import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;
import spacegraph.render.Draw;

import java.util.function.Consumer;

/**
 * Created by me on 11/11/16.
 */
public class PushButton extends Widget {

    private String text;

    float depression = 0f; //how depresssed the button is, from 0= not touched, to 1=push through the screen
    private boolean pressed, enabled = true;

    @Nullable private Consumer<PushButton> onClick;

    public PushButton(String s) {
        setText(s);
    }

    public PushButton(String s, Consumer<PushButton> onClick) {
        this(s);
        setOnClick(onClick);
    }

    public void setOnClick(Consumer<PushButton> onClick) {
        this.onClick = onClick;
    }

    public void setText(String s) {
        this.text = s;
    }

    @Override
    protected void paintComponent(GL2 gl) {
        paintBack(gl);
        paintLabel(gl);
    }

    public void paintLabel(GL2 gl) {
        gl.glColor3f(0.75f, 0.75f, 0.75f);
        Draw.text(gl, text, 0.1f, 0.5f, 0.5f, 0);
    }

    public void paintBack(GL2 gl) {
        float p = depression/2f;
        float dim = 1f - (depression /* + if disabled, dim further */ ) * 3f;
        gl.glColor3f(0.25f * dim, 0.25f * dim, 0.25f * dim);
        Draw.rect(gl, p, p, 1 - 2 * p, 1 - 2 * p);
    }

    @Override
    public void touch(@Nullable Finger finger) {
        super.touch(finger);

        boolean pressed = false;

        if (finger == null) {
            depression = 0;
        } else {
            if (enabled && finger.buttonDown[0]) {
                depression = 0.1f;
                pressed = true;
            } else {
                depression = 0.05f;
            }
        }

        boolean wasPressed = this.pressed;
        this.pressed = pressed;
        if (!pressed && wasPressed) {
            onClick();
        }
    }

    private void onClick() {
        if (onClick!=null)
            onClick.accept(this);
    }

}
