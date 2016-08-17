package nars.op;

import nars.Param;
import nars.Task;
import nars.term.var.Variable;
import org.eclipse.collections.impl.bag.mutable.HashBag;
import nars.$;
import nars.NAR;
import nars.task.GeneratedTask;
import nars.term.Compound;
import nars.term.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

import static nars.nal.Tense.DTERNAL;


public class VariableCompressor implements Consumer<Task> {

    private static final Logger logger = LoggerFactory.getLogger(VariableCompressor.class);

    final static String tag = VariableCompressor.class.getSimpleName();

    final int introductionThreshold =
            //3; //the cost of introducing a variable
            5;

    final int minCompressedComplexity = 3;
    final static int VOLUME_SAFETY_THRESH = 6;
    private final NAR nar;


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

        int n = nar.compoundVolumeMax.intValue();
        if (contnt.volume() + VOLUME_SAFETY_THRESH >= n)
            return null;

        //if (contnt.op() == CONJ) {

            HashBag<Term> contents = new HashBag();
            contnt.recurseTerms((subterm) -> {
                if (subterm.complexity() > 3) //ignore atoms, 1-product of atom, and negated 1-product of atoms, etc.
                    contents.add(subterm);
            });
            //        contents.forEachWithOccurrences((x, o) -> {
            //            System.out.println(o + " x " + x);
            //        });
            if (contents.size() > 1) {

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
        //if (newContent != null) {

            newContent =
                //$.secte( newContent,
                    //$.impl(
                $.conj(
                    $.sim(var, max),
                    task.dt() == 0 ? 0 : DTERNAL, //allow +0 to merge with the other part
                        //DTERNAL,

                    newContent
            );

        newContent = nar.normalize((Compound)newContent);

                //newContent = $.impl($.sim(var, max), newContent);

                //newContent = Task.normalizeTaskTerm(newContent, task.punc(), nar, true);
                //if (newContent!=null) {

//                    float ratio = ((float) newContent.complexity()) / oldContent.complexity();
//                    System.out.println(oldContent + "\n\t" + newContent + ": " + ratio + " compression ratio");



                    if (newContent!=null && !task.isDeleted()) {

                        Task tt = new GeneratedTask(newContent, task.punc(), task.truth())
                                .time(task.creation(), task.occurrence())
                                .evidence(task.evidence())
                                .budget(task.budget())
                                .log(tag);

                        if (!tt.isDeleted()) //applying task's budget may have deleted it by here
                            return tt;
                    }
                //}

        //}

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
            try {
                //stage 1
                input = c.tryCompress(input);


                //stage 2
                Set<Task> inputs = i.compress(input);

                if (!inputs.isEmpty()) {
                    Iterator<Task> ii = inputs.iterator();
                    input = ii.next();
                    while (ii.hasNext()) {
                        nar.inputLater(ii.next());
                    }
                }

                return input;

            } catch (Exception e) {
                if (Param.DEBUG)
                    logger.error("{}", e);
                return input;
            }


        }

    }
}
