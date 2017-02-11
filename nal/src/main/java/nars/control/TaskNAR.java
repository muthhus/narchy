package nars.control;

import jcog.data.MutableInteger;
import jcog.data.random.XorShift128PlusRandom;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.attention.Activation;
import nars.attention.Forget;
import nars.bag.Bag;
import nars.bag.HijackBag;
import nars.budget.Budget;
import nars.budget.BudgetMerge;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.conceptualize.DefaultConceptBuilder;
import nars.conceptualize.state.ConceptState;
import nars.conceptualize.state.DefaultConceptState;
import nars.derive.DefaultDeriver;
import nars.derive.Deriver;
import nars.index.task.TaskIndex;
import nars.index.term.map.CaffeineIndex;
import nars.link.BLink;
import nars.link.DependentBLink;
import nars.premise.Derivation;
import nars.premise.PreferSimpleAndConfidentPremise;
import nars.premise.Premise;
import nars.premise.PremiseBuilder;
import nars.term.Term;
import nars.term.Termed;
import nars.time.FrameTime;
import nars.time.Time;
import nars.truth.TruthDelta;
import nars.util.exe.Executioner;
import nars.util.exe.MultiThreadExecutioner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static nars.Op.*;

/**
 * heuristic task-driven NAR model without concept link network
 */
public class TaskNAR extends NAR {

    final static Logger logger = LoggerFactory.getLogger(TaskNAR.class);

    public final Bag<Task,BLink<Task>> tasksBag;
    final Deriver deriver = new DefaultDeriver();

    final MutableInteger derivationsPerCycle = new MutableInteger(128);

    final PremiseBuilder premiseBuilder = new PremiseBuilder() {

        @Override
        protected Premise newPremise(@NotNull Termed c, @NotNull Task task, Term beliefTerm, Task belief, float pri, float qua) {
            return new PreferSimpleAndConfidentPremise(c, task, beliefTerm, belief, pri, qua);
        }
    };

    static class SimpleConceptBuilder extends DefaultConceptBuilder {

        public SimpleConceptBuilder() {
            this(new DefaultConceptState("x",
                    16, 16,
                    3,
                    0, 0));
        }

        public SimpleConceptBuilder(ConceptState s) {
            super(s, s);
        }

        @NotNull
        @Override
        public <X> Bag<X,BLink<X>> newBag(@NotNull Map m) {
            return Bag.EMPTY;
        }

    }

    @Override
    protected TaskIndex newTaskIndex() {
        return new TaskBagIndex();
    }


    public TaskNAR(int capacity) {
        this(capacity, new MultiThreadExecutioner(2, 4096), new FrameTime());
    }

    public TaskNAR(int capacity, Executioner exe, Time time) {
        super(time, new CaffeineIndex(new SimpleConceptBuilder(), -1, false, exe),
                new XorShift128PlusRandom(1), exe);


        tasksBag = new HijackBag<Task>(capacity, 2, BudgetMerge.maxBlend, random) {
        //tasksBag = new CurveBag<Task>(capacity, new CurveBag.NormalizedSampler(power2BagCurve, random), BudgetMerge.maxBlend, new ConcurrentHashMap<>(capacity)) {

            @Override
            public Forget<Task> forget(float rate) {
                return new MyTaskForget(rate);
            }

//            @Override
//            protected void sortAfterUpdate() {
//                //do nothing here = only sort on commit
//
//                //float sortPercentage = 0.025f;
//                //sortPartial(sortPercentage);
//            }




            //final AtomicInteger size = new AtomicInteger();

            @Override
            public void onAdded(BLink<Task> value) {
                //value.get().state(nar.concepts.conceptBuilder().awake(), nar);
                //System.out.println("added: " + size());
//                if (size.incrementAndGet() > capacity) {
//                    //System.err.println("Wtf");
//                }
            }

            @Override
            public void onRemoved(@NotNull BLink<Task> value) {
                //System.out.println("removed: " + size());

                Task x = value.get();
                if (!x.isDeleted())
                    x.delete();

                if (!value.isDeleted())
                    value.delete();

                CompoundConcept c = (CompoundConcept) x.concept(TaskNAR.this);
                if (c != null) {
                    //runLater(() -> {

                        c.tableFor(x.punc()).remove(x);

                        if (!(c instanceof PermanentConcept)) {
                            //synchronized (c) {
                            if (c.taskCount() == 0) {
                                concepts.remove(c.term());
                                c.delete(TaskNAR.this);
                            }
                            //}
                        }
                    //});
                } else {
                    System.err.println("concept not found: " + x);
                }


                //value.get().state(nar.concepts.conceptBuilder().sleep(), nar);
            }
        };

        onCycle(this::cycle);


    }

