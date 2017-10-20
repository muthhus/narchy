package spacegraph;

import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL2;
import jcog.Util;
import spacegraph.input.Finger;
import spacegraph.layout.Stacking;
import spacegraph.math.v2;
import spacegraph.phys.util.AnimVector2f;
import spacegraph.render.Draw;
import spacegraph.widget.Widget;

import java.util.concurrent.atomic.AtomicBoolean;

import static spacegraph.Surface.Align.None;

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
    private int[] moveTarget = new int[2];
    @Deprecated
    private int[] resizeTarget = new int[2];
    private int[] windowStart = new int[2];
    private InsetsImmutable windowInsets;

    final HUD HUDSurface = new HUD();
    private int pmx, pmy;

    public ZoomOrtho(Surface content) {
        super();
        setSurface(content);


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
    public void setSurface(Surface content) {
        this.surface = HUDSurface.set(content);
    }

    @Override
    public void start(SpaceGraph s) {
        super.start(s);

        //window.window.setUndecorated(true);
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


        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        pmx = e.getX();
        pmy = windowHeight - e.getY();

        if ((pmx < resizeBorder) && (pmy < resizeBorder)) {
            potentialDragMode = WindowDragMode.RESIZE_SW; //&& window.isResizable()
        } else if ((pmx > windowWidth - resizeBorder) && (pmy < resizeBorder)) {
            potentialDragMode = WindowDragMode.RESIZE_SE;  //&& window.isResizable()
        } else {
            potentialDragMode = WindowDragMode.MOVE;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragMode = null;
        panStart = null;
        super.mouseReleased(e);
    }

    final AtomicBoolean windowMoving = new AtomicBoolean(false);

    final int resizeBorder = 48; //pixels

    enum WindowDragMode {
        MOVE,
        RESIZE_NW,
        RESIZE_SW,
        RESIZE_NE,
        RESIZE_SE
    }

    final int windowMinWidth = resizeBorder * 3;
    final int windowMinHeight = resizeBorder * 3;
    WindowDragMode dragMode = null, potentialDragMode = null;


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

                    dragMode = potentialDragMode;

                    //System.out.println("window drag mode: " + dragMode);
                }

            } else {

                int dx = mx - panStart[0];
                int dy = my - panStart[1];
                if (dx == 0 && dy == 0) {

                } else {
                    if (bd[0] == PAN_BUTTON) {

                        move(dx, -dy);
                        panStart[0] = mx;
                        panStart[1] = my;

                    } else if (bd[0] == MOVE_WINDOW_BUTTON) {

                        //compute even if the window is in progress

                        if (dragMode == WindowDragMode.MOVE) {


                            if (windowMoving.compareAndSet(false, true)) {
                                moveTarget[0] = windowStart[0] + dx;
                                moveTarget[1] = windowStart[1] + dy;
                                window.window.getScreen().getDisplay().getEDTUtil().invoke(true, this::moveWindow);
                            }

                        } else if (dragMode == WindowDragMode.RESIZE_SE) {

                            int windowWidth = window.getWidth();
                            int windowHeight = window.getHeight();

                            windowStart[0] = window.windowX;
                            windowStart[1] = window.windowY;

                            moveTarget[0] = windowStart[0];
                            moveTarget[1] = windowStart[1];

                            resizeTarget[0] = Math.min(window.window.getScreen().getWidth(), Math.max(windowMinWidth, windowWidth + dx));
                            resizeTarget[1] = Math.min(window.window.getScreen().getHeight(), Math.max(windowMinHeight, windowHeight + dy));

                            if (windowMoving.compareAndSet(false, true)) {

                                window.window.getScreen().getDisplay().getEDTUtil().invoke(false, () ->
                                        resizeWindow(windowStart[0], windowStart[1], resizeTarget[0], resizeTarget[1]));
                                //this::resizeWindow);
                                if (panStart != null) {
                                    panStart[0] = mx;
                                    panStart[1] = my;
                                }
                            }

                        }

                    }
                }
            }
        } else {
            panStart = null;
            dragMode = null;
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        potentialDragMode = null;
    }

    private void moveWindow() {
        try {
            window.window.setPosition(moveTarget[0], moveTarget[1]);
        } finally {
            windowMoving.set(false);
        }
    }

    private void resizeWindow(int x, int y, int w, int h) {
        try {
            //System.out.println(Arrays.toString(moveTarget) + " " + Arrays.toString(resizeTarget));
            window.window.setSize(w, h);
            window.window.setPosition(x, y);
        } catch (Throwable t) {
            t.printStackTrace();
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

    private class HUD extends Stacking {

        float smx, smy;


        {
            align = None;
            aspect = 1f;
        }

        final Widget bottomRightMenu = new Widget() {

            @Override
            protected void paintComponent(GL2 gl) {

            }
        };


        @Override
        protected void paint(GL2 gl) {
            super.paint(gl);

            gl.glPushMatrix();
            gl.glLoadIdentity();

            int W = window.getWidth();
            int H = window.getHeight();

            gl.glColor4f(0.8f, 0.6f, 0f, 0.25f);

            int borderThick = 8;
            gl.glLineWidth(borderThick);
            Draw.line(gl, 0, 0, W, 0);
            Draw.line(gl, 0, 0, 0, H);
            Draw.line(gl, W, 0, W, H);
            Draw.line(gl, 0, H, W, H);

            WindowDragMode p;
            if ((p = potentialDragMode)!=null) {
                switch (p) {
                    case RESIZE_SE:
                        gl.glColor4f(1f, 0.8f, 0f, 0.5f);
                        Draw.quad2d(gl, pmx, pmy, W, resizeBorder, W, 0, W-resizeBorder, 0);
                        break;
                    case RESIZE_SW:
                        gl.glColor4f(1f, 0.8f, 0f, 0.5f);
                        Draw.quad2d(gl, pmx, pmy, 0, resizeBorder, 0, 0, resizeBorder, 0);
                        break;
                }
            }

            gl.glLineWidth(8f);

            float ch = 175f; //TODO proportional to ortho height (pixels)
            float cw = 175f; //TODO proportional to ortho width (pixels)

            gl.glColor4f(0.5f, 0.5f, 0.5f, 0.25f);
            Draw.rectStroke(gl, smx - cw / 2f, smy - ch / 2f, cw, ch);

            gl.glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
            Draw.line(gl, smx, smy - ch, smx, smy + ch);
            Draw.line(gl, smx - cw, smy, smx + cw, smy);
            gl.glPopMatrix();

        }

        {
//            set(
//                    overlay
//                    //bottomRightMenu.scale(64,64)
//            );
        }

        {
//            clipTouchBounds = false;
        }

        boolean canDragBR = false;

        @Override
        public Surface onTouch(Finger finger, v2 hitPoint, short[] buttons) {

            //System.out.println(hitPoint);
            if (hitPoint != null) {
                float hudMarginThick = 0.05f; //pixels

                smx = finger.hitGlobal.x;
                smy = finger.hitGlobal.y;

                //boolean nearEdge = Math.abs(sx - )
                canDragBR = (smx > 1f - hudMarginThick && smy > hudMarginThick);
//                    if (canDragBR) {
//                        System.out.println("draggable");
//                    }
            } else {
                canDragBR = false;
            }

            Surface x = super.onTouch(finger, hitPoint, buttons);

            if (x == this) {
                return null; //pass-thru
            } else
                return x;
        }

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
