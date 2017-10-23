//package nars.op;
//
//import jcog.spatial.Point2D;
//import jcog.spatial.Rect2D;
//import jcog.spatial.SpatialSearch;
//import nars.$;
//import nars.NAR;
//import nars.nar.Default;
//import nars.term.Term;
//import nars.term.atom.Atom;
//import nars.term.obj.IntTerm;
//import org.junit.jupiter.api.Test;
//
//import static nars.Op.COMMAND;
//import static nars.Op.GOAL;
//import static nars.op.Operator.args;
//
///**
// * Created by me on 12/21/16.
// */
//public class SpatialTest {
//
//    public static class LRect<X> extends Rect2D {
//
//        public final X id;
//
//        public LRect(X z, float x, float y) {
//            this(z, new Point2D(x, y));
//        }
//        public LRect(X x, Point2D p) {
//            super(p);
//            this.id = x;
//        }
//
//        @Override
//        public String toString() {
//            return id + "@" + center();
//        }
//    }
//
//    @Test
//    public void testSpatialQuery() {
//        /*
//        //ex: http://stackoverflow.com/questions/10588865/allegrograph-geospatial-prolog-queries#10744331
//        (select (?x ?y ?dist)
//            (q- ?x !exns:geolocation ?locx)
//            (q- ?y !exns:geolocation ?locy)
//            (geo-distance ?locx ?locy ?dist))
//         */
//
//        SpatialSearch<Rect2D> r = SpatialSearch.rTree(new Rect2D.Builder() );
//        NAR n = new Default();
//
//        n.on("at", (t, nar) -> {
//
//            if (t.punc() == COMMAND || (t.punc()==GOAL && t.expectation() > 0.75f)) {
//                Term[] a = args(t);
//                Term id = a[0];
//                if (id instanceof Atom) {
//                    Term x = a[1];
//                    Term y = a[1];
//                    if (x instanceof IntTerm && y instanceof IntTerm) {
//                        int ix = ((IntTerm)x).val();
//                        int iy = ((IntTerm)y).val();
//                        r.add(new LRect<Atom>((Atom)id, ix, iy));
//                        nar.believe( $.func("at", id, $.p(ix, iy), $.the("date()")) );
//                    }
//                }
//            }
//
//            return t;
//        });
//
////        n.on(Functor.LambdaFunctor.f("at", 2, (a) -> {
////            return null;
////        }));
//
//        n.log();
//        n.input("at(a, 0, 0)!",
//                "at(b, 2, 2)!",
//                "at(c, 4, 4)!");
//
//        r.forEach(x -> {
//            System.out.println(x);
//        });
//
//
//    }
//}
