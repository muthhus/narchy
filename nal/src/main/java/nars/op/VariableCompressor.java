package nars.op;

import nars.$;
import nars.NAR;
import nars.Task;
import nars.task.GeneratedTask;
import nars.term.Compound;
import nars.term.Term;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import static nars.term.Terms.compoundOrNull;


/**
 * generalized variable introduction implemented as input preprocessor stage
 */
public class VariableCompressor implements Function<Task, Task> {

    private static final Logger logger = LoggerFactory.getLogger(VariableCompressor.class);

    final static String tag = VariableCompressor.class.getSimpleName();

    final int introductionThreshold =
            //3; //the cost of introducing a variable
            //5;
            0;

    private final NAR nar;


    public VariableCompressor(NAR n) {
        this.nar = n;
    }


    void introduceVariables(Task task) {
        Compound c = task.term();

        //size limitations
        if ((c.size()) < 2 || (c.volume() + introductionThreshold > nar.compoundVolumeMax.intValue()))
            return;

        Compound a = c, b;

        int i = 0;
        do {
            b = c;
            c = introduceNextVariable(c, i++);
        } while (b!=c);

        if (a!=c) {
            //introduction changed something
            Task newTask = inputCloned(task, c);
//            System.out.println(a + " ====> " + c);
//            System.out.println("\t" + task + " ====> " + newTask);
//            System.out.println();
        }


    }

    private Compound introduceNextVariable(Compound c, int iteration) {


        Term target = substMaximal(c, this::canIntroduce, 2, 3);
        if (target != null) {
            Term var = //$.varIndep("c"); //use indep if the introduction spans BOTH subj and predicate of any statement (even recursive)
                    $.varDep("c" + iteration); //otherwise use dep

            Term newContent = $.terms.replace(c, target, var);
            if ((newContent instanceof Compound) && !newContent.equals(c))
                return (Compound) newContent; //success
        }

        return c;
    }

    /** returns the most optimal subterm that can be replaced with a variable, or null if one does not meet the criteria */
    @Nullable
    public static Term substMaximal(Compound c, Predicate<Term> include, int minCount, int minScore) {
        HashBag<Term> uniques = subtermRepeats(c, include);
        final int[] best = {0};
        final Term[] which = new Term[1];

        uniques.forEachWithOccurrences((subterm, count) -> {
            if (count >= minCount) {
                int xc = subterm.complexity();
                int score = xc * count;
                if (score >= minScore) {
                    int currentBest = best[0];
                    if (score > currentBest) {
                        best[0] = score;
                        which[0] = subterm;
                    }
                }
            }
        });

        return which[0];
    }


    @NotNull public static ObjectIntMap<Term> substAnalysis(Compound c, Predicate<Term> include, int minCount) {

        HashBag<Term> uniques = subtermRepeats(c, include);

        if (!uniques.isEmpty()) {

            ObjectIntHashMap<Term> h = new ObjectIntHashMap<>(uniques.size());
            uniques.forEachWithOccurrences((subterm, count) -> {
                if (count >= minCount) {
                    int xc = subterm.complexity();
                    int score = xc * count;
                    h.put(subterm, score);
                }
            });

            if (!h.isEmpty()) {
                h.compact();
                return h;
            }

        }

        return ObjectIntMaps.immutable.empty();
    }

    private static HashBag<Term> subtermRepeats(Compound c, Predicate<Term> include) {
        HashBag<Term> uniques = new HashBag<>(c.volume());

        c.recurseTerms((subterm) -> {
            if (include.test(subterm)) //ignore atoms, 1-product of atom, and negated 1-product of atoms, etc.
                uniques.add(subterm);
        });

        return uniques;
    }



    private boolean canIntroduce(Term subterm) {
        return !subterm.op().var;
    }

    @Nullable protected Task inputCloned(@NotNull Task task, @NotNull Term newContent) {

        Compound c = nar.normalize((Compound) newContent);
        if (c != null) {

            Task tt = new GeneratedTask(c, task.punc(), task.truth())
                    .time(task.creation(), task.occurrence())
                    .evidence(task.evidence())
                    .budget(task.budget())
                    .log(tag);

            nar.inputLater(tt);
            return tt;
        }

        return null;
    }


    @Override
    public Task apply(Task input) {
        try {
            introduceVariables(input);
        } catch (Exception e) {
            //if (Param.DEBUG)
            logger.error("{}", e.toString());
        }

        //pass-through original input
        return input;

    }

}
