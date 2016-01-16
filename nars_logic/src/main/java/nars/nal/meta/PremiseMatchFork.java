package nars.nal.meta;

import org.jetbrains.annotations.NotNull;

/**
 * reverting fork for use during premise matching
 */
public class PremiseMatchFork extends ThenFork<PremiseMatch> {

    public PremiseMatchFork(ProcTerm<PremiseMatch>[] n) {
        super(n);
    }

    @Override
    public void accept(@NotNull PremiseMatch m) {
        int revertTime = m.now();
        for (ProcTerm<PremiseMatch> s : terms()) {
            s.accept(m);
            m.revert(revertTime);
        }
    }
}
