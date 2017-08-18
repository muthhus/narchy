package nars.gui;

import com.google.common.collect.Lists;
import com.jogamp.opengl.GL2;
import jcog.bag.Bag;
import jcog.data.FloatParam;
import jcog.event.On;
import jcog.pri.PriReference;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.Task;
import nars.bag.leak.LeakOut;
import nars.concept.Concept;
import nars.exe.FocusExec;
import nars.gui.graph.EdgeDirected;
import nars.gui.graph.run.SimpleConceptGraph1;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import spacegraph.AbstractSpace;
import spacegraph.Ortho;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.layout.Flatten;
import spacegraph.layout.ForceDirected;
import spacegraph.layout.Grid;
import spacegraph.layout.Stacking;
import spacegraph.math.Color3f;
import spacegraph.render.SpaceGraph2D;
import spacegraph.space.CrosshairSurface;
import spacegraph.widget.Label;
import spacegraph.widget.LabeledPane;
import spacegraph.widget.button.CheckBox;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.console.ConsoleSurface;
import spacegraph.widget.console.ConsoleTerminal;
import spacegraph.widget.console.TerminalUI;
import spacegraph.widget.meta.ReflectionSurface;
import spacegraph.widget.meter.Plot2D;
import spacegraph.widget.slider.FloatSlider;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;
import static spacegraph.SpaceGraph.window;
import static spacegraph.layout.Grid.col;
import static spacegraph.layout.Grid.row;

/**
 * SpaceGraph-based visualization utilities for NAR analysis
 */
public class Vis {


    public static ConsoleTerminal newInputEditor() {
        return new ConsoleTerminal(new TerminalUI(20, 5));
    }

    public static Grid beliefCharts(int window, NAR nar, Object... x) {
        return beliefCharts(window, Lists.newArrayList(x), nar);
    }

    public static Grid beliefCharts(int window, Iterable ii, NAR nar) {

        return new BeliefChartsGrid(ii, nar, window);
    }


    //    public static <X extends Termed> BagChart<X> items(Bag<X,PLink<X>> bag, final Cycles d, final int count) {
//        BagChart tc = new BagChart(bag, count) {
//            @Override
//            public void accept(PLink x, ItemVis y) {
//                float p = x.pri();
//
//                float[] f = Draw.hsb(
//                        (0.3f * x.get().hashCode() / (float) Integer.MAX_VALUE),
//                        .5f + 0.25f * p, 0.5f + 0.25f * p, 1f, null);
//                y.update(p, f[0], f[1], f[2]);
//
//            }
//        };
//
//        d.onCycle(xx -> {
//
//            //if (s.window.isVisible()) {
//            tc.update();
//            //}
//        });
//
//        return tc;
//    }

//    public static Surface budgetHistogram(NAR nar, int bins) {
//        if (nar instanceof Default) {
//            return budgetHistogram((Iterable) nar.focus().concepts(), bins);
//        } else { //if (nar instance)
//            //return budgetHistogram(((Default2)nar).active, bins);
//            return grid(); //TODO
//        }
//    }

    public static Surface budgetHistogram(Iterable<PriReference> bag, int bins) {
        //new SpaceGraph().add(new Facial(

        double[] d = new double[bins];
        return col(
                Vis.pane("Concept Priority Distribution (0..1)",
                        new HistogramChart(
                                () -> Bag.priHistogram(bag, d),
                                new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.1f))
                )
//                Vis.pane("Concept Volume",
//                        new HistogramChart(
//                                () -> Bag.priHistogram(bag, d),
//                                new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.1f))
//                )
        );

//                PanelSurface.of("Concept Durability Distribution (0..1)", new HistogramChart(nar, c -> {
//                    if (c != null)
//                        return c.dur();
//                    return 0;
//                }, bins, new Color3f(0f, 0.25f, 0.5f), new Color3f(0.1f, 0.5f, 1f)))

    }

