//package nars.gui;
//
//import jcog.data.FloatParam;
//import jcog.event.On;
//import jcog.meter.TelemetryRing;
//import jcog.pri.mix.Mix;
//import jcog.pri.mix.PSink;
//import nars.$;
//import nars.NAR;
//import org.jetbrains.annotations.NotNull;
//import spacegraph.Surface;
//import spacegraph.layout.Grid;
//import spacegraph.widget.meter.Plot2D;
//import spacegraph.widget.slider.FloatSlider;
//
//import java.util.List;
//import java.util.function.Consumer;
//
//import static java.lang.Math.sqrt;
//
///**
// * Created by me on 4/3/17.
// */
//public class MixBoard extends Grid implements Consumer<NAR> {
//
//    private final On on;
//    private final Plot2D /*priInPlot, */plot;
//
//    private final Mix mix;
//    float[] next;
//
//    public MixBoard(@NotNull NAR nar, @NotNull Mix mix) {
//        super(HORIZONTAL);
//
//        this.mix = mix;
//
//        List<Surface> sliders = $.newArrayList();
//
//        plot = new Plot2D(32, Plot2D.Line);
//
//        int streamCols = 2;
//        int i = 0;
//        next = new float[streamCols * mix.streamID.length];
//        for (PSink k : mix.streamID) {
//            final int ii = i;
//            plot.add(k.toString(), () -> {
////                float N = next[1 + ii * 2];
////                if (N == 0)
////                    return 0;
////                else {
//                float sum = next[1 + ii * streamCols + 0];
//                return sum;
//
//            });
//            sliders.add(
//                    new FloatSlider(new PowerAdapter(k)) {
//
//                        @Override
//                        public String labelText() {
//                            return k.toString();
//                        }
//                    }
//            );
//            i++;
//        }
//
//        set(col(sliders), col(/*priInPlot, */plot));
//
//        on = nar.onCycle(this);
//    }
//
//    @Override
//    public Surface hide() {
//        on.off();
//        return this;
//    }
//
//    @Override
//    public void accept(NAR pLinks) {
//
//        TelemetryRing m = mix.data;
//        if (m!=null) {
//            next = m.sample(next);
//            plot.update();
//        }
//    }
//
//    /**
//     * the x=0..1 left-half
//     *          mapped to a exponential-like curve, with x=1 corresponding to gain=1
//     *     x=1..2 right-half
//     *          mapped to a linear up to max gain (ex: =2)
//     */
//
//    static class PowerAdapter extends FloatParam {
//        private final FloatParam target;
//
//        public PowerAdapter(FloatParam target) {
//            super(1, 0, 2);
//            this.target = target;
//            setValue(x(target.asFloat()));
//        }
//
//        //forward
//        public static float y(float x) {
//            return x*x*x*x; //x^4, exponential-like
//        }
//
//        //reverse
//        public static float x(float y) {
//            return (float) sqrt(sqrt(y));
//        }
//
//
//        @Override
//        public void setValue(float x) {
//            super.setValue(x);
//            target.setValue(y(x));
//        }
//    }
//
////    private static class PriDifference implements DoubleSupplier {
////        private final PeriodMeter v;
////        double last;
////
////        public PriDifference(PeriodMeter v) {
////            this.v = v;
////            last = 0;
////        }
////
////        @Override
////        public double getAsDouble() {
////            double s = v.sum();
////            double last = this.last;
////            this.last = s;
////            return s-last;
////        }
////
////    }
//}
