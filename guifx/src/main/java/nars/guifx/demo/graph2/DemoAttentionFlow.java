//package nars.guifx.demo.graph2;
//
//import com.gs.collections.api.block.function.primitive.IntToObjectFunction;
//import com.gs.collections.api.tuple.Twin;
//import com.gs.collections.impl.tuple.Tuples;
//import nars.$;
//import nars.NAR;
//import nars.budget.Budget;
//import nars.budget.UnitBudget;
//import nars.concept.AbstractConcept;
//import nars.concept.Concept;
//import nars.concept.TermTemplate;
//import nars.guifx.demo.AbstractNARGraphDemo;
//import nars.nar.Default;
//import nars.task.MutableTask;
//import nars.term.Compound;
//import nars.term.Termed;
//import org.apache.commons.lang3.mutable.MutableFloat;
//
//import java.io.IOException;
//import java.util.List;
//
///**
// * Created by me on 2/23/16.
// */
//public class DemoAttentionFlow extends AbstractNARGraphDemo {
//
//    static Twin<Termed> addTermLinkChain(NAR nar, int n, IntToObjectFunction<Termed> each, boolean forward, boolean reverse) {
//        Termed first = null, last = null;
//
//        Concept prev = null;
//
//        MutableFloat overflowing = new MutableFloat();
//
//        for (int i = 0; i < n; i++) {
//            Termed x = each.valueOf(i);
//            if (i == 0) first = x;
//            else if (i == n-1) last = x;
//            Budget initialConceptBudget = UnitBudget.Mid;
//            Concept c = nar.conceptualize(x, initialConceptBudget);
//
//            /* linking function, TODO extract as a lambda for alternate procedures */
//            if (prev!= null) {
//
//                if (reverse) {
//                    linkHalf(prev, overflowing, c, nar);
//
//                }
//
//                if (forward) {
//                    linkHalf(c, overflowing, prev, nar);
//                }
//
//
//
//            }
//
//
//            prev = c;
//        }
//
//
//        return Tuples.twin(first,last);
//    }
//
//    private static void linkHalf(Concept prev, MutableFloat overflowing, Concept c, NAR nar) {
//        //the reverse
//        List<TermTemplate> ct = c.termlinkTemplates();
//        if (ct!=null) {
//            if (!ct.contains(prev))
//                ct.add(new TermTemplate(prev, 1f));
//        }
//        AbstractConcept.linkSub(c, prev, UnitBudget.Mid, 1f, false, overflowing, null, nar);
//    }
//
//    public static void main(String[] args)  {
//
//        Default n = new Default(128,4,1,1) {
////            @Override
////            public Function<Term, Concept> newConceptBuilder() {
////                return new DefaultConceptBuilder(this.memory.random, 16, 16) {
////
////                    @Override
////                    protected
////                    @NotNull
////                    AtomConcept newAtomConcept(Term t, Bag<Task> taskLinks, Bag<Termed> termLinks) {
////                        return new AtomConcept(t, termLinks, taskLinks) {
////
////
////                            public List<Termed> templates = new FasterList();
////
////                            public List<Termed> termlinkTemplates() {
////                                return templates;
////                            }
////                        };
////                    }
////
////                };
////            }
//
//        };
//
//        n.perfection.setValue(0);
//
//
//        IntToObjectFunction<Termed> tt = (i) ->
//                //$.$("a:" + i);
//                $.$("a" + i + ":b" + i); //dstinct
//                //$.$("a" + i);
//
//        int chainSize = 16;
//        Twin<Termed> ends = addTermLinkChain(n, chainSize, tt, true,false);
//        n.forEachConcept(Concept::print);
//        //n.log();
//
//        n.step();
//
//        graphIDE(n, (e)-> {
//
//            n.onCycle(m-> printState(n, tt, chainSize));
//
//            new Thread(()-> {
//                while (true) {
//                    System.out.println("press enter to fire");
//                    try {
//                        System.in.read();
//                    } catch (IOException e1) {
//                        e1.printStackTrace();
//                    }
//
//                    System.out.println("firing");
//                    fire(n,tt.valueOf(0));
//                }
//            }).start();
//        });
//
//
//        console(n, tt, chainSize);
//    }
//
//    public static void console(Default n, IntToObjectFunction<Termed> tt, int chainSize) {
//        fire(n, tt.valueOf(
//                0
//                //n.memory.random.nextInt(chainSize)
//        ));
//
//        int time = 10;
//        for (int i = 0; i < time; i++) {
//            //printState(n, tt, chainSize);
//            n.step();
//        }
//    }
//
//    public static void printState(Default n, IntToObjectFunction<Termed> tt, int chainSize) {
//        for (int j = 0; j < chainSize; j++) {
//            System.out.print( n.conceptPriority( tt.valueOf(j), 0f ) + ", " );
//        }
//        System.out.println();
//    }
//
//    protected static void fire(NAR n, Termed t) {
//        UnitBudget b = new UnitBudget(1f, 0.5f, 0.5f);
//
//        if (t instanceof Compound) {
//            n.input(
//                    new MutableTask(t).belief().budget(
//                            b).present(n)
//            );
//        } else {
//            n.conceptualize(t, b);
//        }
//
//    }
//}
