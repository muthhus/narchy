package nars.term.atom;

import org.jetbrains.annotations.NotNull;

/**
 * Atomic impl which relies on a String instance
 */
public abstract class AtomicString extends ToStringAtomic {

    @NotNull public final String id;

    /** (cached for speed) */
    final int hash;

    protected AtomicString(@NotNull String id) {
        this.id = id;
        this.hash = super.hashCode();
    }

    @NotNull
    @Override public final String toString() {
        return id;
    }

    @Override
    public final int varIndep() {
        return 0;
    }

    @Override
    public final int varDep() {
        return 0;
    }

    @Override
    public final int varQuery() {
        return 0;
    }

    @Override
    public final int varPattern() {
        return 0;
    }

    @Override
    public final int vars() {
        return 0;
    }


    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final void init(@NotNull int[] meta) {

        meta[4] ++; //volume
        meta[5] |= op().bit; //structure();

    }


}
