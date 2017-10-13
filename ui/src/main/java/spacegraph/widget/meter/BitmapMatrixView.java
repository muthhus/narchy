package spacegraph.widget.meter;

import jcog.math.FloatSupplier;
import jcog.tensor.ArrayTensor;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import spacegraph.video.TextureSurface;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.util.function.Supplier;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;


/**
 * Created by me on 7/29/16.
 */
public class BitmapMatrixView extends TextureSurface {


    private final int w;
    private final int h;
    private final ViewFunction2D view;
    private BufferedImage buf;
    private int[] rasInt;
    private WritableRaster raster;

//
//    public static ViewFunction2D arrayRenderer(float[][] ww) {
//        return (x, y) -> {
//            float v = ww[x][y];
//            Draw.colorBipolar(gl, v);
//            return 0;
//        };
//    }

//    public static ViewFunction2D arrayRenderer(float[] w) {
//        return (x, y) -> {
//            float v = w[y];
//            Draw.colorBipolar(gl, v);
//            return 0;
//        };
//    }
//
//    public static ViewFunction2D arrayRenderer(double[] w) {
//        return (x, y) -> {
//            float v = (float) w[y];
//            Draw.colorBipolar(gl, v);
//            return 0;
//        };
//    }

    public interface ViewFunction1D {
        /**
         * updates the GL state for each visited matrix cell (ex: gl.glColor...)
         * before a rectangle is drawn at the returned z-offset
         */
        int update(float x);
    }

    @FunctionalInterface
    public interface ViewFunction2D {
        /**
         * updates the GL state for each visited matrix cell (ex: gl.glColor...)
         * before a rectangle is drawn at the returned z-offset
         */
        int update(int x, int y);
    }

    protected BitmapMatrixView(int w, int h) {
        this(w, h, null);
    }


    public BitmapMatrixView(float[] d, ViewFunction1D view) {
        this(d, 1, view);
    }


//    public BitmapMatrixView(float[][] w) {
//        this(w.length, w[0].length, BitmapMatrixView.arrayRenderer(w));
//    }

    public BitmapMatrixView(int w, int h, ViewFunction2D view) {
        this.w = w;
        this.h = h;
        this.view = view != null ? view : ((ViewFunction2D) this);
    }

//    public static final ViewFunction1D bipolar1 = (x, gl) -> {
//        Draw.colorBipolar(gl, x);
//        return 0;
//    };
//    static final ViewFunction1D unipolar1 = (x, gl) -> {
//        Draw.colorGrays(gl, x);
//        return 0;
//    };
//
//    public BitmapMatrixView(float[] d, boolean bipolar) {
//        this(d, 1, bipolar ? bipolar1 : unipolar1);
//    }

    public BitmapMatrixView(float[] d, int stride, ViewFunction1D view) {
        this((int) Math.floor(((float) d.length) / stride), stride, (x, y) -> {
            int i = y * stride + x;
            if (i < d.length)
                return view.update(d[i]);
            else
                return 0;
        });
    }

    public BitmapMatrixView(IntToFloatFunction d, int len, int stride, ViewFunction1D view) {
        this((int) Math.floor(((float) len) / stride), stride, (x, y) -> {
            int i = y * stride + x;
            if (i < len)
                return view.update(d.valueOf(i));
            else
                return 0;
        });
    }

    public BitmapMatrixView(double[] d, int stride, ViewFunction1D view) {
        this((int) Math.floor(((float) d.length) / stride), stride, (x, y) -> {
            int i = y * stride + x;
            if (i < d.length)
                return view.update((float) d[i]);
            else
                return 0;
        });
    }

    public <P extends FloatSupplier> BitmapMatrixView(P[] d, int stride, ViewFunction1D view) {
        this((int) Math.floor(((float) d.length) / stride), stride, (x, y) -> {
            int i = y * stride + x;
            if (i < d.length)
                return view.update(d[i].asFloat());
            else
                return 0;
        });
    }
//    public BitmapMatrixView(Tensor t, int stride, ViewFunction1D view) {
//        this((int) Math.floor(((float) t.volume()) / stride), stride, (x, y) ->
//            view.update(t.get(x * stride + y), gl)
//        );
//    }

    public static BitmapMatrixView get(ArrayTensor t, int stride, ViewFunction1D view) {
        float[] d = t.data;
        return new BitmapMatrixView((int) Math.floor(((float) t.volume()) / stride), stride, (x, y) -> {
            float v = d[x * stride + y];
            return view.update(v);
        });
    }

    public BitmapMatrixView(Supplier<double[]> e, int length, int stride, ViewFunction1D view) {
        this((int) Math.floor(((float) length) / stride), stride, (x, y) -> {
            double[] d = e.get();
            if (d != null) {

                int i = y * stride + x;
                if (i < d.length)
                    return view.update((float) d[i]);
            }
            return 0;
        });
    }

    /** must call this to re-generate texture so it will display */
    public void update() {
        if (buf == null) {
            buf = new BufferedImage(w, h, TYPE_INT_ARGB);
            raster = buf.getRaster();
            this.rasInt = ((DataBufferInt)raster.getDataBuffer()).getData();
        }

        int i = 0;
        int[] rr = this.rasInt;
        final int h = this.h;
        final int w = this.w;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                rr[i++] = view.update(x, y);
            }
        }

        update(buf);
    }


    //    @Override
//    protected void paintComponent(GL2 gl) {
//
//        float h = this.h;
//        float w = this.w;
//
//        if ((w == 0) || (h == 0))
//            return;
//
//
//        float dw = 1f / w;
//        float dh = 1f / h;
//
//
//        for (int y = 0; y < h; y++) {
//            for (int x = 0; x < w; x++) {
//
//                //try {
//                float dz = view.update(x, y);
//                if (dz == dz)
//                    Draw.rect(gl, x * dw, 1f - (y + 1) * dh, dw, dh, dz);
//                /*} catch (Exception e) {
//                    logger.error(" {}",e);
//                    return;
//                }*/
//
//            }
//        }
//
////            //border
////            gl.glColor4f(1f, 1f, 1f, 1f);
////            Draw.strokeRect(gl, 0, 0, tw + dw, th + dh);
//
//    }


}
