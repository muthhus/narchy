package nars.op;

import com.gs.collections.api.tuple.primitive.ObjectIntPair;
import com.gs.collections.impl.bag.mutable.HashBag;
import nars.$;
import nars.NAR;
import nars.budget.Budget;
import nars.budget.RawBudget;
import nars.task.GeneratedTask;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

import static nars.Op.CONJ;
import static nars.nal.Tense.DTERNAL;


public class VariableCompressor implements Consumer<Task> {

    final static String tag = VariableCompressor.class.getSimpleName();

    final int introductionThreshold =
            //3; //the cost of introducing a variable
            5;

    final int minCompressedComplexity = 3;
    private final NAR nar;
    private boolean deleteOriginal = false;

    public VariableCompressor(NAR n) {
        this.nar = n;
        //n.onTask(this);
    }

    @Override
    public void accept(Task task) {

        Task result = compress(task);
        if (result!=null) {
            nar.input(result);
        }

    }

    public Task compress(Task task) {
        Compound<?> contnt = task.term();
        //if (contnt.op() == CONJ) {

            HashBag<Term> contents = new HashBag();
            contnt.recurseTerms((subterm) -> {
                if (subterm.complexity() > 1) //ignore atoms
                    contents.add(subterm);
            });
            //        contents.forEachWithOccurrences((x, o) -> {
            //            System.out.println(o + " x " + x);
            //        });
            if (contents.size() > 0) {

                final Term[] max = {null};
                final int[] score = {0};
                contents.forEachWithOccurrences((x, o) -> {

                    if (o > 1) {
                        int xc = x.complexity();
                        if (xc >= minCompressedComplexity) {
                            int sc = xc * o;
                            if (sc > introductionThreshold && sc > score[0]) {
                                max[0] = x;
                                score[0] = sc;
                            }
                        }
                    }
                });

                if (max[0] != null)
                    return compress(task, max[0]);

            }
        //}

        return null;
    }

    private Task compress(Task task, Term max) {
        Term var =
                $.varDep("c");
                //$.varIndep("c");

        Compound<?> oldContent = task.term();
        Term newContent = $.terms.remap(oldContent, max, var);
        if (newContent != null) {

            newContent = $.conj(newContent,

                    //task.dt() == 0 ? 0 : DTERNAL, //allow +0 to merge with the other part
                    DTERNAL,

                    $.sim(var, max)
            );
            //newContent = $.impl($.sim(var, max), newContent);

                newContent = Task.normalizeTaskTerm(newContent, task.punc(), nar, true);
                if (newContent!=null) {

//                    float ratio = ((float) newContent.complexity()) / oldContent.complexity();
//                    System.out.println(oldContent + "\n\t" + newContent + ": " + ratio + " compression ratio");



                    if (!task.isDeleted()) {

                        RawBudget b;
                        try {
                             b = new RawBudget(task.budget(), 1f);
                        } catch (Budget.BudgetException e) {
                            return null; //HACK
                        }

                        Task tt = new GeneratedTask(newContent, task.punc(), task.truth())
                                .time(nar.time(), task.occurrence())
                                .evidence(task.evidence())
                                .budget(b)
                                .log(tag);

                        if (deleteOriginal)
                            task.delete();

                        return tt;
                    }
                }

        }

        return null;
    }

    public Task tryCompress(Task input) {
        Task c1 = compress(input);
        if (c1!=null)
            return c1;
        return input;
    }

    public static class Precompressor {
        private final NAR nar;
        VariableCompressor c;
        ArithmeticInduction i;

        public Precompressor(NAR nar) {
            this.nar = nar;
            this.c = new VariableCompressor(nar);
            this.i = new ArithmeticInduction(nar);
        }

        public Task pre(Task input) {
            input = c.tryCompress(input);
            Set<Task> inputs = i.compress(input);
            if (inputs.isEmpty()) {
                return input;
            } else {
                Iterator<Task> ii = inputs.iterator();
                input = ii.next(); //directly input the first, queue any others
                while (ii.hasNext()) {
                    nar.inputLater(ii.next());
                }
                return input;
            }
        }

    }
}
