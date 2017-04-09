package nars.op;

import com.google.common.base.Joiner;
import jcog.list.CircularArrayList;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.Functor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by me on 1/24/17.
 */
public class SeqTest {


    @Test
    public void test1() throws Narsese.NarseseException {
        NAR n = new Default();
        Seq s = new Seq().on(n);
        n.log();
        n.input("seqAdd(s, a:x). :|:");
        n.input("seqAdd(s, a:y). :|:");
        n.input("(seq(s, 0), seq(s, 1)).");
        n.input("log(reflect(seq(s, 0)));");
        n.run(20);
        System.out.println(s);

    }

    public static class Seq {

        final int capacity = 1024;
        final Map<Term,CircularArrayList<Term>> seqs = new ConcurrentHashMap<>();

        float expThresh = 0.66f;

        public Seq on(NAR n) {
            n.on("seqAdd", new seqAdd());
            n.on(Functor.f2("seq", (key, index) -> {
                if (key.vars()==0 && index.vars()==0) {
                    int i = Term.intValue(index, Integer.MIN_VALUE);
                    if (i!=Integer.MIN_VALUE) {
                        CircularArrayList<Term> c = seqs.get(key);
                        if (c!=null) {
                            synchronized(c) {
                                return c.get(c.size()-1-i);
                            }
                        }
                    }
                }
                return null;
            }));
            return this;
        }

        @Override
        public String toString() {
            return Joiner.on('\n').join(seqs.entrySet());
        }

        private class seqAdd implements Operator {


            @Override
            public @Nullable Task run(@NotNull Task t, @NotNull NAR nar) {
                if (t.isBelief() && !t.isEternal() && t.term().vars()==0) {
                    float e = t.expectation();
                    if (e > expThresh) {
                        Term[] args = Operator.args(t);
                        if (args.length == 2) {
                            Term key = args[0];
                            Term belief = args[1];

                            CircularArrayList<Term> seq = seqs.computeIfAbsent(key, k -> new CircularArrayList(capacity));
                            synchronized (seq) {
                                seq.add(belief);
                            }

                            if (belief instanceof Compound) {
                                Task u = Task.clone(t, (Compound) belief);
                                if (u != null)
                                    return u.log("seqAdd(" + key + ")");
                            } else {
                                return null; //cant create a task from an atom
                            }

                        }
                    }
                }
                return t;
            }
        }

    }
}
