package nars.gui;

import jcog.data.FloatParam;
import jcog.event.On;
import jcog.pri.mix.Mix;
import jcog.pri.mix.PSink;
import nars.$;
import nars.NAR;
import spacegraph.Surface;
import spacegraph.layout.Grid;
import spacegraph.widget.meter.Plot2D;
import spacegraph.widget.slider.FloatSlider;

import java.util.List;
import java.util.function.Consumer;

import static java.lang.Math.sqrt;

/**
 * Created by me on 4/3/17.
 */
public class MixBoard extends Grid implements Consumer<NAR> {

    private final On on;
    private final Plot2D /*priInPlot, */priOutPlot;
    private final Plot2D quaPlot;
    private final Mix mix;
    float[] next;

    public MixBoard(NAR nar, Mix<Object, ?> mix) {
        super(HORIZONTAL);

        this.mix = mix;

        List<Surface> sliders = $.newArrayList();

        //priInPlot = new Plot2D(32, Plot2D.Line);
        priOutPlot = new Plot2D(32, Plot2D.Line);
        quaPlot = new Plot2D(32, Plot2D.Line);
        String[] col = nar.in.data.col;
        int streamCols = 2;
        int i = 0;
        next = new float[streamCols * nar.in.streamID.length];
        for (PSink k : nar.in.streamID) {
            final int ii = i;
            priOutPlot.add(k + " out", () -> {
//                float N = next[1 + ii * 2];
//                if (N == 0)
//                    return 0;
//                else {
                float sum = next[1 + ii * streamCols + 0];
                return sum;

            });
            sliders.add(
                    new FloatSlider(new PowerAdapter(k)) {

                        @Override
                        public String labelText() {
                            return k.toString();
                        }
                    }
            );
            i++;
        }

        set(col(sliders), col(/*priInPlot, */priOutPlot, quaPlot));

        on = nar.onCycle(this);
    }

    @Override
    public Surface hide() {
        on.off();
        return this;
    }

    @Override
    public void accept(NAR pLinks) {

        next = mix.data.sample(next);

        //priInPlot.update();
        priOutPlot.update();

        quaPlot.update();
    }

    /**
     * the x=0..1 left-half
     *          mapped to a exponential-like curve, with x=1 corresponding to gain=1
     *     x=1..2 right-half
     *          mapped to a linear up to max gain (ex: =2)
     */

    static class PowerAdapter extends FloatParam {
        private final FloatParam target;

        public PowerAdapter(FloatParam target) {
            super(1, 0, 2);
            this.target = target;
            setValue(x(target.asFloat()));
        }

        //forward
        public static float y(float x) {
            return x*x*x*x; //x^4, exponential-like
        }

        //reverse
        public static float x(float y) {
            return (float) sqrt(sqrt(y));
        }


        @Override
        public void setValue(float x) {
            super.setValue(x);
            target.setValue(y(x));
        }
    }

//    private static class PriDifference implements DoubleSupplier {
//        private final PeriodMeter v;
//        double last;
//
//        public PriDifference(PeriodMeter v) {
//            this.v = v;
//            last = 0;
//        }
//
//        @Override
//        public double getAsDouble() {
//            double s = v.sum();
//            double last = this.last;
//            this.last = s;
//            return s-last;
//        }
//
//    }
}
