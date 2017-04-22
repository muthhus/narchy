package nars.gui;

import jcog.bag.util.Bagregate;
import jcog.pri.PLink;
import nars.NAR;
import nars.Narsese;
import nars.Param;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.layout.Flatten;
import spacegraph.widget.button.PushButton;

import java.util.Collection;
import java.util.function.Function;

import static nars.gui.Vis.MyForceDirected;
import static nars.gui.Vis.reflect;
import static spacegraph.layout.Grid.col;

public class ConceptsSpace extends NARSpace<Term, ConceptWidget> {


    public static final float UPDATE_RATE = 0.5f;
    public final NAR nar;
    private final int maxEdgesPerNode;
    final Bagregate<Concept> bag;
    private final int maxNodes;
    public long now;
    public int dur;

    public ConceptsSpace(NAR nar, int maxNodes, int maxEdgesPerNode) {
        this(nar, maxNodes, maxNodes, maxEdgesPerNode);
    }

    public ConceptsSpace(NAR nar, int maxNodes, int bufferedNodes, int maxEdgesPerNode) {
        super(nar);
        this.nar = nar;
        this.maxNodes = maxNodes;
        this.maxEdgesPerNode = maxEdgesPerNode;
        bag = new Bagregate<Concept>(nar.concepts(), maxNodes + bufferedNodes, UPDATE_RATE) {
            @Override
            protected boolean include(Concept x) {
                return ConceptsSpace.this.include(x.term());
            }

            @Override
            public void onAdded(PLink<Concept> conceptPLink) {

            }

            @Override
            public void onRemoved(@NotNull PLink<Concept> value) {
                widgetRemove(value.get());
//                    cw.deactivate();
//                    //cw.delete();
//                }
            }
        };
    }



    @Override
    protected void get(Collection<ConceptWidget> displayNext) {

        bag.forEach(maxNodes, (PLink<Concept> concept) ->
            displayNext.add( widgetGetOrCreate(concept.get()) )
            //space.getOrAdd(concept.term(), materializer).setConcept(concept, now)
        );


        //System.out.println(nar.time() + " " + displayNext.size() );

    }

    ConceptWidget widgetRemove(Concept concept) {
        ConceptWidget cw = concept.remove(this);
        if (cw!=null) {
            cw.delete(space.dyn);
        }
        return cw;
    }

    ConceptWidget widgetGet(Concept concept) {
        return concept.get(this);
    }

    ConceptWidget widgetGetOrCreate(Concept concept) {
        @NotNull ConceptWidget cw = concept.meta(this, (k, p) -> {
            if (p == null) {
                ConceptWidget c = materializer().apply(concept);
                c.concept = concept;
                p = c;
            }
            return p;
        });

        cw.activate();

        return cw;
    }

    private Function<Termed, ConceptWidget> materializer() {
        return t -> new ConceptWidget(nar, t, maxEdgesPerNode);
    }

    @Override
    protected void update() {
        this.now = nar.time();
        this.dur = nar.dur();

        super.update();

        active.forEach(c -> c.commit(this));
    }


    public static void main(String[] args) throws Narsese.NarseseException {

        Param.DEBUG = false;

        Default n = new Default(512, 3);
        //Default n = NARBuilder.newMultiThreadNAR(1, new RealTime.DSHalf(true).durSeconds(0.05f));
        //n.nal(1);
//        n.termVolumeMax.setValue(7f);
//        n.DEFAULT_BELIEF_PRIORITY = 0.9f;
//        n.DEFAULT_GOAL_PRIORITY = 0.9f;
//        n.DEFAULT_QUESTION_PRIORITY = 0.01f;
//        n.DEFAULT_QUEST_PRIORITY = 0.01f;

//        n.inputAt(1, "c:a?");
//        n.inputAt(2, "b:a.");
//        n.inputAt(3, "c:b.");

        //new DeductiveChainTest(n, 8,  2048, inh);
        //n.mix.stream("Derive").setValue(0.005f); //quiet derivation
        n.focus.activationRate.setValue(0.2f);

        n.loop(12f);

        n.input("(x:a ==> x:b).",
                "(x:b ==> x:c).",
                "(x:c ==> x:d).",
                "(x:d ==> x:e).",
                "(x:e ==> x:f)."
//                "(x:f ==> x:g).",
//                "(x:g ==> x:h)."

                );
//        for (int i = 0; i < 10; i++) {
//            n.inputAt(i * 5 , i % 2 == 0 ? "x:c! :|:" : "--x:c! :|:");
//        }


        //new DeductiveMeshTest(n, new int[] {3, 3}, 16384);

        NARSpace cs = new ConceptsSpace(n, 64, 8) {
//            @Override
//            protected boolean include(Term term) {
//
//                return term instanceof Compound &&
//                        term.complexity()==3 && term.toString().endsWith("-->x)");
//            }
        };


        SpaceGraph<Term> s = new SpaceGraph(

                cs.with(
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

                        new Flatten()
//                        new Flatten() {
//                            protected void locate(SimpleSpatial s, v3 f) {
//                                f.set(s.x(), s.y(), 10 - ((Term) (s.key)).volume() * 1);
//                            }
//                        }


                        //new Spiral()
//                        //new FastOrganicLayout()
                )
        ) {
//            @Override
//            protected void initLighting() {
//                //no
//            }
        };

        s.dyn.addBroadConstraint(new MyForceDirected());

        //s.ortho(Vis.logConsole(nar, 90, 40, new FloatParam(0f)).opacity(0.25f));


        //Vis.conceptsWindow2D
        s

                //.add(new ZoomOrtho(logConsole(n, 120, 40, new FloatParam(0.25f)).opacity(0.5f)))
                .camPos(0, 0, 90)
                //.ortho( logConsole(n, 40, 10, 0.0f) )
                .show(1300, 900);

        SpaceGraph.window(
            col(
                reflect( new CycleView(n) ),
                new PushButton("+", () -> {
                    try {
                        n.input("x:h! :|:");
                    } catch (Narsese.NarseseException e) {
                        e.printStackTrace();
                    }
                }),
                new PushButton("-", () -> {
                    try {
                        n.input("--x:h! :|:");
                    } catch (Narsese.NarseseException e) {
                        e.printStackTrace();
                    }
                })
            ),
        400, 400);

        //n.log();
        //n.loop(2f);



        //n.input("(a-->b).", "(b-->c).","(c-->d).");

        //new DeductiveChainTest(n, 10, 9999991, (x, y) -> $.p($.the(x), $.the(y)));

        //n.linkFeedbackRate.setValue(0.05f);


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


    }

}
