package nars.term.atom;

import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 2/18/16.
 */
public abstract class AtomicStringConstant extends AtomicString {

    @NotNull public final String id;

    protected AtomicStringConstant(@NotNull String id) {
        this.id = id;
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
    public final int init(@NotNull int[] meta) {

        meta[4] ++; //volume
        meta[5] |= structure();

        return hashCode();
    }


}
