package nars.nal;

import nars.Task;

import java.util.Collection;

/**
 * Effectively a 'compiled Premise evaluation' which contains certain invariant characteristics
 * of a derivation that it may be cached for future exact or similar premises, by using it as
 * a value in correspondence to the premise as a key in a LRU cache.
 *
 *  * TODO Premise Cache
 *  --memoizes the matching state and result procedure, effectively compiling a premise to an evaluable function
 *          --completely
 *          --partially (when > 0 termutations)
 *  --use caffeine cache with a fixed size
 *  --key = (task term, task punc, task time, beliefTerm, belief punc (or null), belief time )
 *      avoid needing to store the generating concept; it is not really important
 *      budget information is passed transiently per execution because this will fluctuate
 *      truth values and evidence can also be passed transiently because the truth function can apply it each execution
 *      same for time information
 *  --value =
 *      list of derive([transients]) -> Task functions
 *      meter of about applied vs. total termutation permutations,
 *          which can be used to evaluate the approximate completion of possibilities encountered
 *      meter of past usefulness and other cost/benefit information
 *
 *      these metics can later be used to sort the estimated values of premise batches in a queue
 *
 *      TODO not complete
 */
public class Conclusion {

    public Conclusion(Collection<Task> target) {
        this.derive = target;
    }

    public final Collection<Task> derive;

//    /** the termutators which can be evaluated to generate all possible permutations. */
//    public List<Termutator> termutators = null;
//
//    /** if no permutations, then there were no termutations involved, so it is constant and repeatable.
//     *  if some permutations, this gives a heuristic of how many varieties of derivations that can
//     *  be expected after a full traversal of the permutations (not necessarily all at once, but
//     *  ammortized over time through the controlled psuedo-random termutator shuffling behavior.
//     *
//     */
//    public int permutations = 0;

}
