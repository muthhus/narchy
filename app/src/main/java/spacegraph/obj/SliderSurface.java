package spacegraph.obj;

import bulletphys.ui.ShapeDrawer;
import com.google.common.collect.Lists;
import com.jogamp.opengl.GL2;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import org.apache.commons.lang3.mutable.MutableFloat;

import javax.vecmath.Vector2f;
import java.util.List;

/**
 * Created by me on 6/26/16.
 */
public class SliderSurface extends Surface {

    public final MutableFloat value;
    public final MutableFloat min;
    public final MutableFloat max;


    public SliderSurface(float v, float min, float max) {
        this(new MutableFloat(v), new MutableFloat(min), new MutableFloat(max));
    }

    public SliderSurface(MutableFloat value, MutableFloat min, MutableFloat max) {
        this.value = value;
        this.min = min;
        this.max = max;
    }

//    public static void main(String[] args) {
//        new GraphSpace<Surface>(
//
//                (Surface vt) -> new SurfaceMount(null, vt),
//
//                new XYPadSurface()
//
//        ).show(800,800);
//    }
    public static void main(String[] args) {
        SpaceGraph s = new SpaceGraph<>(

                (List<Surface> vt) -> new SurfaceMount<>(null,
                    new GridSurface(vt, GridSurface.VERTICAL)),

                Lists.newArrayList(
                    //new SliderSurface(0.75f, 0, 1),
                    new GridSurface(Lists.newArrayList(
                        new XYPadSurface(),
                        new XYPadSurface()
                    ), GridSurface.HORIZONTAL),
                    new GridSurface(Lists.newArrayList(
                            new SliderSurface(0.75f,  0, 1),
                            new SliderSurface(0.25f,  0, 1),
                            new SliderSurface(0.5f,  0, 1)
                    ), GridSurface.VERTICAL)
//                    new SliderSurface(0.25f, 0, 1),

                )

        );

        s.add(new Facial(new ConsoleSurface(80, 25)).scale(500f, 400f));
        s.add(new Facial(new CrosshairSurface(s)));

        s.show(800,800);
    }


    public float value() {
        return value.floatValue();
    }

    @Override
    protected void paint(GL2 gl) {

        float p = value();
        //float margin = 0.1f;
        //float mh = margin / 2.0f;

        float W = 1;
        float H = 1;

        //gl.glLineWidth(mh * 2);
        //gl.glColor3f(0.5f, 0.5f, 0.5f);
        //ShapeDrawer.strokeRect(gl, 0, 0, W, H);

        //double hp = 0.5 + 0.5 * p;
        gl.glColor3f(0.9f, 0.2f, 0f);
        //g1.setFill(Color.ORANGE.deriveColor(70 * (p - 0.5), hp, 0.65f, 1.0f));

        float barSize = W * p;
        //ShapeDrawer.rect(gl, mh/2, mh/2f, barSize - mh, H - mh);
        ShapeDrawer.rect(gl, 0, 0, barSize, H);
    }


    @Override
    protected boolean onTouching(Vector2f hitPoint, short[] buttons) {
        if (leftButton(buttons)) {
            //System.out.println(this + " touched " + hitPoint + " " + Arrays.toString(buttons));

            value.setValue(r(decode(hitPoint)));

            return true;
        }
        return false;
    }

    private float decode(Vector2f hitPoint) {
        return hitPoint.x; //TODO interpret point coordinates according to the current drawn model, which could be a knob etc
    }


    /**
     * normalizesa a value to the specified numeric bounds
     */
    public final float p(float v) {
        float min = this.min.floatValue();
        float max = this.max.floatValue();
        return (v - min) / (max - min);
    }

