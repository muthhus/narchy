package nars.index.term.tree;

import nars.$;
import nars.term.Term;
import nars.term.Termed;
import nars.util.ByteSeq;
import nars.util.radixtree.MyConcurrentRadixTree;

import java.util.function.Function;

/**
 * String interner that maps strings to integers and resolves them
 * bidirectionally with a globally shared Atomic concept
 */
public class TermTree extends MyConcurrentRadixTree<Termed> {


    public final Termed get(String id) {
        return getValueForExactKey(key($.$(id)));
    }


    public final Termed computeIfAbsent(ByteSeq s, Function<Term, ? extends Termed> conceptBuilder) {
        return putIfAbsent(s, () -> conceptBuilder.apply($.the(s.toString())));
    }

    @Override
    public Termed put(Termed value) {
        return put(key(value), value);
    }

    public static TermKey key(Termed value) {
        return TermKey.term(value.term());
    }

    public Termed get(TermKey term) {
        return getValueForExactKey(term);
    }

}
