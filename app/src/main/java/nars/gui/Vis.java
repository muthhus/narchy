package nars.gui;

import com.google.common.collect.Lists;
import com.jogamp.opengl.GL2;
import jcog.bag.Bag;
import jcog.bag.PLink;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import nars.util.Cycles;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.Nullable;
import spacegraph.*;
import spacegraph.layout.Flatten;
import spacegraph.layout.ForceDirected;
import spacegraph.layout.Spiral;
import spacegraph.math.Color3f;
import spacegraph.math.v3;
import spacegraph.phys.Collidable;
import spacegraph.phys.collision.broad.Broadphase;
import spacegraph.render.Draw;
import spacegraph.render.SpaceGraph2D;
import spacegraph.space.CrosshairSurface;
import spacegraph.space.layout.Grid;
import spacegraph.space.layout.Stacking;
import spacegraph.space.widget.*;
import spacegraph.space.widget.console.ConsoleSurface;
import spacegraph.space.widget.console.ConsoleTerminal;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static spacegraph.space.layout.Grid.*;

/**
 * SpaceGraph-based visualization utilities for NAR analysis
 */
public class Vis {
    public static void newBeliefChartWindow(NAgent narenv, long window) {
        Grid chart = agentActions(narenv, window);
        new SpaceGraph().add(new Ortho(chart).maximize()).show(800, 600);
    }

    public static void newBeliefChartWindow(NAR nar, long window, Term... t) {
        Grid chart = agentActions(nar, Lists.newArrayList(t), window);
        new SpaceGraph().add(new Ortho(chart).maximize()).show(800, 600);
    }

    public static void newBeliefChartWindow(NAR nar, long window, List<? extends Termed> t) {
        Grid chart = agentActions(nar, t, window);
        new SpaceGraph().add(new Ortho(chart).maximize()).show(800, 600);
    }

    public static Surface newInputEditor(NAR nar) {
        return new ConsoleTerminal(new ConsoleSurface.EditTerminal(40,6));
    }

    public static Grid agentActions(NAR nar, Iterable<? extends Termed> cc, long window) {
        long[] btRange = new long[2];
        nar.onCycle(nn -> {
            long now = nn.time();
            btRange[0] = now - window;
            btRange[1] = now + window;
        });
        List<Surface> actionTables = $.newArrayList();
        for (Termed c : cc) {
            actionTables.add(new BeliefTableChart(nar, c, btRange));
        }

        return new Grid(VERTICAL, actionTables);
    }

    public static Grid agentActions(NAgent a, float window) {
        List<Termed> ii = Lists.newArrayList();
        ii.addAll(a.actions);
//        ii.add(a.happy);
//        ii.add(a.joy);

        NAR nar = a.nar;

        return beliefCharts(window, ii, nar);
    }

    public static Grid beliefCharts(float window, List<? extends Termed> ii, NAR nar) {
        long[] btRange = new long[2];
        nar.onCycle(nn -> {
            long now = nn.time();
            float dur = nn.time.dur();
            btRange[0] = now - (long) Math.ceil(window * dur);
            btRange[1] = now + (long) Math.ceil(window * dur);
        });
        List<Surface> s = ii.stream().map(c -> new BeliefTableChart(nar, c, btRange)).collect(toList());
        return new Grid(1 / 3f, s);
    }


    public static BagChart<Concept> conceptsTreeChart(final Default d, final int count) {
        return treeChart(d, d.core.active, count);
    }

    public static BagChart<Concept> treeChart(NAR nar, Bag<Concept,PLink<Concept>> b, final int count) {
        long[] now = new long[]{nar.time()};
        BagChart<Concept> tc = new BagChart<Concept>(b, count) {

            @Override
            public void accept(PLink<Concept> x, ItemVis<PLink<Concept>> y) {
                float p = x.pri();
                float ph = 0.25f + 0.75f * p;

                float r, g, b;

                Concept c = x.get();
                if (c != null) if (c instanceof Atomic) {
                    r = g = b = ph * 0.5f;
                } else {
                    float belief = 0;

                    long n = now[0];

                    float dur = nar.time.dur();
                    @Nullable Truth bt = c.beliefs().truth(n, dur);
                    if (bt != null)
                        belief = bt.conf();


                    float goal = 0;
                    @Nullable Truth gt = c.goals().truth(n, dur);
                    if (gt != null)
                        goal = gt.conf();

                    if (belief > 0 || goal > 0) {
                        r = 0;
                        g = 0.25f + 0.75f * belief;
                        b = 0.25f + 0.75f * goal;
                    } /*else if (c.hasQuestions() || c.hasQuests()) {
                        r = 1; //yellow
                        g = 1/2;
                        b = 0;
                    } */ else {
                        r = g = b = 0;
                    }
                }
                else {
                    r = g = b = 0.5f;
                }

                y.update(p, r, g, b);

            }
        };

        nar.onCycle(xx -> {

            //if (s.window.isVisible()) {
            now[0] = xx.time();
            tc.update();
            //}
        });

        return tc;
    }

