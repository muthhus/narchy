package nars.gui;

import com.jogamp.newt.opengl.GLWindow;
import nars.$;
import nars.NAR;
import nars.bag.Bag;
import nars.bag.impl.experimental.HijackBag;
import nars.budget.merge.BudgetMerge;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.test.DeductiveMeshTest;
import nars.util.data.list.FasterList;
import nars.util.event.On;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import spacegraph.*;

import java.util.List;
import java.util.function.Consumer;


/**
 * thread-safe visualization of capacity-bound NAR data buffers
 */
public class NARSpace<X, Y extends Spatial<X>> extends ListSpace<X, Y> {

    public static final @NotNull Atomic L = $.the("_l");

    public interface TriConsumer<A,B,C> {
        void accept(A a, B b, C c);
    }

    private final TriConsumer<NAR, SpaceGraph<X>, List<Y>> collect;
    private On on;

    private NAR nar;

    public static void main(String[] args) {

        Default n = new Default(256, 1, 2, 1 );
        //n.nal(4);


        //new ArithmeticInduction(n);

        newConceptWindow(n,  64, 8);

        //n.run(20); //headstart

        n.DEFAULT_BELIEF_PRIORITY = 0.05f;


        //n.log();
        //n.input("(a<->b).", "(b<->c).");
        new DeductiveMeshTest(n, new int[]{4, 4}, 16384);

        //new DeductiveChainTest(n, 10, 9999991, (x, y) -> $.p($.the(x), $.the(y)));


        //Param.DEBUG = true;
//        n
//                //.log()
//                //.logSummaryGT(System.out, 0.05f)
//                .input(
////                        "((parent($X,$Y) && parent($Y,$Z)) <=> grandparent($X,$Z)).",
////                        "parent(c, p).",
////                        "parent(p, g).",
////                        "grandparent(p, #g)?"
//                        "x:(a,(b,c))."
////                        "$0.9;0.9;0.9$ (a,(b,(c,(d,e))))."
////
//                );
//                //.run(800);
//
        n.linkFeedbackRate.setValue(0.05f);
        n.loop(5f);
        //n.run(1);
//        n.forEachConcept(c -> {
//            c.print();
//        });

    }

