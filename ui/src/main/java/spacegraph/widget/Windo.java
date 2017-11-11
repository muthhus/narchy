package spacegraph.widget;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.Scale;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.render.Draw;
import spacegraph.render.GridTex;
import spacegraph.widget.button.PushButton;

import static spacegraph.layout.Grid.grid;
import static spacegraph.widget.Windo.WindowDragging.MOVE;

/**
 * draggable panel
 */
public class Windo extends Widget {

    public enum WindowDragging {
        MOVE,
        RESIZE_N, RESIZE_E, RESIZE_S, RESIZE_W,
        RESIZE_NW,
        RESIZE_SW,
        RESIZE_NE,
        RESIZE_SE
    }

    public WindowDragging dragMode = null;
    public WindowDragging potentialDragMode = null;

    public final static float resizeBorder = 0.1f;
    protected final static float maxAspectRatioChange = 0.1f;

    private boolean hover;
//    float pmx, pmy;

    public Windo() {
        super();

//        Surface menubar = //row(new PushButton("x"),
//                new Label(title)
//        ;
//
//        PushButton upButton = new PushButton("^");
//        upButton.scale(0.25f,0.25f).move(0.5f,0.75f);
//
//        PushButton leftButton = new PushButton("<", (p) -> Windo.this.move(-1f, 0f));
//        leftButton.scale(0.25f,0.25f).move(0f, 0.5f);
//
//        PushButton rightButton = new PushButton(">", (p) -> Windo.this.move(1f, 0));
//        rightButton.scale(0.25f,0.25f).move(0.75f, 0.5f);
//
//        set(
//                upButton, leftButton, rightButton,
//                new VSplit(menubar, content, 0.1f)
//                );
    }


//    @Override
//    protected boolean onTouching(v2 hitPoint, short[] buttons) {
//
//        if (super.onTouching(hitPoint, buttons)) {
//            return true;
//        }
//
//        boolean lb = leftButton(buttons);
//        if (hitPoint!=null && dragStart == null && lb) {
//            //drag start
//            dragStart = new v2(hitPoint);
//        } else if ((hitPoint == null || !lb) && dragStart!=null) {
//            //drag release
//            dragStart = null;
//        }
//
//        if (hitPoint!=null && dragStart!=null) {
//            //float dx =  2 * ( hitPoint.x - dragStart.x );
//            //float dy =  2 * ( hitPoint.y - dragStart.y );
//            //move(dx, dy);
//            //return true;
//        }
//
//
//        return false;
//    }

    RectFloat2D before = null;

