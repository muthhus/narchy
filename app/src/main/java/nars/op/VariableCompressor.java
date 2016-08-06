package nars.op;

import com.gs.collections.api.tuple.primitive.ObjectIntPair;
import com.gs.collections.impl.bag.mutable.HashBag;
import nars.$;
import nars.NAR;
import nars.task.GeneratedTask;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static nars.Op.CONJ;


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
        n.onTask(this);
    }

    @Override
    public void accept(Task task) {

        Compound<?> contnt = task.term();

        if (contnt.op() == CONJ) {

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
                    compress(task, max[0]);

            }
        }

    }

    private void compress(Task task, Term max) {
        Term var =
                //$.varDep("c");
                $.varIndep("c");

        Compound<?> oldContent = task.term();
        Term newContent = $.terms.remap(oldContent, max, var);
        if (newContent != null) {

            newContent = $.conj(newContent, $.sim(var, max));




                newContent = Task.normalizeTaskTerm(newContent, task.punc(), nar, true);
                if (newContent!=null) {

//                    float ratio = ((float) newContent.complexity()) / oldContent.complexity();
//                    System.out.println(oldContent + "\n\t" + newContent + ": " + ratio + " compression ratio");


                    if (!task.isDeleted()) {
                        @NotNull MutableTask tt = new GeneratedTask(newContent, task.punc(), task.truth()).evidence(task.evidence())
                                .budget(task.budget());

                        if (deleteOriginal)
                            task.delete();

                        nar.inputLater(tt.log(tag));
                    }
                }

        }


    }
}
