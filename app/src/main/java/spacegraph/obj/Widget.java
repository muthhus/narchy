package spacegraph.obj;

import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.render.Draw;

/**
 * Base class for GUI widgets, similarly designed to JComponent
 */
public abstract class Widget extends Surface {



    @Nullable Finger touchedBy = null;

    public Widget() {

    }

    @Override
    protected void paint(GL2 gl) {
        if (touchedBy!=null) {
            gl.glColor3f(1f,1f,0f);
            gl.glLineWidth(4);
            Draw.rectStroke(gl, 0, 0, 1, 1);
        }

        paintComponent(gl);

    }

    protected abstract void paintComponent(GL2 gl);

//    @Override
//    protected boolean onTouching(v2 hitPoint, short[] buttons) {
////        int leftTransition = buttons[0] - (touchButtons[0] ? 1 : 0);
////
////        if (leftTransition == 0) {
////            //no state change, just hovering
////        } else {
////            if (leftTransition > 0) {
////                //clicked
////            } else if (leftTransition < 0) {
////                //released
////            }
////        }
//
//
//        return false;
//    }


    public void touch(@Nullable Finger finger) {
        touchedBy = finger;
    }
}
