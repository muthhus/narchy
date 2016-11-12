package spacegraph.obj.widget;

import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;
import spacegraph.render.Draw;

/**
 * Created by me on 11/12/16.
 */
public abstract class AbstractButton extends Widget {
    float depression = 0f; //how depresssed the button is, from 0= not touched, to 1=push through the screen
    private boolean pressed;
    private boolean enabled = true;

    @Override
    protected void paintComponent(GL2 gl) {
        paintBack(gl);
        paintContent(gl);
    }

    public abstract void paintContent(GL2 gl);

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

    protected abstract void onClick();

    static void label(GL2 gl, String text) {
        gl.glColor3f(0.75f, 0.75f, 0.75f);
        gl.glLineWidth(2);
        Draw.text(gl, text, 0.1f, 0.5f, 0.5f,  0);
    }

}
