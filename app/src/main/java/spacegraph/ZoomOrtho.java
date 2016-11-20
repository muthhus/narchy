package spacegraph;

import com.jogamp.newt.event.MouseEvent;
import spacegraph.math.v2;

import static spacegraph.math.v3.v;

/**
 * Ortho with mouse zoom controls
 */
public class ZoomOrtho extends Ortho {

    float zoomRate = 0.1f;

    final static float minZoom = 0.1f;
    final static float maxZoom = 10f;

    final static short PAN_BUTTON = 3;

    v2 panStart;

    public ZoomOrtho(Surface surface) {
        super(surface);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);

        panStart = null;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);

        short[] bd = e.getButtonsDown();

        if (bd.length > 0 && bd[0] == PAN_BUTTON) {
            int mx = e.getX();
            int my = window.getHeight() - e.getY();
            if (panStart == null) {
                panStart = v(mx, my);
            } else {
                float dx = mx - panStart.x;
                float dy = my - panStart.y;
                move(dx, dy);
                panStart.set(mx, my);
            }
        } else {
            panStart = null;
        }
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        super.mouseWheelMoved(e);

        //when wheel rotated on negative (empty) space, adjust scale
        //if (mouse.touching == null) {
            //System.out.println(Arrays.toString(e.getRotation()) + " " + e.getRotationScale());
            float zoomMult = 1f + -e.getRotation()[1] * zoomRate;
            v2 s = scale();
            float psx = s.x;
            float psy = s.y;
            float sx = psx * zoomMult;
            float sy = psy * zoomMult;
            int wx = window.getWidth();
            int wy = window.getHeight();
            if (sx/wx >= minZoom && sy/wy >= minZoom && sx/wx <= maxZoom && sy/wy <= maxZoom) {

                s.set(sx, sy);

                float epx, epy;
                if (zoomMult > 1f) {
                    epx = -psx / 4f * ((float) e.getX() / wx - 0.5f);
                    epy = psy / 4f * ((float) e.getY() / wy - 0.5f);
                } else {
                    //TODO calculate correctly based on viewed center
                    epx = epy = 0; //zoom out from center
                }

                float dsx = sx - psx;
                float dsy = sy - psy;
                move(-dsx/2f + epx, -dsy/2f + epy); //centered on mouse
            }
        //}
    }

}
