//package nars.gui;
//
//import com.jogamp.opengl.GL2;
//import nars.$;
//import nars.nal.Deriver;
//import nars.truth.Truth;
//import nars.truth.TruthFunctions;
//import nars.truth.func.BeliefFunction;
//import nars.truth.func.DesireFunction;
//import nars.truth.func.TruthOperator;
//import spacegraph.Facial;
//import spacegraph.SpaceGraph;
//import spacegraph.Surface;
//import spacegraph.obj.ControlSurface;
//import spacegraph.obj.CrosshairSurface;
//import spacegraph.obj.RectWidget;
//
//import java.util.Collections;
//import java.util.List;
//
///**
// * Created by me on 7/31/16.
// */
//public class TruthFunctionExplorer {
//
//    public static class TruthFunctionView extends Surface {
//
//        TruthOperator t;
//        final List<Truth> points = $.newArrayList();
//        private float resolution;
//        private float resPoints;
//
//        public TruthFunctionView(TruthOperator t) {
//            update(t, 0.01f);
//        }
//
//        private void update(TruthOperator t, float resolution) {
//            this.resolution = resolution;
//            this.t = t;
//
//            this.resPoints = 0;
//            points.clear();
//            for (float f = 0; f <=1f; f+= resolution) {
//                for (float c = 0; c <= 1f; c += resolution) {
//                    resPoints.add(t.)
//                }
//                resPoints++;
//            }
//        }
//
//
//        @Override
//        protected void paint(GL2 gl) {
//
//        }
//    }
//
//    public static void main(String[] arg) {
//        Deriver d = Deriver.getDefaultDeriver();
//        List<TruthOperator> l = $.newArrayList();
//        Collections.addAll(l,  BeliefFunction.values());
//        Collections.addAll(l,  DesireFunction.values());
//
//
//        SpaceGraph<?> s = new SpaceGraph();
//
//        s.add(new RectWidget(
//                new TruthFunctionView(l.get(0)), 8f /* width */, 8f /* height */
//        ));
//
//        s.show(1200, 800);
//    }
//}