    @Override
    public Surface onTouch(Finger finger, v2 hitPoint, short[] buttons) {

        if (!editable())
            return super.onTouch(finger, hitPoint, buttons); //pass-through

        if (finger != null) {
            float fx = finger.hit.x;
            float fy = finger.hit.y;

            if (dragMode == null && !bounds.contains(fx, fy)) {
                hover = false;
                dragMode = null;
                return null;
            }

            Surface s = //dragMode == null ? super.onTouch(finger, hitPoint, buttons) : this;
                    super.onTouch(finger, hitPoint, buttons);

            if (s == this) {

                //if (moveable()) System.out.println(bounds + "\thit=" + finger.hit + "\thitGlobal=" + finger.hitGlobal);

                if (hitPoint != null) {

                    hover = true;


                    if (dragMode == null) {

                        potentialDragMode = null;

                        if (potentialDragMode == null && hitPoint.x >= 0.5f - resizeBorder / 2f && hitPoint.x <= 0.5f + resizeBorder / 2) {
                            if (potentialDragMode == null && hitPoint.y <= resizeBorder) {
                                potentialDragMode = WindowDragging.RESIZE_S;
                            }
                            if (potentialDragMode == null && hitPoint.y >= 1f - resizeBorder) {
                                potentialDragMode = WindowDragging.RESIZE_N;
                            }
                        }

                        if (potentialDragMode == null && hitPoint.y >= 0.5f - resizeBorder / 2f && hitPoint.y <= 0.5f + resizeBorder / 2) {
                            if (potentialDragMode == null && hitPoint.x <= resizeBorder) {
                                potentialDragMode = WindowDragging.RESIZE_W;
                            }
                            if (potentialDragMode == null && hitPoint.x >= 1f - resizeBorder) {
                                potentialDragMode = WindowDragging.RESIZE_E;
                            }
                        }

                        if (potentialDragMode == null && hitPoint.x <= resizeBorder) {
                            if (potentialDragMode == null && hitPoint.y <= resizeBorder) {
                                potentialDragMode = WindowDragging.RESIZE_SW;
                            }
                            if (potentialDragMode == null && hitPoint.y >= 1f - resizeBorder) {
                                potentialDragMode = WindowDragging.RESIZE_NW;
                            }
                        }

                        if (potentialDragMode == null && hitPoint.x >= 1f - resizeBorder) {

                            if (potentialDragMode == null && hitPoint.y <= resizeBorder) {
                                potentialDragMode = WindowDragging.RESIZE_SE;
                            }
                            if (potentialDragMode == null && hitPoint.y >= 1f - resizeBorder) {
                                potentialDragMode = WindowDragging.RESIZE_NE;
                            }
                        }


                        if (potentialDragMode == null)
                            potentialDragMode = MOVE;
                    }


                    boolean bDrag = buttons != null && buttons.length > 0 && buttons[0] == 1;
                    if (bDrag) {
                        if (dragMode == null && potentialDragMode != null) {

                            if (editable(potentialDragMode)) {
                                dragMode = potentialDragMode;
                                //finger.lock(0, )..
                                before = bounds; //TODO store these in a shared Finger-context "posOnHit" field, not in this instance
                            } else {
                                dragMode = null;
                            }

                        }
                        if (dragMode != null) {
                            switch (dragMode) {
                                case RESIZE_N: {
                                    float pmy = before.min.y;
                                    float bh = before.h();
                                    float ty = (fy - finger.hitOnDown[0].y);
                                    pos(before.min.x, pmy, before.max.x, Math.max(pmy + maxAspectRatioChange * bh, bh + pmy + ty));
                                }
                                break;
                                case RESIZE_NE: {
                                    float pmx = before.min.x;
                                    float pmy = before.min.y;
                                    float bw = before.w();
                                    float bh = before.h();
                                    float tx = (fx - finger.hitOnDown[0].x);
                                    float ty = (fy - finger.hitOnDown[0].y);
                                    pos(pmx, pmy, Math.max(pmx + maxAspectRatioChange * bw, bw + pmx + tx), Math.max(pmy + maxAspectRatioChange * bh, bh + pmy + ty));
                                }
                                break;

                                case RESIZE_SW: {
                                    float pmx = before.max.x;
                                    float pmy = before.max.y;
                                    float bw = before.w();
                                    float bh = before.h();
                                    float tx = (fx - finger.hitOnDown[0].x);
                                    float ty = (fy - finger.hitOnDown[0].y);
                                    pos(pmx - bw + tx, pmy - bh + ty, pmx, pmy); //TODO limit aspect ratio change
                                }
                                break;
                                case MOVE: {
                                    float pmx = before.min.x;
                                    float pmy = before.min.y;
                                    float tx = pmx + (fx - finger.hitOnDown[0].x);
                                    float ty = pmy + (fy - finger.hitOnDown[0].y);
                                    pos(tx, ty, w() + tx, h() + ty);
                                }
                                break;
                                default:
                                    return s;
                            }
                        }
                    } else {
                        dragMode = null;
                    }

                    return this;
                } else {
                    potentialDragMode = dragMode = null;
                }
            }

            return s;
        }

        hover = false;
        potentialDragMode = dragMode = null;
        return null;
    }

    public boolean editable() {
        return true;
    }

    public boolean editable(WindowDragging d) {
        return true;
    }

    public boolean opaque() {
        return true;
    }


    protected void prepaint(GL2 gl) {

    }

    protected void postpaint(GL2 gl) {

    }

