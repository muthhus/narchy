package nars.index.term.tree;

import jcog.data.byt.ByteSeq;
import jcog.radixtree.MyConcurrentRadixTree;
import nars.$;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * String interner that maps strings to integers and resolves them
 * bidirectionally with a globally shared Atomic concept
 */
public class TermTree extends MyConcurrentRadixTree<Termed> {

    public final Termed computeIfAbsent(@NotNull ByteSeq s, @NotNull Function<Term, ? extends Termed> conceptBuilder) {
        return putIfAbsent(s, () -> conceptBuilder.apply($.the(s.toString())));
    }

    @Override
    public Termed put(@NotNull Termed value) {
        return put(key(value), value);
    }

    @NotNull
    public static TermKey key(@NotNull Termed value) {
        return TermKey.term(value.term());
    }

    public Termed get(TermKey term) {
        return getValueForExactKey(term);
    }

}
