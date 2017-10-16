//package nars.term.subst;
//
//import nars.Param;
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Map;
//
///** wrapper which parameterized by an additional mapping pair that acts as an overriding overlay prior to accessing the MapSubst internal map */
//public final class MapSubstWithOverride extends MapSubst {
//    @NotNull
//    final Term ox, oy;
//
//    public MapSubstWithOverride(@NotNull Map<Term, Term> xy, @NotNull Term ox, @NotNull Term oy) {
//        super(xy);
//        this.ox = ox;
//        this.oy = oy;
//        if (Param.DEBUG && ox.equals(oy)) throw new RuntimeException("pointless substitution");
//    }
//
//
//
//    @Override
//    public Term xy(@NotNull Term t) {
//        return t.equals(ox) ? oy : super.xy(t);
//    }
////
////        @Override
////        public void forEach(@NotNull BiConsumer<? super Term, ? super Term> each) {
////            throw new UnsupportedOperationException();
////        }
//
//    @Override
//    public boolean isEmpty() {
//        return false;
//    }
//
//    @Override
//    public void clear() {
//        throw new UnsupportedOperationException();
//    }
//
//    @NotNull
//    @Override
//    public String toString() {
//        return "Substitution{(" + ox + ',' + oy + ") && " +
//                "inherited subs=" + xy +
//                '}';
//    }
//
//}