    @Override
    protected void paintComponent(GL2 gl) {
        if (hover && opaque()) {
            switch (potentialDragMode) {
                case RESIZE_N:
                case RESIZE_S:
                case RESIZE_W:
                case RESIZE_E:
                    gl.glColor3f(0.15f, 0f, 0.2f);
                    break;
                case RESIZE_NE:
                case RESIZE_SW:
                case RESIZE_NW:
                case RESIZE_SE:
                    gl.glColor3f(0.15f, 0.2f, 0f);
                    break;
                default:
                    gl.glColor3f(0.1f, 0.1f, 0.1f);
                    break;
            }
            Draw.rect(gl, x(), y(), w(), h());
        }


        prepaint(gl);

        float W = w(); //window.getWidth();
        float H = h(); //window.getHeight();

//        gl.glColor4f(0.8f, 0.6f, 0f, 0.25f);

//        int borderThick = 8;
//        gl.glLineWidth(borderThick);
//        Draw.line(gl, 0, 0, W, 0);
//        Draw.line(gl, 0, 0, 0, H);
//        Draw.line(gl, W, 0, W, H);
//        Draw.line(gl, 0, H, W, H);

        float resizeBorder = Math.max(W, H) * this.resizeBorder;

        WindowDragging p;
        if ((p = potentialDragMode) != null) {
            switch (p) {
                case RESIZE_SE:
                    //gl.glColor4f(1f, 0.8f, 0f, 0.5f);
                    //Draw.quad2d(gl, pmx, pmy, W, resizeBorder, W, 0, W - resizeBorder, 0);
                    break;
                case RESIZE_SW:
                    //gl.glColor4f(1f, 0.8f, 0f, 0.5f);
                    //Draw.quad2d(gl, pmx, pmy, 0, resizeBorder, 0, 0, resizeBorder, 0);
                    break;
            }
        }


//            gl.glLineWidth(2);
//            gl.glColor3f(0.8f, 0.5f, 0);
//            Draw.text(gl, str(notifications.top().get()), 32, smx + cw, smy + ch, 0);
//            gl.glColor3f(0.4f, 0f, 0.8f);
//            Draw.text(gl, wmx + "," + wmy, 32, smx - cw, smy - ch, 0);

        postpaint(gl);


    }


    @Override
    public boolean tangible() {
        return true;
    }

    //    @Override
//    protected void paintComponent(GL2 gl) {
//        gl.glColor3f(0.25f,0.25f,0.25f);
//        Draw.rect(gl, x(), y(), w(), h());
//    }


    public static void main(String[] args) {
        Wall d = new Wall() {
//            boolean init = true;
//            int shaderprogram;
//            String vsrc;
//
//            {
//                try {
//                    vsrc = new StringBuilder(new String(GLSL.class.getClassLoader().getResourceAsStream(
//                            "glsl/grid.glsl"
//                            //"glsl/16seg.glsl"
//                            //"glsl/metablob.glsl"
//                    ).readAllBytes())).toString();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            protected void paint(GL2 gl) {
//                if (init) {
////                    int v = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
//                    int f = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
//
//                    {
//
//                        gl.glShaderSource(f, 1, new String[]{vsrc}, new int[]{vsrc.length()}, 0);
//                        gl.glCompileShader(f);
//                    }
////                        {
////                            String vsrc = new StringBuilder(new String(GLSL.class.getClassLoader().getResourceAsStream(
////                                    "glsl/16seg.glsl"
////                            ).readAllBytes())).toString();
////                            gl.glShaderSource(f, 1, new String[]{vsrc}, new int[]{vsrc.length()}, 0);
////                            gl.glCompileShader(f);
////                        }
//
//                    shaderprogram = gl.glCreateProgram();
//                    //gl.glAttachShader(shaderprogram, v);
//                    gl.glAttachShader(shaderprogram, f);
//                    gl.glLinkProgram(shaderprogram);
//                    gl.glValidateProgram(shaderprogram);
//
//                    init = false;
//                }
//
//                gl.glUseProgram(shaderprogram);
//                super.paint(gl);
//                gl.glUseProgram(0);
//            }

        };

        d.children.add(new GridTex(16).pos(0,0,1000,1000));

        d.addWindo(new Scale(Widget.widgetDemo(), 0.9f)).pos(80, 80, 550, 450);

        d.addWindo(new Scale(grid(new PushButton("x"), new PushButton("y")), 0.9f)).pos(10, 10, 50, 50);

        d.addWindo(new Scale(new PushButton("w"), 0.9f)).pos(-50, -50, 10, 10);

        //d.newWindo(new Scale(grid(new PushButton("x"), new PushButton("y")), 0.9f)).pos(-100, -100, 0, 0);

        SpaceGraph.window(d, 800, 800);
    }

}
