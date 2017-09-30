package spacegraph.widget;

import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;
import spacegraph.render.Draw;

/**
 * Created by me on 11/12/16.
 */
public abstract class AbstractButton extends Widget {
    float pushed; //how depresssed the button is, from 0= not touched, to 1=push through the screen
    private boolean pressed;
    private final boolean enabled = true;

    @Override
    protected void paintComponent(GL2 gl) {
        paintBack(gl);
        paintContent(gl);
    }

    protected void paintContent(GL2 gl) {

    }

    public void paintBack(GL2 gl) {
        float p = pushed / 2f;
        float dim = 1f - (pushed /* + if disabled, dim further */) * 3f;
        gl.glColor3f(0.25f * dim, 0.25f * dim, 0.25f * dim);
        Draw.rect(gl, p, p, 1 - 2 * p, 1 - 2 * p);
    }

    @Override
    public void touch(@Nullable Finger finger) {
        super.touch(finger);

        boolean nowPressed = false;
        if (finger != null) {
            if (finger.clickReleased(0)) {
                pushed = 0;
                onClick();
            } else if (finger.pressed(0)) {
                pushed = 0.05f;
            } else {
                pushed = 0;
            }
        } else {
            pushed = 0;
        }

    }

    protected abstract void onClick();

//    static void label(GL2 gl, String text) {
//        gl.glColor3f(0.75f, 0.75f, 0.75f);
//        gl.glLineWidth(2);
//        Draw.text(gl, text, 1f/(1+text.length()), 0.5f, 0.5f,  0);
//    }

}
