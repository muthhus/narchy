package nars.index.symbol;

import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by me on 3/13/16.
 */
public interface SymbolMap {

    Termed resolve(String id);

    default Termed resolve(@NotNull Atomic a) {
        return resolve(a.toString());
    }

    Termed resolveOrAdd(String s, Function<Term, ? extends Termed> conceptBuilder);

    default Termed resolveOrAdd(@NotNull Atomic a, Function<Term, ? extends Termed> conceptBuilder) {
        return resolveOrAdd(a.toString(), conceptBuilder);
    }

    void print(Appendable out);

    void forEach(Consumer<? super Termed> c);


}
