package nars.guifx.demo.graph2;

import com.gs.collections.api.block.function.primitive.IntToObjectFunction;
import com.gs.collections.api.tuple.Twin;
import com.gs.collections.impl.tuple.Tuples;
import nars.$;
import nars.NAR;
import nars.bag.Bag;
import nars.budget.Budget;
import nars.budget.UnitBudget;
import nars.concept.AbstractConcept;
import nars.concept.AtomConcept;
import nars.concept.Concept;
import nars.concept.DefaultConceptBuilder;
import nars.guifx.demo.AbstractNARGraphDemo;
import nars.nar.Default;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

/**
 * Created by me on 2/23/16.
 */
public class DemoAttentionFlow extends AbstractNARGraphDemo {

    static Twin<Termed> addTermLinkChain(NAR nar, int n, IntToObjectFunction<Termed> each, boolean forward, boolean reverse) {
        Termed first = null, last = null;

        Concept prev = null;

        for (int i = 0; i < n; i++) {
            Termed x = each.valueOf(i);
            if (i == 0) first = x;
            else if (i == n-1) last = x;
            Budget initialConceptBudget = UnitBudget.Mid;
            Concept c = nar.conceptualize(x, initialConceptBudget);

            /* linking function, TODO extract as a lambda for alternate procedures */
            if (prev!= null) {

                if (reverse) {
                    //the reverse
                    List<Termed> ct = c.termlinkTemplates();
                    if (!ct.contains(prev))
                        ct.add(prev);
                    AbstractConcept.linkTerm(c, prev, UnitBudget.Mid, 1f, true, false);
                }

                if (forward) {
                    List<Termed> pt = prev.termlinkTemplates();
                    if (!pt.contains(c))
                        pt.add(c);
                    AbstractConcept.linkTerm(prev, c, UnitBudget.Mid, 1f, true, false);
                }



            }


            prev = c;
        }


        return Tuples.twin(first,last);
    }

    public static void main(String[] args)  {

        Default n = new Default(128,4,1,1) {
            @Override
            public Function<Term, Concept> newConceptBuilder() {
                return new DefaultConceptBuilder(this.memory.random, 16, 16) {

                    @Override
                    protected
                    @NotNull
                    AtomConcept newAtomConcept(Term t, Bag<Task> taskLinks, Bag<Termed> termLinks) {
                        return new AtomConcept(t, termLinks, taskLinks) {


                            public List<Termed> templates = new FasterList();

                            public List<Termed> termlinkTemplates() {
                                return templates;
                            }
                        };
                    }

                };
            }

        };
        n.memory.duration.set(5);
        n.memory.perfection.setValue(0);
        n.run(5);

        graphIDE(n, (e)-> {

            IntToObjectFunction<Termed> tt = (i) -> $.the("a" + i);
            int chainSize = 8;
            Twin<Termed> ends = addTermLinkChain(n, chainSize, tt, true,false);
            n.forEachConcept(c->c.print());

            new Thread(()-> {
                while (true) {
                    System.out.println("press enter to fire");
                    try {
                        System.in.read();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    System.out.println("firing");

                    n.runLater(() -> {
                        n.conceptualize(

                                tt.valueOf(
                                        0
                                        //n.memory.random.nextInt(chainSize)
                                ),

                                new UnitBudget(1f, 0.5f, 0.5f), 1f, 0.6f);
                    });
                }
            }).start();
        });
    }
}
