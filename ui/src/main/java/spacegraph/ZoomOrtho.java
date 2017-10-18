package spacegraph;

import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.newt.event.MouseEvent;
import jcog.Util;
import org.apache.commons.math3.util.ArithmeticUtils;
import spacegraph.input.Finger;

/**
 * Ortho with mouse zoom controls
 */
public class ZoomOrtho extends Ortho {

    float zoomRate = 0.2f;

    final static float minZoom = 0.1f;
    final static float maxZoom = 10f;

    final static short PAN_BUTTON = 3;
    final static short MOVE_WINDOW_BUTTON = 2;

    private int[] panStart = null;
    private int[] windowStart = null;
    private InsetsImmutable windowInsets;


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

    @Override
    public void start(SpaceGraph s) {
        super.start(s);

        window.window.setUndecorated(true);
        window.window.setResizable(false);
    }

    public float cw() {
        return (ZoomOrtho.this.window.getWidth() / ZoomOrtho.this.scale.x);
    }

    public float ch() {
        return (ZoomOrtho.this.window.getHeight() / ZoomOrtho.this.scale.y);
    }

    public float cx() {
        return (0.5f - pos.x) / scale.x;
    }

    public float cy() {
        return (0.5f - pos.y) / scale.y;
    }


    @Override
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);

        panStart = null;
    }

    boolean windowMoveWait = false;

    @Override
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);

        short[] bd = e.getButtonsDown();

        if (bd.length > 0 && (bd[0] == PAN_BUTTON) || (bd[0] == MOVE_WINDOW_BUTTON)) {
            //int mx = e.getX();
            //int my = window.getHeight() - e.getY();
            int mx = Finger.pointer.getX();
            int my = Finger.pointer.getY();
            if (panStart == null) {
                panStart = new int[2];
                panStart[0] = mx;
                panStart[1] = my;


                Point p = new Point();
                window.window.getLocationOnScreen(p);
                windowStart = new int[2];
                windowStart[0] = p.getX();
                windowStart[1] = p.getY();

                windowInsets = window.window.getInsets();

            } else {
                int dx = mx - panStart[0];
                int dy = my - panStart[1];
                if (bd[0] == PAN_BUTTON) {
                    move(-dx, dy);
                    panStart[0] = mx;
                    panStart[1] = my;
                } else if (bd[0] == MOVE_WINDOW_BUTTON) {

//                    if (!windowMoveWait) {
//                        windowMoveWait = true;
//                        window.window.getScreen().getDisplay().getEDTUtil().invoke(false, () -> {
//                            Point p = new Point();
//                            window.window.getLocationOnScreen(p);
//                            int cx = p.getX();
//                            int cy = p.getY();

                            //wait for EDT to update the window, dont spam it
                            //if (windowNext[0] == cx && windowNext[1] == cy) {
//                            int grid = 5;
//                            int fx = (int)Util.round((windowStart[0] + (mx - panStart[0])), grid);
//                            int fy = (int)Util.round((windowStart[1] - (my - panStart[1])), grid);
                            int fx = windowStart[0] + (mx - panStart[0]);
                            int fy = windowStart[1] + (my - panStart[1]);


                            //dont re-issue set position unless it has changed
                            window.window.setTopLevelPosition(fx - windowInsets.getLeftWidth(), fy - windowInsets.getTopHeight());
//                            window.window.getScreen().getDisplay().dispatchMessages();
//                            window.window.getScreen().getDisplay().getEDTUtil().waitUntilIdle();

                      //      }
//
//                            windowMoveWait = false;
//                        });
                    //}
                }
            }
        } else {
            panStart = null;
            windowStart = null;
        }
    }


    @Override
    public void mouseWheelMoved(MouseEvent e) {
        super.mouseWheelMoved(e);

        //when wheel rotated on negative (empty) space, adjust scale
        //if (mouse.touching == null) {
        //System.out.println(Arrays.toString(e.getRotation()) + " " + e.getRotationScale());
        float zoomMult = Util.clamp(1f + -e.getRotation()[1] * zoomRate, 0.5f, 1.5f);

        float psx = scale.x;
        float psy = scale.y;
        float sx = psx * zoomMult;
        float sy = psy * zoomMult;
        int wx = window.getWidth();
        int wy = window.getHeight();

        if (sx / wx >= minZoom && sy / wy >= minZoom && sx / wx <= maxZoom && sy / wy <= maxZoom) {

            float pcx = cx();
            float pcy = cy();
            float pcw = cw();
            float pch = ch();

            float dx = (pcw * psx - pcw * sx) / 2;
            float dy = (pch * psy - pch * sy) / 2;
            //System.out.println(pcx + " " + pcy + " : " + dx + " " + dy);
            pos.add(dx, dy, 0);
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