    /** unnormalize */
    public final float r(float nn) {
        float mn = this.min.floatValue();
        float mx = this.max.floatValue();
        float v = (nn) * (mx - mn) + mn;
        if (v < mn) v = mn; //clip to bounds
        if (v > mx) v = mx;
        return v;
    }


//    public void denormalized(double... n) {
//        double mn = min.get();
//        double mx = max.get();
//
//        for (int i = 0; i < n.length; i++) {
//            double nn = n[i];
//            double v = (nn) * (mx - mn) + mn;
//            if (v < mn) v = mn; //clip to bounds
//            if (v > mx) v = mx;
//            value(i, v);
//        }
//
//    }
//
//    public double[] normalized() {
//        double[] n = normalized;
//        if (n == null) {
//            n = normalized = new double[dimensions];
//        }
//
//        //TODO only compute this if invalidated
//        for (int i = 0; i < dimensions; i++) {
//            n[i] = p(value[i].get());
//        }
//
//        return n;
//    }


//
//    public interface Vis {
//
//        /**
//         * @param vector vector of the normalized proportional positions, between 0 and 1.0
//         * @param canvas target canvas
//         * @param w      parent width
//         * @param h      parent height
//         * @param g      re-usable graphics context
//         */
//        void redraw(double[] vector, Canvas canvas, double w, double h, GraphicsContext g) throws RuntimeException;
//
//        static double getFirstAndOnlyDimension(double[] vector) {
//            if (vector.length != 1) throw new RuntimeException("invalid dimension");
//            return vector[0];
//        }
//    }
//
//
//    public static final Vis BarSlider = (vector, canvas1, W, H, g1) -> {
//
//
//        double p = Vis.getFirstAndOnlyDimension(vector);
//
//        double margin = 4;
//        double mh = margin / 2.0;
//
//
//        g1.setLineWidth(mh * 2);
//        g1.setStroke(Color.GRAY);
//        g1.strokeRect(0, 0, W, H);
//
//        g1.setLineWidth(0);
//        double hp = 0.5 + 0.5 * p;
//        g1.setFill(Color.ORANGE.deriveColor(70 * (p - 0.5), hp, 0.65f, 1.0f));
//        double barSize = W * p;
//        g1.fillRect(mh, mh, barSize - mh * 2, H - mh * 2);
//    };
//
//    public static final Vis NotchSlider = (v, canvas, W, H, g) -> {
//
//        double p = Vis.getFirstAndOnlyDimension(v);
//
//        double margin = 4;
//
//
//        g.setLineWidth(0);
//
//        //TODO use a x,w calculation that keeps the notch within bounds that none if it goes invisible at the extremes
//
//        double hp = 0.5 + 0.5 * p;
//        g.setFill(Color.ORANGE.deriveColor(70 * (p - 0.5), hp, 0.65f, 1.0f));
//        double notchRadius = W * 0.1;
//        double mh = margin / 2.0;
//        double barSize = W * p;
//        g.fillRect(mh + barSize - notchRadius, mh, notchRadius * 2, H - mh * 2);
//    };
//
//    public static final Vis CircleKnob = (v, canvas, W, H, g) -> {
//        double p = Vis.getFirstAndOnlyDimension(v);
//
//        g.clearRect(0, 0, W, H);
//        //g.setFill(Color.BLACK);
//        //g.fillRect(0, 0, W, H);
//
//        double angleStart = 0;
//        double circumferenceActive = 0.5; //how much of the circumference of the interior circle is active as a dial track
//
//
////        double x = W/2 + (W/2-margin) * Math.cos(theta);
////        double y = H/2 + (H/2-margin) * Math.sin(theta);
////        double t = 4;
//        /*g.setFill(Color.WHITE);
//        g.fillOval(x-t, y-t, t*2,t*2);*/
//
//        double ew = W;
//        double eh = H;
//
//        double np = 0.75 + (p * 0.25);
//
//        //double ews = ew * np, ehs = eh * np; //scale by prop
//
//        g.setFill(Color.DARKGRAY);
//        double ut = (H - eh) / 2.0;
//        double ul = (W - ew) / 2.0;
//        g.fillOval(ul, ut, ew, eh);
//
//        double hp = 0.5 + 0.5 * p;
//        g.setFill(Color.ORANGE.deriveColor(70 * (p - 0.5), hp, 0.65f, 1.0f));
//
//
//        double theta = angleStart + (1 - p) * circumferenceActive * (2 * Math.PI);
//        double atheta = theta * 180.0 / Math.PI; //radian to degree
//        double knobArc = 60;
//        g.fillArc(ul, ut, ew, eh,
//                atheta - knobArc / 2, knobArc, ArcType.ROUND);
//
//
//    };
//
//


//    public NSlider bind(DoubleProperty... p) {
//        ensureDimension(p.length);
//
//        for (int i = 0; i < p.length; i++)
//            p[i].bindBidirectional(value[i]);
//
//        return this;
//    }


//    public static void makeDraggable(final Stage stage, final Node byNode) {
//        final Delta dragDelta = new Delta();
//        byNode.setOnMouseEntered(new EventHandler<MouseEvent>() {
//            @Override public void handle(MouseEvent mouseEvent) {
//                if (!mouseEvent.isPrimaryButtonDown()) {
//                    byNode.setCursor(Cursor.HAND);
//                }
//            }
//        });
//        byNode.setOnMouseExited(new EventHandler<MouseEvent>() {
//            @Override public void handle(MouseEvent mouseEvent) {
//                if (!mouseEvent.isPrimaryButtonDown()) {
//                    byNode.setCursor(Cursor.DEFAULT);
//                }
//            }
//        });
//    }


//    public static void main(String[] args) {
//        FX.run((a, b) -> {
//
//            FlowPane p = new FlowPane(16, 16);
//
//            p.getChildren().setAll(
//                    new NSlider("Bar", 256, 96, NSlider.BarSlider, 0.5),
//                    new NSlider("Notch", 128, 45, NSlider.NotchSlider, 0.25),
//                    new NSlider("Notch--", 64, 25, NSlider.NotchSlider, 0.75),
//                    new NSlider("Knob", 256, 256, NSlider.CircleKnob, 0.5),
//                    new NSlider("Ranged", 256, 256, NSlider.BarSlider, 75)
//                            .range(0, 100).on(0, c -> System.out.println(Arrays.toString(c.normalized())))
//            );
//
//
//            b.setScene(new Scene(p, 800, 800));
//            b.show();
//        });
//    }
//
//    private NSlider range(double min, double max) {
//        this.min.set(min);
//        this.max.set(max);
//        return this;
//    }
//    private NSlider on(int dimension, Consumer<NSlider> callback) {
//
//        //TODO save listener so it can be de-registered
//        value[0].addListener(c -> callback.accept(NSlider.this));
//
//        return this;
//    }
//
//    public static class LeftRightDrag implements Control, EventHandler<MouseEvent> {
//
//        private Canvas canvas;
//        private NSlider n;
//
//        @Override
//        public void start(NSlider n) {
//            canvas = n.canvas;
//            this.n = n;
//
//            canvas.setOnMouseDragged(this);
//            canvas.setOnMousePressed(this); //could also work as released
//            canvas.setOnMouseReleased(this);
//
//            canvas.setCursor(Cursor.CROSSHAIR);
//
//        }
//
//        @Override
//        public void stop() {
//            throw new RuntimeException("unimpl");
//        }
//
//        @Override
//        public void handle(MouseEvent e) {
//
//            canvas.setCursor(
//                    (e.getEventType()==MouseEvent.MOUSE_RELEASED) ? Cursor.CROSSHAIR : Cursor.MOVE
//            );
//
//            n.denormalized(e.getX() / canvas.getWidth());
//
//            //System.out.println(dx + " " + dy + " " + value.get());
//
//            e.consume();
//        }
//
//        double p(double dx) {
//            return dx / canvas.getWidth();
//        }
//
//    }
//
}
