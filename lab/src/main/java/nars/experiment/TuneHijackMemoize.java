//package nars.experiment;
//
//import jcog.Optimize;
//import nars.$;
//import nars.Param;
//import nars.nar.Default;
//import nars.term.util.CachedTermIndex;
//
//public class TuneHijackMemoize {
//
//    public int reprobes = 3;
//    public int capacity = 1 * 1024;
//
//    private float boost, cut;
//
//    @Override
//    public String toString() {
//        return "TuneHijackMemoize{" +
//                "reprobes=" + reprobes +
//                ", capacity=" + capacity +
//                '}';
//    }
//
//    public static void main(String[] arg) {
//            Param.ANSWER_REPORTING = false;
//
//        final Object x = new Object();
//
//        Optimize.Result best = new Optimize<TuneHijackMemoize>(() -> {
//            return new TuneHijackMemoize();
//        }).tweak("boost", 0.001f, 0.02f, 0.001f, (c, t) -> {
//            t.boost = c;
//        }).tweak("cut", 0.001f, 0.02f, 0.001f, (c, t) -> {
//            t.cut = c;
//        })/*.tweak("reprobes", 1, 8, 1, (r, t) -> {
//            t.reprobes = Math.round(r);
//        })*/.run(2000, 1, (e) -> {
//            //synchronized (x) {
//            $.terms.terms = CachedTermIndex.termMemoize(e.capacity, e.reprobes, e.boost, e.cut);
//
//            Line1D.Line1DExperiment ee = new Line1D.Line1DExperiment();
//            ee.floatValueOf(new Default());
//
//            long misses = $.terms.terms.miss.get();
//            long rejects = $.terms.terms.reject.get();
//            long hits = $.terms.terms.hit.get();
//
//            double score = ((double) hits) / (hits + misses + rejects);
//            //System.out.println(e + " " + score);
//            return (float) score;
//            //}
//        });
//
//        best.print();
//
//    }
//}