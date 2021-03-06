package spacegraph.input;

import com.jogamp.newt.event.MouseEvent;
import spacegraph.math.v3;
import spacegraph.render.JoglPhysics;

import static com.jogamp.opengl.math.FloatUtil.sin;
import static java.lang.Math.cos;
import static spacegraph.math.v3.v;

/**
 * Created by me on 11/20/16.
 */
public class FPSLook extends SpaceMouse {

    boolean dragging;
    private int prevX, prevY;
    float h = (float) Math.PI; //angle
    float v; //angle

    public FPSLook(JoglPhysics g) {
        super(g);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
    }


    @Override
    public void mouseDragged(MouseEvent e) {
        short[] bd = e.getButtonsDown();
        if (bd.length > 0 && bd[0] == 3 /* RIGHT */) {
            if (!dragging) {
                prevX = e.getX();
                prevY = e.getY();
                dragging = true;
            }

            int x = e.getX();
            int y = e.getY();

            int dx = x - prevX;
            int dy = y - prevY;

            float angleSpeed = 0.001f;
            h += -dx * angleSpeed;
            v += -dy * angleSpeed;

            v3 direction = v(
                    (float) (cos(this.v) * sin(h)),
                    sin(this.v),
                    (float) (cos(this.v) * cos(h))
            );

            //System.out.println("set direction: " + direction);

            space.camFwd.set(direction);

            prevX = x;
            prevY = y;
        }
    }

}