//    public static Grid conceptLinePlot(NAR nar, Iterable<? extends Termed> concepts, int plotHistory, FloatFunction<Termed> value) {
//
//        //TODO make a lambda Grid constructor
//        Grid grid = new Grid();
//        List<Plot2D> plots = $.newArrayList();
//        for (Termed t : concepts) {
//            Plot2D p = new Plot2D(plotHistory, Plot2D.Line /*BarWave*/);
//            p.add(t.toString(), () -> value.floatValueOf(t), 0f, 1f);
//            grid.children.add(p);
//            plots.add(p);
//        }
//        grid.layout();
//
//        nar.onCycle(f -> {
//            plots.forEach(Plot2D::update);
//        });
//
//        return grid;
//    }

    public static Grid conceptBeliefPlots(NAgent a, Iterable<? extends Termed> concepts, int plotHistory) {

        //TODO make a lambda Grid constructor
        Grid grid = new Grid();
        List<Plot2D> plots = $.newArrayList();
        for (Termed t : concepts) {
            Plot2D p = new Plot2D(plotHistory, Plot2D.Line /*BarWave*/) {

                @Override
                protected void paint(GL2 gl) {
                    Concept concept = a.nar.concept(t);

                    Truth t = a.nar.beliefTruth(concept, a.nar.time());
                    float b;
                    if (t == null)
                        b = 0f;
                    else
                         b = 2f * (t.freq()) -1f;

                    backgroundColor[0] = b < 0 ? -b/4f : 0;
                    backgroundColor[1] = 0;
                    backgroundColor[2] = b >= 0 ? b/4f : 0;
                    backgroundColor[3] = 0.9f;

                    super.paint(gl);
                }

            };
            p.setTitle(t.toString());
//            p.add("P", () -> a.nar.pri(t, Float.NaN), 0f, 1f);
//            p.add("G", () -> a.nar.concept(t).goalFreq(nar.time(), nar.dur()), 0f, 1f);
            p.add("B", () -> {
                Truth b = a.nar.beliefTruth(t, a.nar.time());
                return b!=null ? b.freq() : Float.NaN;
            }, 0f, 1f);
            p.add("G", () -> {
                Truth b = a.nar.goalTruth(t, a.nar.time());
                return b!=null ? b.freq() : Float.NaN;
            }, 0f, 1f);

            grid.children.add(p);
            plots.add(p);
        }
        grid.layout();

        a.onFrame(f -> {
            plots.forEach(Plot2D::update);
        });

        return grid;
    }


//    public static Grid agentBudgetPlot(NAgent t, int history) {
//        return conceptLinePlot(t.nar,
//                Iterables.concat(t.actions, Lists.newArrayList(t.happy, t.joy)), history);
//    }

    public static Grid emotionPlots(NAgent a, int plotHistory) {

        return new EmotionPlot(plotHistory, a);
    }

    public static Label label(Object x) {
        return label(x.toString());
    }

    public static Label label(String text) {
        return new Label(text);
    }

    /**
     * ordering: first is underneath, last is above
     */
    public static Stacking stack(Surface... s) {
        return new Stacking(s);
    }

    public static LabeledPane pane(String k, Surface s) {
        return new LabeledPane(k, s);
    }

    public static SimpleConceptGraph1 conceptsWindow3D(NAR n, int maxNodes, int maxEdges) {


        SimpleConceptGraph1 cs = new SimpleConceptGraph1(n,
                ((FocusExec)n.exe).concepts //TODO generalize
                /* TODO */, 128, 256, 2, 7);


        SpaceGraph<Term> s = new SpaceGraph(

                cs.with( new Flatten() )
        );

        EdgeDirected fd = new EdgeDirected();
        s.dyn.addBroadConstraint(fd);

        s.camPos(0, 0, 90).show(1300, 900);
        //s.ortho(Vis.logConsole(nar, 90, 40, new FloatParam(0f)).opacity(0.25f));


        //Vis.conceptsWindow2D

                //.add(new ZoomOrtho(logConsole(n, 120, 40, new FloatParam(0.25f)).opacity(0.5f)))
                //.ortho( logConsole(n, 40, 10, 0.0f) )

        window(reflect(fd), 500, 500);

        //s.ortho(Vis.logConsole(nar, 90, 40, new FloatParam(0f)).opacity(0.25f));

        return cs;

    }

