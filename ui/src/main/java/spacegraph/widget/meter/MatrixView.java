package spacegraph.widget.meter;

import com.jogamp.opengl.GL2;
import jcog.math.FloatSupplier;
import jcog.pri.Priority;
import spacegraph.render.Draw;
import spacegraph.widget.Widget;

import java.util.function.Supplier;

/**
 * Created by me on 7/29/16.
 */
public class MatrixView extends Widget {


    private final int w;
    private final int h;
    private final ViewFunction2D view;


    public static ViewFunction2D arrayRenderer(float[][] ww) {
        return (x, y, gl) -> {
            float v = ww[x][y];
            Draw.colorPolarized(gl, v);
            return 0;
        };
    }

    public static ViewFunction2D arrayRenderer(float[] w) {
        return (x, y, gl) -> {
            float v = w[y];
            Draw.colorPolarized(gl, v);
            return 0;
        };
    }
  public static ViewFunction2D arrayRenderer(double[] w) {
        return (x, y, gl) -> {
            float v = (float) w[y];
            Draw.colorPolarized(gl, v);
            return 0;
        };
    }

    public interface ViewFunction1D {
        /**
         * updates the GL state for each visited matrix cell (ex: gl.glColor...)
         * before a rectangle is drawn at the returned z-offset
         */
        float update(float x, GL2 gl);
    }

    @FunctionalInterface
    public interface ViewFunction2D {
        /**
         * updates the GL state for each visited matrix cell (ex: gl.glColor...)
         * before a rectangle is drawn at the returned z-offset
         */
        float update(int x, int y, GL2 gl);
    }

    protected MatrixView(int w, int h) {
        this(w, h, null);
    }


    public MatrixView(float[] d, ViewFunction1D view) {
        this(d, 1, view);
    }


    public MatrixView(float[][] w) {
        this(w.length, w[0].length, MatrixView.arrayRenderer(w));
    }
    public MatrixView(int w, int h, ViewFunction2D view) {
        this.w = w;
        this.h = h;
        this.view = view != null ? view : ((ViewFunction2D) this);
    }

    public MatrixView(float[] d, int stride, ViewFunction1D view) {
        this((int) Math.floor(((float) d.length) / stride), stride, (x, y, gl) -> {
            int i = y * stride + x;
            if (i < d.length)
                return view.update(d[i], gl);
            else
                return Float.NaN;
        });

    }

    public MatrixView(double[] d, int stride, ViewFunction1D view) {
        this((int) Math.floor(((float) d.length) / stride), stride, (x, y, gl) -> {
            int i = y * stride + x;
            if (i < d.length)
                return view.update((float) d[i], gl);
            else
                return Float.NaN;
        });
    }
    public <P extends FloatSupplier> MatrixView(P[] d, int stride, ViewFunction1D view) {
        this((int) Math.floor(((float) d.length) / stride), stride, (x, y, gl) -> {
            int i = y * stride + x;
            if (i < d.length)
                return view.update(d[i].asFloat(), gl);
            else
                return Float.NaN;
        });
    }
    public MatrixView(Supplier<double[]> e, int length, int stride, ViewFunction1D view) {
        this((int) Math.floor(((float) length) / stride), stride, (x, y, gl) -> {
            double[] d = e.get();
            if (d != null) {

                int i = y * stride + x;
                if (i < d.length)
                    return view.update((float) d[i], gl);
            }
            return Float.NaN;
        });
    }

    @Override
    protected void paintComponent(GL2 gl) {

        float h = this.h;
        float w = this.w;

        if ((w == 0) || (h == 0))
            return;


        float dw = 1f / w;
        float dh = 1f / h;


        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                //try {
                float dz = view.update(x, y, gl);
                if (dz == dz)
                    Draw.rect(gl, x * dw, 1f - (y + 1) * dh, dw, dh, dz);
                /*} catch (Exception e) {
                    logger.error(" {}",e);
                    return;
                }*/

            }
        }

//            //border
//            gl.glColor4f(1f, 1f, 1f, 1f);
//            Draw.strokeRect(gl, 0, 0, tw + dw, th + dh);

    }


}
