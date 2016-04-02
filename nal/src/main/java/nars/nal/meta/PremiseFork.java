package nars.nal.meta;

import org.jetbrains.annotations.NotNull;

/**
 * reverting fork for use during premise matching
 */
public class PremiseFork extends ThenFork {

    private final ProcTerm[] termCache;

    public PremiseFork(ProcTerm[] n) {
        super(n);
        if (n.length == 1)
            throw new RuntimeException("unnecessary use of fork");
        this.termCache = n;
    }

    @Override
    public void accept(@NotNull PremiseEval m) {
        final int stack = m.now();
        for (ProcTerm s : termCache) {
            s.accept(m);
            m.revert(stack);
        }
    }
}
