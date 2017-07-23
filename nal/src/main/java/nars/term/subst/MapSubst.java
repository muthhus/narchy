package nars.term.subst;

import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public class MapSubst implements Subst {

    public final Map<Term, Term> xy;

    public MapSubst(Map<Term, Term> xy) {
        this.xy = xy;
    }

//    @Override
//    public void cache(@NotNull Term x, @NotNull Term y) {
//        //ignored
//    }

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
     *
     * @param t
     */
    @Nullable
    @Override
    public Term xy(Term t) {
        return xy.get(t);
    }

//    public void forEach(@NotNull BiConsumer<? super Term, ? super Term> each) {
//        if (xy.isEmpty()) return;
//        xy.forEach(each);
//    }
//
//    @Override
//    public boolean put(@NotNull Unify copied) {
//        throw new UnsupportedOperationException("TODO");
//    }

    @NotNull
    @Override
    public String toString() {
        return "Substitution{" +
                "subs=" + xy +
                '}';
    }


}