    public static <X extends Termed> BagChart<X> items(Bag<X,PLink<X>> bag, final Cycles d, final int count) {
        BagChart tc = new BagChart(bag, count) {
            @Override
            public void accept(PLink x, ItemVis y) {
                float p = x.pri();

                float[] f = Draw.hsb(
                        (0.3f * x.get().hashCode() / (float) Integer.MAX_VALUE),
                        .5f + 0.25f * p, 0.5f + 0.25f * p, 1f, null);
                y.update(p, f[0], f[1], f[2]);

            }
        };

        d.onCycle(xx -> {

            //if (s.window.isVisible()) {
            tc.update();
            //}
        });

        return tc;
    }

    public static Surface budgetHistogram(NAR nar, int bins) {
        if (nar instanceof Default) {
            return budgetHistogram(((Default) nar).core.active, bins);
        } else { //if (nar instance)
            //return budgetHistogram(((Default2)nar).active, bins);
            return grid(); //TODO
        }
    }

    public static Surface budgetHistogram(Bag bag, int bins) {
        //new SpaceGraph().add(new Facial(

        double[] d = new double[bins];
        return //new GridSurface(VERTICAL,
                Vis.pane("Concept Priority Distribution (0..1)", new HistogramChart(
                        () -> bag.priHistogram(d), new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.1f)));

//                PanelSurface.of("Concept Durability Distribution (0..1)", new HistogramChart(nar, c -> {
//                    if (c != null)
//                        return c.dur();
//                    return 0;
//                }, bins, new Color3f(0f, 0.25f, 0.5f), new Color3f(0.1f, 0.5f, 1f)))

    }

    public static Grid conceptLinePlot(NAR nar, Iterable<? extends Termed> concepts, int plotHistory, FloatFunction<Termed> value) {

        //TODO make a lambda Grid constructor
        Grid grid = new Grid();
        List<Plot2D> plots = $.newArrayList();
        for (Termed t : concepts) {
            Plot2D p = new Plot2D(plotHistory, Plot2D.Line /*BarWave*/);
            p.add(t.toString(), () -> value.floatValueOf(t), 0f, 1f);
            grid.children.add(p);
            plots.add(p);
        }
        grid.layout();

        nar.onCycle(f -> {
            plots.forEach(Plot2D::update);
        });

        return grid;
    }

    public static Grid conceptLinePlot(NAR nar, Iterable<? extends Termed> concepts, int plotHistory) {

        //TODO make a lambda Grid constructor
        Grid grid = new Grid();
        List<Plot2D> plots = $.newArrayList();
        for (Termed t : concepts) {
            Plot2D p = new Plot2D(plotHistory, Plot2D.Line /*BarWave*/) {

                @Override
                protected void paint(GL2 gl) {
                    Concept concept = nar.concept(t);

                    float b = 2f * (concept.beliefFreq(nar.time(), 0.5f) - 0.5f);
                    backgroundColor[0] = b < 0 ? -b : 0;
                    backgroundColor[1] = b >= 0 ? b : 0;
                    backgroundColor[3] = 0.75f;


                    super.paint(gl);
                }

            };
            p.setTitle(t.toString());
            p.add("P", () -> nar.pri(t, Float.NaN), 0f, 1f);
            p.add("B", () -> nar.concept(t).beliefFreq(nar.time(), nar.time.dur()), 0f, 1f);
            p.add("G", () -> nar.concept(t).goalFreq(nar.time(), nar.time.dur()), 0f, 1f);
            grid.children.add(p);
            plots.add(p);
        }
        grid.layout();

        nar.onCycle(f -> {
            plots.forEach(Plot2D::update);
        });

        return grid;
    }


