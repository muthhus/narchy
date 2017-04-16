package nars.gui;

import com.google.common.collect.Iterables;
import jcog.bag.PLink;
import nars.NAR;
import nars.Narsese;
import nars.Param;
import nars.bag.Bagregate;
import nars.concept.Concept;
import nars.nar.Default;
import nars.nar.NARBuilder;
import nars.term.Term;
import nars.term.Termed;
import nars.time.RealTime;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.Spatial;
import spacegraph.widget.button.PushButton;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import static nars.gui.Vis.MyForceDirected;
import static nars.gui.Vis.reflect;
import static spacegraph.layout.Grid.col;

public class ConceptsSpace extends NARSpace<Term, ConceptWidget> {


    public static final float UPDATE_RATE = 0.1f;
    public final NAR nar;
    private final int maxEdgesPerNode;
    final Bagregate<Concept> bag;
    public long now;
    public int dur;

    public ConceptsSpace(NAR nar, int maxNodes, int maxEdgesPerNode) {
        super(nar);
        this.nar = nar;
        this.maxEdgesPerNode = maxEdgesPerNode;
        bag = new Bagregate<Concept>(Iterables.transform(nar.conceptsActive(), Supplier::get), maxNodes, UPDATE_RATE) {
            @Override
            protected boolean include(Concept x) {
                return ConceptsSpace.this.include(x.term());
            }

            @Override
            public void onRemoved(@NotNull PLink<Concept> value) {
                ConceptWidget cw = widgetGet(value.get());
                if (cw!=null) {
                    //cw.hide();
                    cw.delete();
                }
            }
        };
    }

    @Override
    protected void get(Collection<ConceptWidget> displayNext) {



        //long now = nar.time();

        bag.forEach((PLink<Concept> b) ->{

            Concept concept = b.get();

            displayNext.add(
                widgetGetOrCreate(concept)
                //space.getOrAdd(concept.term(), materializer).setConcept(concept, now)
            );

        });

        //System.out.println(nar.time() + " " + displayNext.size() );

    }

    public ConceptWidget widgetGet(Concept concept) {
        return concept.get(this);
    }

    public ConceptWidget widgetGetOrCreate(Concept concept) {
        return concept.meta(this, (k,p) -> {
            if (p == null) {
                ConceptWidget c = materializer().apply(concept);
                c.concept = concept;
                c.activate();
                return c;
            } else {
                ((Spatial)p).activate();
                return p;
            }
        });
    }

    private Function<Termed, ConceptWidget> materializer() {
        return t -> new ConceptWidget(nar, t, maxEdgesPerNode);
    }

    @Override
    protected void update() {
        super.update();


        this.now = nar.time();
        this.dur = nar.dur();
        active.forEach(c -> c.commit(this));
    }


    public static void main(String[] args) throws Narsese.NarseseException {

        Param.DEBUG = false;

        //Default n = new Default(64, 3, 1, 3);
        Default n = NARBuilder.newMultiThreadNAR(3, new RealTime.DSHalf(true).durSeconds(0.2f));
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

        n.loop(1f);

        n.input("(x:a ==> x:b).",
                "(x:b ==> x:c).",
                "(x:c ==> x:d).",
                "(x:d ==> x:e).",
                "(x:e ==> x:f).",
                "(x:f ==> x:g).",
                "(x:g ==> x:h)."

                );
//        for (int i = 0; i < 10; i++) {
//            n.inputAt(i * 5 , i % 2 == 0 ? "x:c! :|:" : "--x:c! :|:");
//        }


        //new DeductiveMeshTest(n, new int[] {3, 3}, 16384);

        NARSpace cs = new ConceptsSpace(n, 32, 8) {
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

                        //new Flatten()
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