//    public static SpaceGraph<Term> conceptsWindow2D(NAR nar, int maxNodes, int maxEdges) {
//        return conceptsWindow(new ConceptsSpace(nar, maxNodes, 1, maxEdges));
//    }
//
//    public static SpaceGraph<Term> conceptsWindow2D(NAR nar, Iterable<? extends Termed> terms, int max, int maxEdges) {
//        List<ConceptWidget> termWidgets = StreamSupport.stream(terms.spliterator(), false).map(x -> new ConceptWidget(x.term())).collect(toList());
//
//        NARSpace active = new NARSpace(nar) {
//
//            final ObjectFloatHashMap<Term> priCache = new ObjectFloatHashMap<>();
//            final FloatFunction<Term> termFloatFunction = k -> nar.pri(k);
//
//            @Override
//            protected void get(Collection displayNext) {
//                Collections.sort(termWidgets, (a, b) -> {
//                    return Float.compare(
//                            priCache.getIfAbsentPutWithKey(b.key, termFloatFunction),
//                            priCache.getIfAbsentPutWithKey(a.key, termFloatFunction)
//                    );
//                });
//                priCache.clear();
//
//                for (int i = 0; i < max; i++) {
//                    ConceptWidget w = termWidgets.get(i);
//                    displayNext.add(w);
//                }
//            }
//        };
//
//        return conceptsWindow(
//                active
//        );
//    }

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
                )).with(fd = new EdgeDirected());

        s.add(new Ortho(new CrosshairSurface(s)));

        window(new ReflectionSurface(fd), 500, 500);

        return s;
    }

    public static ConsoleSurface logConsole(NAR nar, int cols, int rows, FloatParam priMin) {
        ConsoleSurface term = new ConsoleTerminal(cols, rows);
        new LeakOut(nar, 4, 0.25f) {

            @Override
            public boolean preFilter(@NotNull Task t) {
                if (t.pri() >= priMin.floatValue()) {
                    return super.preFilter(t);
                }
                return false;
            }

            @Override
            protected float leak(Task t) {
                if (t.pri() >= priMin.floatValue()) {
                    try {
                        t.appendTo(term);
                        term.append('\n');
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return 1;
                }
                return 0;
            }
        };
        return term;
    }

    public static ReflectionSurface reflect(Object c) {
        return new ReflectionSurface(c);
    }

    public static class EmotionPlot extends Grid implements Consumer<NAR> {

        private final int plotHistory;
        private final On on;
        Plot2D plot1;
        Plot2D plot2;
        Plot2D plot3;
        Plot2D plot4;

        public EmotionPlot(int plotHistory, NAgent a) {
            this(plotHistory, a, a.nar);
        }

        public EmotionPlot(int plotHistory, NAgent a, NAR x) {
            super(Grid.VERTICAL);

            NAR nar = x;

            this.plotHistory = plotHistory;
            plot1 = new Plot2D(plotHistory, Plot2D.Line);
            plot2 = new Plot2D(plotHistory, Plot2D.Line);
            plot3 = new Plot2D(plotHistory, Plot2D.Line);
            plot4 = new Plot2D(plotHistory, Plot2D.Line);
            set(plot1, plot2, plot3, plot4);

            //plot1.add("Conf", nar.emotion.confident::getSum);
            plot2.add("Busy", nar.emotion.busyVol::getSum);
            plot3.add("Lern", nar.emotion::learningVol, 0f, 1f);

            plot1.add("Dex", a::dexterity, 0f, 1f);

            //plot4.add("Hapy", a.happy, 0f, 1f);
            plot4.add("Hapy", () -> a.reward, -1f, 1f);

//            plot4.add("Hapy", nar.emotion.happy::getSum);
//            plot4.add("Sad", nar.emotion.sad::getSum);
//                plot4.add("Errr", ()->nar.emotion.errr.getSum());

            on = nar.onCycle(this);
        }

        @Override
        public void stop() {
            super.stop();
            on.off();
        }

        @Override
        public void accept(NAR nar) {
            plot1.update();
            plot2.update();
            plot3.update();
            plot4.update();
        }
    }

    public static class BeliefChartsGrid extends Grid implements Consumer<NAR> {

        private final int window;
        private final On on;
        long[] btRange;

        public BeliefChartsGrid(Iterable<?> ii, NAR nar, int window) {
            super();

            btRange = new long[2];
            this.window = window;

            List<Surface> s = StreamSupport.stream(ii.spliterator(), false)
                    .map(x -> x instanceof Termed ? (Termed) x : null).filter(Objects::nonNull)
                    .map(c -> new BeliefTableChart(nar, c, btRange)).collect(toList());

            if (!s.isEmpty()) {
                set(s);
                on = nar.onCycle(this);
            } else {
                on = null;
                set(label("(empty)"));
            }

        }

        @Override
        public void stop() {
            if (on != null)
                on.off();
        }

        @Override
        public void accept(NAR nar) {
            long now = nar.time();
            int dur = nar.dur();
            btRange[0] = now - (window * dur);
            btRange[1] = now + (window * dur);
        }
    }
}
