package nars.premise;

import nars.Op;
import nars.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.truth.TruthFunctions.w2c;

/** prioritizes derivations exhibiting confidence increase, relative to the premise's evidence */
public class PreferSimpleAndConfidentPremise extends DefaultPremise {

    public PreferSimpleAndConfidentPremise(Termed c, @NotNull Task task, Term beliefTerm, Task belief, float pri, float qua) {
        super(c, task, beliefTerm, belief, pri, qua);
    }

    @Override protected float qualityFactor(@Nullable Truth truth, @NotNull Derivation conclude) {
        if (truth == null) {
            //question or quest:
            return //1;
                   conclude.nar.qualityDefault(Op.QUESTION);
        } else {
            float pe = conclude.premiseEvidence;
            if (pe == 0)
                return 0; //??
            return truth.conf() / w2c(pe);
        }
    }

}
