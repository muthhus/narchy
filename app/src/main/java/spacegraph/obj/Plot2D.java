package spacegraph.obj;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import nars.$;
import nars.NAR;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import spacegraph.Surface;
import spacegraph.render.Draw;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

public class Plot2D extends Surface {
    private final List<Series> series;
    private String title = null;

    public void setTitle(String title) {
        this.title = title;
    }

//    public <X> Surface to(Consumer<Consumer<X>> y) {
//        y.accept((Consumer<X>)this);
//        return this;
//    }


    //public static final ColorArray BlueRed = new ColorArray(128, Color.BLUE, Color.RED);

    //public static final ColorMatrix ca = new ColorMatrix(17, 1, (x, y) -> Color.hsb(x * 360.0, 0.6f, y * 0.5 + 0.5));

    public static abstract class Series extends FloatArrayList {

        final String name;

        /** history size */
        private final int capacity;
        private final FloatProcedure rangeFinder;

        protected transient float maxValue, minValue;

        public float[] color = { 1, 1, 1, 0.75f };

        @Override
        public float[] toArray() {
            return items;
        }


        @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
        public Series(String name, int capacity) {
            super(capacity);

            this.name = name;
            this.color = new float[4];
            Draw.hsb((name.hashCode()%500) / 500f * 360.0f, 0.7f, 0.7f, 1f, color);

            this.capacity = capacity;

            this.rangeFinder = v -> {
                if (v < minValue) minValue = v;
                if (v > maxValue) maxValue = v;
                //mean += v;
            };
        }

        @Override
        public String toString() {
            return name + '[' + size() + "/" + capacity + "]";
        }

        public abstract void update();




        protected void autorange() {
            minValue = Float.POSITIVE_INFINITY;
            maxValue = Float.NEGATIVE_INFINITY;
            forEach(rangeFinder);
        }

        protected void limit() {
            int over = size() - (this.capacity-1);
            for (int i = 0; i < over; i++)
                removeAtIndex(0);
        }

    }

    private transient float minValue, maxValue;

    private final int maxHistory;


    public PlotVis plotVis;
    //private final SimpleObjectProperty<PlotVis> plotVis = new SimpleObjectProperty<>();

    public Plot2D(int history, PlotVis vis) {
        //super(w, h);

        this.series = $.newArrayList();
        this.maxHistory = history;

        this.plotVis = vis;

    }

    public Plot2D add(Series s) {
        series.add(s);
        return this;
    }

    public Plot2D add(String name, DoubleSupplier valueFunc, float min, float max) {
        Series s;
        add(s = new Series(name, maxHistory) {
            @Override public void update() {
                double v = valueFunc.getAsDouble();

                limit();
                if (v!=v) {
                    //throw new RuntimeException("invalid value");
                    add(Float.NaN);
                } else {
                    if (v < min) v = min;
                    if (v > max) v = max;
                    add((float)v);
                }

            }
        });
        s.minValue = min;
        s.maxValue = max;
        return this;
    }

    public Plot2D add(String name, DoubleSupplier valueFunc) {
        add(new Series(name, maxHistory) {
            @Override public void update() {
                limit();
                add((float)valueFunc.getAsDouble());
                autorange();
            }
        });
        return this;
    }

    @Override
    protected void paint(GL2 gl) {


        List<Series> series = this.series;

        //HACK (not initialized yet but run() called
        if (series == null || series.isEmpty()) {
            return;
        }

//        GraphicsContext g = graphics();
//
//        double W = g.getCanvas().getWidth();
//        double H = g.getCanvas().getHeight();
//
//        g.clearRect(0, 0, W, H);
//
//        PlotVis pv = plotVis.get();
//        if (pv != null) {
//            pv.draw(series, g, minValue, maxValue);
//        }

        plotVis.draw(series, gl, minValue, maxValue);

        if (title!=null) {
            gl.glEnable(GL2.GL_COLOR_LOGIC_OP);
            gl.glLogicOp(GL2.GL_XOR);

            //Draw.text(gl, 0.001f, 0.001f, title, 0.5f,0.5f,0);
            gl.glColor3f(1f, 1f, 1f);

            Draw.text(gl, title, 0.1f, 0.5f, 0.5f, 0);
            gl.glDisable(GL2.GL_COLOR_LOGIC_OP);
        }

    }

