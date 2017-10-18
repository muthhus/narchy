package spacegraph;

import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.newt.event.MouseEvent;
import jcog.Util;
import spacegraph.input.Finger;
import spacegraph.phys.util.AnimVector2f;

import java.util.concurrent.atomic.AtomicBoolean;

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
    private int[] windowTarget = new int[2];
    private int[] windowStart = new int[2];
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
        AnimVector2f s = (AnimVector2f) this.scale;
        return (ZoomOrtho.this.window.getWidth() / s.targetX());
    }

    public float ch() {
        AnimVector2f s = (AnimVector2f) this.scale;
        return (ZoomOrtho.this.window.getHeight() / s.targetY());
    }

    public float cx() {
        AnimVector2f s = (AnimVector2f) this.scale;
        return (0.5f - pos.x) / s.targetX();
    }

    public float cy() {
        AnimVector2f s = (AnimVector2f) this.scale;
        return (0.5f - pos.y) / s.targetY();
    }


    @Override
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);

        panStart = null;
    }

    final AtomicBoolean windowMoving = new AtomicBoolean(false);

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

                if (bd[0] == MOVE_WINDOW_BUTTON) {
                    //TODO compute this in the EDT on the first invocation
                    //Point p = new Point();

                    windowStart[0] = window.windowX;
                    windowStart[1] = window.windowY;
                    windowInsets = window.window.getInsets();
                }

            } else {

                if (bd[0] == PAN_BUTTON) {
                    int dx = mx - panStart[0];
                    int dy = my - panStart[1];
                    move(dx, -dy);
                    panStart[0] = mx;
                    panStart[1] = my;

                } else if (bd[0] == MOVE_WINDOW_BUTTON) {

                    //compute even if the window is in progress

                    windowTarget[0] = windowStart[0] + (mx - panStart[0]);
                    windowTarget[1] = windowStart[1] + (my - panStart[1]);

                    if (windowMoving.compareAndSet(false, true)) {
                        window.window.getScreen().getDisplay().getEDTUtil().invoke(false, this::moveWindow);
                    }

                }
            }
        } else {
            panStart = null;
        }
    }

    private void moveWindow() {
        try {
            window.window.setPosition(windowTarget[0] - windowInsets.getLeftWidth(), windowTarget[1] - windowInsets.getTopHeight());
        } finally {
            windowMoving.set(false);
        }
    }


    @Override
    public void mouseWheelMoved(MouseEvent e) {
        super.mouseWheelMoved(e);

        //when wheel rotated on negative (empty) space, adjust scale
        //if (mouse.touching == null) {
        //System.out.println(Arrays.toString(e.getRotation()) + " " + e.getRotationScale());
        float dWheel = e.getRotation()[1];


        float zoomMult = Util.clamp(1f + -dWheel * zoomRate, 0.5f, 1.5f);

        AnimVector2f s = (AnimVector2f) this.scale;
        float psx = s.targetX();
        float psy = s.targetY();
        float sx = psx * zoomMult;
        float sy = psy * zoomMult;
        int wx = window.getWidth();
        int wy = window.getHeight();

        if (sx / wx >= minZoom && sy / wy >= minZoom && sx / wx <= maxZoom && sy / wy <= maxZoom) {

//            float pcx = cx();
//            float pcy = cy();
//            float pcw = cw();
//            float pch = ch();

            //float dx = (pcw * psx - pcw * sx) / 2;
            //float dy = (pch * psy - pch * sy) / 2;
            //System.out.println(pcx + " " + pcy + " : " + dx + " " + dy);
            //pos.add(dx, dy, 0);
            this.scale.set(sx, sy);

//            float ncx = cx();
//            float ncy = cy();
//            float ncw = cw();
//            float nch = ch();

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