    public static GLWindow newConceptWindow(Default nn, int maxNodes, int maxEdges) {

        Bag<Pair<ConceptWidget,Term>> edges =
                //new CurveBag(BudgetMerge.plusBlend, nn.random);
            new HijackBag(maxEdges*maxNodes, 4, BudgetMerge.plusBlend, nn.random);

        NARSpace<Term, Spatial<Term>> n = new NARSpace<>(nn, (nar, space, target) -> {
            Bag<Concept> x = ((Default) nar).core.concepts;

            //System.out.println(((Default) nar).core.concepts.size() + " "+ ((Default) nar).index.size());


            x.topWhile(b -> {

                //Concept Core
                Concept concept = b.get();

                ConceptWidget root = space.update(concept.term(),
                        t -> new ConceptWidget(t, nar));

                //float bPri = root.pri = b.priIfFiniteElseZero();
                target.add(root);

                root.clearEdges();

                Consumer<BLink<? extends Termed>> absorb = tgt -> {
                    Term tt = tgt.get().term();
                    if (!tt.equals(root.key)) {
                        edges.put(Tuples.pair(root, tt), tgt);
                    }
                };

                //phase 1: collect
                concept.tasklinks().forEach(absorb);
                concept.termlinks().forEach(absorb);


//                concept.termlinks().forEach(bt -> {
//
//                    final Term tlTarget = bt.get();
//                    if (tlTarget.equals(ss))
//                        return; //no self loop
//
//                    ConceptWidget termLinkConnection = newLinkWidget(nar, space, root, ss, tlTarget, bt, false);
//                    if (termLinkConnection!=null) {
//                        termLinkConnection.pri = bt.pri() * (bPri*1.5f); //scale by its Concept's priority
//                        target.add(termLinkConnection);
//                    }
//                });

                return true;

            }, maxNodes);


            //phase 2: add edges
            edges.forEach(eb -> {
                Pair<ConceptWidget, Term> ebt = eb.get();
                ebt.getOne().addLink(space, ebt.getTwo(), eb);
            });
            edges.clear();


        }, maxNodes);



        SpaceGraph s = new SpaceGraph<>(

                n.with(
//                        new SpaceTransform<Term>() {
//                            @Override
//                            public void update(SpaceGraph<Term> g, AbstractSpace<Term, ?> src, float dt) {
//                                float cDepth = -9f;
//                                src.forEach(s -> {
//                                    ((SimpleSpatial)s).moveZ(
//                                            s.key.volume() * cDepth, 0.05f );
//                                });
//                            }
//                        }
//                        new Flatten()
//                        //new Spiral()
//                        //new FastOrganicLayout()
                )
        );

        s.dyn.addBroadConstraint(new ForceDirected());



        return s.show(1300, 900);
    }

//    public static ConceptWidget newLinkWidget(final NAR nar, SpaceGraph<Term> space, final ConceptWidget core, Term SRC, Term TARGET, BLink bt, boolean task) {
//
//
//
//        @NotNull Compound vTerm = $.p(L, SRC, TARGET);
//        SimpleSpatial targetSpatial = (SimpleSpatial) space.getIfActive(TARGET);
//        if (targetSpatial!=null) {
//            ConceptWidget termLink = space.update(vTerm,
//                    t -> new ConceptWidget(t, nar) {
//
//                        //                                @Override
////                                public Dynamic newBody(boolean collidesWithOthersLikeThis) {
////                                    shape = new SphereShape(.5f);
////                                    Dynamic bb = super.newBody(collidesWithOthersLikeThis);
////                                    return bb;
////                                }
//
//
//                        @Override
//                        protected String label(Term term) {
//                            return "";
//                        }
//
//                        @Override
//                        public void update(SpaceGraph<Term> s) {
//                            super.update(s);
//
//                            clearEdges();
//
//
//                            EDraw in = addEdge(bt, core, task);
//                            in.attraction = 0.25f;
//
//
//                            EDraw out = addEdge(bt, targetSpatial, task);
//                            out.attraction = 1f + (0.5f * bt.priIfFiniteElseZero());
//
//
//                        }
//                    });
//            if (termLink!=null) {
//                termLink.pri = bt.priIfFiniteElseZero();
//            }
//            return termLink;
//        }
//
//        return null;
//
//    }


    private final int capacity;

    //public final MutableFloat maxPri = new MutableFloat(1.0f);
    //public final MutableFloat minPri = new MutableFloat(0.0f);


    //private String keywordFilter;
    //private final ConceptFilter eachConcept = new ConceptFilter();


    public NARSpace(@Nullable TriConsumer<NAR, SpaceGraph<X>, List<Y>> collect, int capacity) {
        super();
        this.capacity = capacity;
        this.collect = collect == null ? (TriConsumer<NAR, SpaceGraph<X>, List<Y>>) this : collect;
    }

    public NARSpace(NAR nar, @Nullable TriConsumer<NAR, SpaceGraph<X>, List<Y>> collect, int capacity) {
        this(collect, capacity);
        start(nar);
    }

    public final synchronized void start(NAR nar) {
        if (on != null)
            throw new RuntimeException("already running");
        this.nar = nar;
        on = nar.onFrame(nn -> updateIfNotBusy(this::update));
    }

    @Override
    public final synchronized void stop() {
        on.off();
        on = null;
    }

    public final boolean running() {
        return on != null;
    }


    @Override
    public long now() {
        return nar.time();
    }



    protected void update(AbstractSpace _notused) {

        this.space = _notused.space;

        List<Y> prev = active;

        prev.forEach((y) -> y.preactivate(false));

        FasterList<Y> next = new FasterList<>(capacity);

        //gather the items, preactivating them
        collect.accept(nar, space, next);

        //remove missing
        for (int i = 0, prevSize = prev.size(); i < prevSize; i++) {
            Spatial y = prev.get(i);
            if (!y.preactive) {
                y.stop();
            }
        }

        //commit the changes
        this.active = next;
    }


}
