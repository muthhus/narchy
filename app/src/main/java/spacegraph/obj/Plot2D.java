package spacegraph.obj;

import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import com.gs.collections.api.block.procedure.primitive.FloatProcedure;
import com.gs.collections.impl.list.mutable.primitive.FloatArrayList;
import com.jogamp.opengl.GL2;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import nars.$;
import nars.util.data.list.FasterList;
import org.apache.commons.math3.util.FastMath;
import spacegraph.Surface;
import spacegraph.render.Draw;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

public class Plot2D extends Surface {
    private final FasterList<Series> series;

    //public static final ColorArray BlueRed = new ColorArray(128, Color.BLUE, Color.RED);

    //public static final ColorMatrix ca = new ColorMatrix(17, 1, (x, y) -> Color.hsb(x * 360.0, 0.6f, y * 0.5 + 0.5));

    public static abstract class Series extends FloatArrayList {

        final String name;

        /** history size */
        private final int capacity;
        private final FloatProcedure rangeFinder;

        protected transient float maxValue, minValue;

        public float[] color = new float[] { 1, 1, 1, 1 };

        @Override
        public float[] toArray() {
            return items;
        }


        @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
        public Series(String name, int capacity) {
            super(capacity);

            this.name = name;
            this.color = new float[4];
            Draw.hsb((name.hashCode()%10000) / 10000f * 360.0f, 0.7f, 0.7f, 0.9f, color);

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

        protected void push(double d) {
            if (Double.isFinite(d))
                add((float)d);
        }

        protected void push(float d) {
            if (Float.isFinite(d))
                add(d);
        }

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
                if (!Double.isFinite(v)) {
                    throw new RuntimeException("invalid value");
                }
                limit();
                if (v < min) v = min;
                if (v > max) v = max;
                push(v);
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
                push((float)valueFunc.getAsDouble());
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

                for (int i = 0; i < histSize; i++) {
                    float v = s.get(i);

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
        if (gl == null)
            return;

        if (minValue != maxValue) {

            //float m = 0; //margin

            float w = 1.0f;
            float H = 1.0f;

            //g.setGlobalBlendMode(BlendMode.DIFFERENCE);
            gl.glColor3f(0.5f,0.5f,0.5f); //gray
            //g.fillText(String.valueOf(maxValue), 0, m + g.getFont().getSize());
            Draw.line(gl, 0, 0, w, 0);
            Draw.line(gl, 0, H, w, H);

            ///gl.fillText(String.valueOf(minValue), 0, H - m - 2);
            //gl.setGlobalBlendMode(BlendMode.SRC_OVER /* default */);

            FloatToFloatFunction ypos = (v) -> {
                float py = (v - minValue) / (maxValue - minValue);
                if (py < 0) py = 0;
                else if (py > 1.0) py = 1.0f;
                return (py);
            };

            series.forEach(s -> {

                //float mid = ypos.valueOf(0.5f * (s.minValue + s.maxValue));

                gl.glColor3fv(s.color, 0);
                //gl.fillText(s.name, m, mid);

                gl.glLineWidth(2);

                FloatArrayList sh = s;
                int ss = sh.size();

                float[] ssh = sh.toArray();

                int histSize = ss;

                float dx = (w / histSize);

                float x = 0;
                float py = 0;
                for (int i = 0; i < ss; i++) { //TODO why does the array change
                    //System.out.println(x + " " + y);
                    //gl.lineTo(x, ypos.valueOf(ssh[i]));

                    float ny = ypos.valueOf(ssh[i]);
                    if (i > 0)
                        Draw.line(gl, x-dx, py, x, ny);

                    x += dx;
                    py = ny;
                }
            });
        }
    };


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

