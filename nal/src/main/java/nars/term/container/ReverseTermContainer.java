package nars.term.container;

import nars.term.Term;
import org.jetbrains.annotations.NotNull;

/** reversed view of a TermContainer */
public class ReverseTermContainer implements TermContainer {

    private final TermContainer ref;
    private final int size;

    public ReverseTermContainer(TermContainer ref) {
        this.ref = ref;
        this.size = ref.subs();
    }

    @Override
    public int subs() {
        return size;
    }

    @Override
    public @NotNull Term sub(int i) {
        return ref.sub(size-1-i);
    }
}
