package nars.premise;

import nars.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;

import static jcog.Util.unitize;

/**
 * Created by me on 2/6/17.
 */
abstract public class DefaultPremise extends Premise {

    public DefaultPremise(@NotNull Termed c, @NotNull Task task, Term beliefTerm, Task belief, float pri, float qua) {
        super(c, task, beliefTerm, belief, pri, qua);
    }



    abstract float qualityFactor(@NotNull Truth truth, @NotNull Derivation conclude);

}