    @FunctionalInterface
    public interface PlotVis {
        void draw(List<Series> series, GL2 g, float minValue, float maxValue);
    }

    public static final PlotVis BarWave = (List<Series> series, GL2 g, float minValue, float maxValue) -> {
        if (minValue != maxValue) {

            float w = 1.0f; //g.getCanvas().getWidth();
            float h = 1.0f; //g.getCanvas().getHeight();


            for (int z = 0, seriesSize = series.size(); z < seriesSize; z++) {
                Series s = series.get(z);

                int histSize = s.size();

                float dx = (w / histSize);

                float x = 0;
                float prevX = -1;

                float[] ss = s.toArray();
                int len = Math.min(s.size(), ss.length);
                for (int i = 0; i < len; i++) {
                    float v = ss[i];

                    float py = (v - minValue) / (maxValue - minValue);
                    if (py < 0) py = 0;
                    if (py > 1.0) py = 1.0f;

                    float y = py * h;

                    g.glColor4fv(s.color, 0);

                    Draw.rect(g, prevX, h / 2.0f - y / 2f, dx, y);

                    prevX = x;
                    x += dx;
                }

            }
        }
    };

    public static final PlotVis Line = (List<Series> series, GL2 gl, float minValue, float maxValue) -> {

        float labelDZ = 0.1f;
        float rangeFontScale = 0.0005f;

        //background
        gl.glColor4f(0,0,0,0.75f);
        Draw.rect(gl, 0, 0, 1, 1);

        if (minValue != maxValue) {

            //float m = 0; //margin

            float W = 1.0f;
            float H = 1.0f;

            gl.glColor4f(0.5f,0.5f,0.5f, 0.75f); //gray

            //g.fillText(String.valueOf(maxValue), 0, m + g.getFont().getSize());
            Draw.text(gl, rangeFontScale, rangeFontScale, String.valueOf(minValue), 0, 0, labelDZ);

            Draw.line(gl, 0, 0, W, 0);
            Draw.line(gl, 0, H, W, H);

            Draw.text(gl, rangeFontScale, rangeFontScale, String.valueOf(maxValue), 0, H, labelDZ);

            for (int si = 0, seriesSize = series.size(); si < seriesSize; si++) {

                Series s = series.get(si);

                float mid = ypos(minValue ,maxValue, (s.minValue + s.maxValue)/2f);


                FloatArrayList sh = s;
                int ss = sh.size();

                float[] ssh = sh.toArray();

                int histSize = ss;

                float dx = (W / histSize);

                float x = 0;

                //float py = 0;

                gl.glLineWidth(2);
                gl.glColor3fv(s.color, 0);

                float ny = mid;
                gl.glBegin(GL.GL_LINE_STRIP);
                float range = maxValue - minValue;
                for (int i = 0; i < ss; i++) {

                    float v = ssh[i];
                    if (v==v) {
                        ny = ypos(minValue, range, v);

                        //if (i > 0) {
                        gl.glVertex3f(x, ny, 0);
                        //Draw.line(gl, x - dx, py, x, ny);
                        //}
                    }

                    x += dx;
                    //py = ny;
                }
                gl.glEnd();

                gl.glLineWidth(1);
                Draw.text(gl, s.name, 0.1f, W, ny, 0, Draw.TextAlignment.Right);

            }
        }
    };

//    private static float ypos(float minValue, float maxValue, float v) {
//        float ny = (v - minValue) / (maxValue - minValue);
//        //if (ny < 0) ny = 0;
//        //else if (ny > 1.0) ny = 1.0f;
//        return ny;
//    }
    private static float ypos(float minValue, float range, float v) {
        float ny = (v - minValue) / range;
        //if (ny < 0) ny = 0;
        //else if (ny > 1.0) ny = 1.0f;
        return ny;
    }


    public void update() {
        series.forEach(Series::update);

        minValue = Float.POSITIVE_INFINITY;
        maxValue = Float.NEGATIVE_INFINITY;
        series.forEach((Series s) -> {
            minValue = Math.min(minValue, s.minValue);
            maxValue = Math.max(maxValue, s.maxValue);
        });
    }

}

