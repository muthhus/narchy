package nars.util.graph;

import nars.Task;
import nars.control.Premise;
import nars.derive.Deriver;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * reverse derivation: generates premises which can result in a given task,
 * for system analysis and metaprogramming
 */
public interface Underiver {

    void underive(Task conclusion, Consumer<Premise> eachPossiblePremise);

    /**
     * a forward deriver implementation which it will use to check its results
     */
    @NotNull Deriver deriver();

}
