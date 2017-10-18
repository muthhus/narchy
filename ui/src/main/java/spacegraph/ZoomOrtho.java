package spacegraph;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL2;
import spacegraph.layout.Stacking;
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

    final Stacking overlay = new Stacking();

    public ZoomOrtho(Surface surface) {
        super(surface);

//        this.surface = new Stacking(this.surface, overlay);
//        overlay.children().add(new Widget() {
//
//            @Override
//            protected void paintComponent(GL2 gl) {
//
//                gl.glColor4f(1f, 0f, 1f, 0.3f);
//
//
//                pos(cx(), cy());
//
//                float w = (ZoomOrtho.this.window.getWidth() / ZoomOrtho.this.scale.x);
//                float h = (ZoomOrtho.this.window.getHeight() / ZoomOrtho.this.scale.y);
//                scale(w, h);
//
//                Draw.rect(gl, 0.25f, 0.25f, 0.5f, 0.5f);
//            }
//
//        });
    }

    public float cw() {
        return (ZoomOrtho.this.window.getWidth() / ZoomOrtho.this.scale.x);
    }
    public float ch() {
        return (ZoomOrtho.this.window.getHeight() / ZoomOrtho.this.scale.y);
    }

    public float cx() {
        return (0.5f - pos.x)/scale.x;
    }

    public float cy() {
        return (0.5f - pos.y)/scale.y;
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

            float psx = scale.x;
            float psy = scale.y;
            float sx = psx * zoomMult;
            float sy = psy * zoomMult;
            int wx = window.getWidth();
            int wy = window.getHeight();

            if (sx/wx >= minZoom && sy/wy >= minZoom && sx/wx <= maxZoom && sy/wy <= maxZoom) {

                float pcx = cx();
                float pcy = cy();
                float pcw = cw();
                float pch = ch();

                scale.set(sx, sy);

                float ncx = cx();
                float ncy = cy();
                float ncw = cw();
                float nch = ch();

                //pos.set()
                //TODO

            }
        //}
    }

}
//    @Override
//    protected Finger newFinger() {
//        return new DebugFinger(this);
//    }
//
//    class DebugFinger extends Finger {
//
//        final Surface overlay = new Surface() {
//
//            @Override
//            protected void paint(GL2 gl) {
//                super.paint(gl);
//
//                gl.glColor4f(1f,1f, 0f, 0.85f);
//                gl.glLineWidth(3f);
//                Draw.rectStroke(gl, 0,0,10,5);
//            }
//        };
//
//        public DebugFinger(Ortho root) {
//            super(root);
//        }
//
//        protected void start() {
//            //window.add(new Ortho(overlay).maximize());
//        }
//    }