//    public static Grid agentBudgetPlot(NAgent t, int history) {
//        return conceptLinePlot(t.nar,
//                Iterables.concat(t.actions, Lists.newArrayList(t.happy, t.joy)), history);
//    }

    public static Grid emotionPlots(NAR nar, int plotHistory) {
//        Plot2D plot = new Plot2D(plotHistory, Plot2D.Line);
//        plot.add("Rwrd", reward);

        Plot2D plot1 = new Plot2D(plotHistory, Plot2D.Line);
        plot1.add("Conf", nar.emotion.confident::getSum);

        Plot2D plot2 = new Plot2D(plotHistory, Plot2D.Line);
        plot2.add("Busy", nar.emotion.busyPri::getSum);


        Plot2D plot3 = new Plot2D(plotHistory, Plot2D.Line);
        //plot3.add("Strs", () -> nar.emotion.stress.getSum());
        plot3.add("Lern", nar.emotion::learningPri);


        Plot2D plot4 = new Plot2D(plotHistory, Plot2D.Line);
        plot4.add("Hapy", nar.emotion.happy::getSum);
        plot4.add("Sad",nar.emotion.sad::getSum);

//                Plot2D plot4 = new Plot2D(plotHistory, Plot2D.Line);
//                plot4.add("Errr", ()->nar.emotion.errr.getSum());

        nar.onCycle(f -> {
            plot1.update();
            plot2.update();
            plot3.update();
            plot4.update();
        });

        return col(plot1, plot2, plot3, plot4);
    }

    public static Label label(String text) {
        return new Label(text);
    }

    /**
     * ordering: first is underneath, last is above
     */
    public static Stacking stacking(Surface... s) {
        return new Stacking(s);
    }

    public static LabeledPane pane(String k, Surface s) {
        return new LabeledPane(k, s);
    }

    public static SpaceGraph<Term> conceptsWindow3D(NAR nar, int maxNodes, int maxEdges) {


        NARSpace n = new ConceptsSpace(nar, maxNodes, maxEdges);


        SpaceGraph<Term> s = new SpaceGraph(

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

//                        new Flatten() {
//                            protected void locate(SimpleSpatial s, v3 f) {
//                                f.set(s.x(), s.y(), 10 - ((Term) (s.key)).volume() * 1);
//                            }
//                        }

//                        //new Spiral()
//                        //new FastOrganicLayout()
                )
        ) {
//            @Override
//            protected void initLighting() {
//                //no
//            }
        };

        s.dyn.addBroadConstraint(new MyForceDirected());


        return s;

    }

    public static SpaceGraph<Term> conceptsWindow2D(NAR nar, int maxNodes, int maxEdges) {
        return conceptsWindow(new ConceptsSpace(nar, maxNodes, maxEdges));
    }

    public static SpaceGraph<Term> conceptsWindow2D(NAR nar, Iterable<? extends Termed> terms, int max, int maxEdges) {
        List<ConceptWidget> termWidgets = StreamSupport.stream(terms.spliterator(), false).map(x -> new ConceptWidget(nar, x.term(), maxEdges)).collect(toList());

        NARSpace active = new NARSpace(nar) {

            final ObjectFloatHashMap<Term> priCache = new ObjectFloatHashMap<>();
            final FloatFunction<Term> termFloatFunction = k -> nar.pri(k, Float.NaN);

            @Override
            protected void get(Collection displayNext) {
                Collections.sort(termWidgets, (a, b) -> {
                    return Float.compare(
                            priCache.getIfAbsentPutWithKey(b.key, termFloatFunction),
                            priCache.getIfAbsentPutWithKey(a.key, termFloatFunction)
                    );
                });
                priCache.clear();

                for (int i = 0; i < max; i++) {
                    ConceptWidget w = termWidgets.get(i);
                    displayNext.add(w);
                }
            }
        };

        return conceptsWindow(
                active
        );
    }

    public static SpaceGraph<Term> conceptsWindow(AbstractSpace nn) {
        Surface controls = col(new PushButton("x"), row(new FloatSlider("z", 0, 0, 4)), new CheckBox("?"))
                .hide();


        ForceDirected fd;
        SpaceGraph<Term> s = new SpaceGraph2D<>()
//                .add(
//                        new Ortho(
////                                new FloatSlider("~", 0, 0, 1f).on((slider, v) -> {
////
////                                }).scale(100, 100).pos(0f, 0f)
//
//                                new ConsoleTerminal(new ConsoleSurface.EditTerminal(40,20))
//
////                                new CheckBox("").on((cb, v) -> {
////                                    if (!v)
////                                        controls.hide();
////                                    else
////                                        controls.scale(200,200f).pos(300f,300f);
////                                }).scale(100, 100).pos(0f, 0f)
//
//                        ).scale(500,500))
                .add(new Ortho(controls))
                .add(nn.with(
                        new Flatten()
                        //new Spiral()
                        //new FastOrganicLayout()
                )).with(fd = new MyForceDirected());

        s.add(new Ortho(new CrosshairSurface(s)));

        SpaceGraph.window(new ReflectionSurface(fd), 500, 500);

        return s;
    }

    private static class MyForceDirected extends ForceDirected {
        @Override
        public void solve(Broadphase b, List<Collidable> objects, float timeStep) {
            super.solve(b, objects, timeStep);

            float a = attraction.floatValue();

            for (Collidable c : objects) {

                Spatial A = ((Spatial) c.data());
                if (A instanceof ConceptWidget) {
                    ((ConceptWidget) A).edges.forEachKey(e -> {

                        ConceptWidget B = e.target;

                        if ((B.body != null)) {

                            attract(c, B.body, a * e.attraction, e.attractionDist);
                        }

                    });
                }

            }
        }
    }
}
