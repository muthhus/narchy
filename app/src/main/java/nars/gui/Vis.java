package nars.gui;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.Default;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.math.Color3f;
import spacegraph.obj.CrosshairSurface;
import spacegraph.obj.GridSurface;
import spacegraph.obj.PanelSurface;
import spacegraph.obj.Plot2D;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static spacegraph.obj.GridSurface.*;

/**
 * SpaceGraph-based visualization utilities for NAR analysis
 */
public class Vis {
    public static void newBeliefChartWindow(NAgent narenv, long window) {
        GridSurface chart = agentActions(narenv, window);
        new SpaceGraph().add(new Facial(chart).maximize()).show(800, 600);
    }

    public static void newBeliefChartWindow(NAR nar, long window, Term... t) {
        GridSurface chart = agentActions(nar, Lists.newArrayList(t), window);
        new SpaceGraph().add(new Facial(chart).maximize()).show(800, 600);
    }

    public static void newBeliefChartWindow(NAR nar, long window, List<? extends Termed> t) {
        GridSurface chart = agentActions(nar, t, window);
        new SpaceGraph().add(new Facial(chart).maximize()).show(800, 600);
    }


    public static GridSurface agentActions(NAR nar, Iterable<? extends Termed> cc, long window) {
        long[] btRange = new long[2];
        nar.onFrame(nn -> {
            long now = nn.time();
            btRange[0] = now - window;
            btRange[1] = now + window;
        });
        List<Surface> actionTables = $.newArrayList();
        for (Termed c : cc) {
            actionTables.add(new BeliefTableChart(nar, c, btRange));
        }

        return new GridSurface(VERTICAL, actionTables);
    }

    public static GridSurface agentActions(NAgent a, long window) {
        NAR nar = a.nar;
        long[] btRange = new long[2];
        nar.onFrame(nn -> {
            long now = nn.time();
            btRange[0] = now - window;
            btRange[1] = now + window;
        });
        List<Surface> s = a.actions.stream().map(c -> new BeliefTableChart(nar, c, btRange)).collect(toList());
        s.add(new BeliefTableChart(nar, a.happy, btRange));
        s.add(new BeliefTableChart(nar, a.joy, btRange));

        return new GridSurface(s);
    }

    public static void show(Default d) {
        show(d, -1);
    }

    public static void show(Default d, int count) {

        SpaceGraph<VirtualTerminal> s = new SpaceGraph<>(     );
        s.add(new Facial(grid(
                concepts(d, count),
                budgetHistogram(d, 32),
                emotionPlots(d, 256)
        )).maximize());
        s.add(new Facial(new CrosshairSurface(s)));


        s.show(1400, 800);


    }

    public static BagChart<Concept> concepts(final Default d, final int count) {
        BagChart<Concept> tc = new BagChart<Concept>(d.core.concepts, count) {
            @Override
            public void accept(BLink<Concept> x, ItemVis<BLink<Concept>> y) {
                float p = x.pri();
                float ph = 0.25f + 0.75f * p;

                float r, g, b;

                Concept c = x.get();
                if (c != null) if (c instanceof Atomic) {
                    r = g = b = ph * 0.5f;
                } else {
                    float belief = 0;

                    @Nullable Truth bt = c.beliefs().truth(now);
                    if (bt != null)
                        belief = bt.conf();


                    float goal = 0;
                    @Nullable Truth gt = c.goals().truth(now);
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

        d.onFrame(xx -> {

            //if (s.window.isVisible()) {
            tc.now = xx.time();
            tc.update();
            //}
        });

        return tc;
    }

    public static GridSurface budgetHistogram(NAR nar, int bins) {
        //new SpaceGraph().add(new Facial(
        return new GridSurface(VERTICAL,
                PanelSurface.of("Concept Priority Distribution (0..1)", new HistogramChart(nar, c -> {
                    if (c != null)
                        return c.pri();
                    return 0;
                }, bins, new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.1f))),
                PanelSurface.of("Concept Durability Distribution (0..1)", new HistogramChart(nar, c -> {
                    if (c != null)
                        return c.dur();
                    return 0;
                }, bins, new Color3f(0f, 0.25f, 0.5f), new Color3f(0.1f, 0.5f, 1f)))
        );
    }

    public static GridSurface conceptLinePlot(NAR nar, Iterable<? extends Termed> concepts, int plotHistory, FloatFunction<Termed> value) {

        //TODO make a lambda Grid constructor
        GridSurface grid = new GridSurface();
        List<Plot2D> plots = $.newArrayList();
        for (Termed t : concepts) {
            Plot2D p = new Plot2D(plotHistory, Plot2D.Line /*BarWave*/);
            p.add(t.toString(), () -> value.floatValueOf(t), 0f, 1f);
            grid.children.add(p);
            plots.add(p);
        }
        grid.layout();

        nar.onFrame(f -> {
            plots.forEach(Plot2D::update);
        });

        return grid;
    }

    public static GridSurface conceptLinePlot(NAR nar, Iterable<? extends Termed> concepts, int plotHistory) {

        //TODO make a lambda Grid constructor
        GridSurface grid = new GridSurface();
        List<Plot2D> plots = $.newArrayList();
        for (Termed t : concepts) {
            Plot2D p = new Plot2D(plotHistory, Plot2D.Line /*BarWave*/);
            p.setTitle(t.toString());
            p.add("P", () -> nar.conceptPriority(t), 0f, 1f);
            p.add("B", () -> nar.concept(t).beliefFreq(nar.time()), 0f, 1f);
            p.add("G", () -> nar.concept(t).goalFreq(nar.time()), 0f, 1f);
            grid.children.add(p);
            plots.add(p);
        }
        grid.layout();

        nar.onFrame(f -> {
            plots.forEach(Plot2D::update);
        });

        return grid;
    }


    public static GridSurface agentBudgetPlot(NAgent t, int history) {
        return conceptLinePlot(t.nar,
                Iterables.concat(t.actions, Lists.newArrayList(t.happy, t.joy)), history);
    }

    public static GridSurface emotionPlots(NAR nar, int plotHistory) {
//        Plot2D plot = new Plot2D(plotHistory, Plot2D.Line);
//        plot.add("Rwrd", reward);

        Plot2D plot1 = new Plot2D(plotHistory, Plot2D.Line);
        plot1.add("Conf", () -> nar.emotion.confident.getSum());

        Plot2D plot2 = new Plot2D(plotHistory, Plot2D.Line);
        plot2.add("Busy", () -> nar.emotion.busy.getSum());
        plot2.add("Lern", () -> nar.emotion.busy.getSum() - nar.emotion.frustration.getSum());

        Plot2D plot3 = new Plot2D(plotHistory, Plot2D.Line);
        plot3.add("Strs", () -> nar.emotion.stress.getSum());


        Plot2D plot4 = new Plot2D(plotHistory, Plot2D.Line);
        plot4.add("Hapy", () -> nar.emotion.happy.getSum());
        plot4.add("Sad", () -> nar.emotion.sad.getSum());

//                Plot2D plot4 = new Plot2D(plotHistory, Plot2D.Line);
//                plot4.add("Errr", ()->nar.emotion.errr.getSum());

        nar.onFrame(f -> {
            //plot.update();
            plot1.update();
            plot2.update();
            plot3.update();
            plot4.update();
        });

        return col(plot1, plot2, plot3, plot4);
    }
}
