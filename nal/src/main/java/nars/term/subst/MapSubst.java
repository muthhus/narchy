package nars.term.subst;

import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Created by me on 12/3/15.
 */
public class MapSubst implements Subst {

    public final Map<Term, Term> xy;

//    /**
//     * creates a substitution of one variable; more efficient than supplying a Map
//     */
//    public MapSubst(Term termFrom, Term termTo) {
//        this(UnifiedMap.newWithKeysValues(termFrom, termTo));
//    }


    public MapSubst(@NotNull Term x, @NotNull Term y) {
        this(Map.of(x,y));
    }

    public MapSubst(Map<Term, Term> xy) {
        this.xy = xy;

    }

    @Override
    public void clear() {
        xy.clear();
    }

    @Override
    public boolean isEmpty() {
        return xy.isEmpty();
    }

    /**
     * gets the substitute
     * @param t
     */
    @Nullable
    @Override
    public Term term(Term t) {
        return xy.get(t);
    }

    public void forEach(@NotNull BiConsumer<? super Term, ? super Term> each) {
        if (xy.isEmpty()) return;
        xy.forEach(each);
    }


    @NotNull
    @Override
    public String toString() {
        return "Substitution{" +
                "subs=" + xy +
                '}';
    }

    /** wrapper which parameterized by an additional mapping pair that acts as an overriding overlay prior to accessing the MapSubst internal map */
    public final static class MapSubstWithOverride extends MapSubst {
        @NotNull
        final Term ox, oy;

        public MapSubstWithOverride(@NotNull Map<Term, Term> xy, @NotNull Term ox, @NotNull Term oy) {
            super(xy);
            this.ox = ox;
            this.oy = oy;
        }

        @Override
        public Term term(@NotNull Term t) {
            return t.equals(ox) ? oy : super.term(t);
        }

        @Override
        public void forEach(@NotNull BiConsumer<? super Term, ? super Term> each) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @NotNull
        @Override
        public String toString() {
            return "Substitution{(" + ox + ',' + oy + ") && " +
                    "inherited subs=" + xy +
                    '}';
        }

    }


}