    @Override
    protected Activation process(@NotNull Task x, Concept cc) {

        //TODO this duplicates CompoundConcept.process code

        boolean accepted = false;
        TruthDelta delta = null;
        CompoundConcept c = (CompoundConcept) cc;


        switch (x.punc()) {
            case BELIEF:
                delta = c.processBelief(x, this);
                break;

            case GOAL:
                delta = c.processGoal(x, this);
                break;

            case QUESTION:
                accepted = c.processQuestion(x, this);
                break;

            case QUEST:
                accepted = c.processQuest(x, this);
                break;

            default:
                throw new RuntimeException("Invalid sentence type: " + x);
        }

        x.feedback(null, Float.NaN, Float.NaN, this); //clear any stored premise

        if (accepted || delta != null) {

            /*if (delta!=null) {
                System.out.println(c + " : " + x + " : " + delta);
            }*/

            return new MyActivation(x, c);
        } else
            return null;
    }


    public void cycle() {
        tasksBag.commit();

        float minPri = tasksBag.priMin();

        float load = exe.load();

        int cpf = Math.round(derivationsPerCycle.floatValue() * (1f - load));
        if (cpf > 0) {

            int cbs = 8; //conceptsFiredPerBatch.intValue();

            //logger.info("firing {} concepts (exe load={})", cpf, load);

            while (cpf > 0) {

                int batchSize = Math.min(cpf, cbs);
                cpf -= cbs;

                runLater(() -> {
                    List<BLink<Task>> sampled = $.newArrayList(batchSize * 2);
                    tasksBag.sample(batchSize * 2, sampled::add);
                    int n = sampled.size();
                    for (int i = 0; i < n; )
                        derive(sampled.get(i++), i < n ? sampled.get(i++) : null, minPri);
                });
            }
        }
    }

    protected void processDuplicate(@NotNull Task input, Task existing) {
        /* n/a */
        if (existing!=input) {
            //set the task budget, the link budget will already have been merged prior to this
            Budget eb = existing.budget();
            if (!eb.equalsBudget(input.budget()))
                BudgetMerge.maxBlend.merge(eb, input, 1f);
        }

    }

    public void derive(BLink<Task> ba, BLink<Task> bb, float minPri) {

        if (ba == null || bb == null)
            return;

        Task a = ba.get();
        if (a == null)
            return;

        Task b = bb.get();
        if (b == null)
            return;

        Term t = b.term();

        Premise c = premiseBuilder.premise(a /* not necessary */,
                a, t, time(),
                this, 1f, minPri
        );

        if (c != null) {

            deriver.accept(new Derivation(this, c,
                    //                t -> {
                    //                    //if (t.pri() > a.pri() || t.pri() > b.pri())
                    //                        logger.info("{} {}\n\t{}", a, b, t);
                    //                    input(t);
                    //                }
                    this::input
            ));
        }

    }


    public static void main(String[] args) {

        TaskNAR n = new TaskNAR(1024) {
        };

        n.log();

        n.input("(a-->b).");
        n.input("(b-->c).");
        n.input("(c-->d).");
        n.input("(a-->d)?");
        n.run(1000);

        System.out.println();

        n.tasksBag.print();

    }

    private static class MyTaskForget extends Forget<Task> {
        public MyTaskForget(float rate) {
            super(rate);
        }

        @Override
        public void accept(@NotNull BLink<Task> bLink) {
            Task t = bLink.get();

            float tp = t.priSafe(-1);
            if (tp < 0) {
                bLink.delete();
                return;
            }

            if (t.isInput() && t.isEternal()) {
                //dont forget eternal input tasks and act as a copy of the task's budget
                bLink.set(t);
                return;
            } else {
                //make the link always less than the task. if the task decreases more than the link, then bring the link down too
                if (tp < bLink.pri()) {
                    bLink.set(t);
                }
            }


            super.accept(bLink);
        }
    }

    private static class MyActivation extends Activation {
        public MyActivation(@NotNull Task t, Concept c) {
            super(t, 1f, c, null);
        }
    }

    private final class TaskBagIndex implements TaskIndex {

        @Override
        public @Nullable Task addIfAbsent(@NotNull Task t) {
            BLink<Task> r = tasksBag.put(new DependentBLink(t));
            if (r == null) {
                t.delete();
                return t; //rejected
            }

            Task t2 = r.get();
            if (t2 != t) {
                return t2; //duplicate
            }

            return null; //accepted
        }

        @Override
        public void removeInternal(@NotNull Task tt) {
            tasksBag.remove(tt);
        }

        @Override
        public void clear() {
            tasksBag.clear();
        }

        @Override
        public void forEach(@NotNull Consumer<Task> each) {
            tasksBag.forEachKey(each);
        }

        @Override
        public boolean contains(@NotNull Task t) {
            return tasksBag.contains(t);
        }
    }
}
