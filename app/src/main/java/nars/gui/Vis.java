package nars.gui;

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
import org.jetbrains.annotations.Nullable;
import spacegraph.Facial;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.math.Color3f;
import spacegraph.obj.CrosshairSurface;
import spacegraph.obj.GridSurface;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static spacegraph.obj.GridSurface.VERTICAL;

/**
 * SpaceGraph-based visualization utilities for NAR analysis
 */
public class Vis {
    public static void newBeliefChartWindow(NAgent narenv, long window) {
        GridSurface chart = agentActions(narenv, window);
        new SpaceGraph().add(new Facial(chart).maximize()).show(800,600);
    }

    public static void newBeliefChartWindow(NAR nar, long window, Term... t) {
        GridSurface chart = agentActions(nar, Lists.newArrayList(t), window);
        new SpaceGraph().add(new Facial(chart).maximize()).show(800,600);
    }

    public static void newBeliefChartWindow(NAR nar, long window, List<? extends Termed> t) {
        GridSurface chart = agentActions(nar, t, window);
        new SpaceGraph().add(new Facial(chart).maximize()).show(800,600);
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
            actionTables.add( new BeliefTableChart(nar, c, btRange) );
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

        return new GridSurface(VERTICAL, s);
    }

    public static void show(Default d) {
        show(d, -1);
    }

    public static void show(Default d, int count) {
        BagChart<Concept> tc = concepts(d, count);
        SpaceGraph<VirtualTerminal> s = new SpaceGraph<>();


        s.show(1400, 800);


        s.add(new Facial(tc).maximize());
        s.add(new Facial(new CrosshairSurface(s)));
    }

    public static BagChart<Concept> concepts(final Default d, final int count) {
        BagChart<Concept> tc = new BagChart<Concept>(d.core.concepts, count) {
            @Override
            public void accept(BLink<Concept> x, ItemVis<BLink<Concept>> y) {
                float p = x.pri();
                float ph = 0.25f + 0.75f * p;

                float r, g, b;

                Concept c = x.get();
                if (c!=null) if (c instanceof Atomic) {
                    r = g = b = ph * 0.5f;
                } else {
                    float belief = 0;

                    @Nullable Truth bt = c.beliefs().truth(now);
                    if (bt!=null)
                        belief = bt.conf();


                    float goal = 0;
                    @Nullable Truth gt = c.goals().truth(now);
                    if (gt!=null)
                        goal = gt.conf();

                    if (belief > 0 || goal > 0) {
                        r = 0;
                        g = 0.25f + 0.75f * belief;
                        b = 0.25f + 0.75f * goal;
                    } /*else if (c.hasQuestions() || c.hasQuests()) {
                        r = 1; //yellow
                        g = 1/2;
                        b = 0;
                    } else*/ {
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
                    new HistogramChart(nar, c -> {
                        if (c!=null)
                            return c.pri();
                        return 0;
                    }, bins, new Color3f(0.5f, 0.25f, 0f), new Color3f(1f, 0.5f, 0.1f)),
                    new HistogramChart(nar, c -> {
                        if (c!=null)
                            return c.dur();
                        return 0;
                    }, bins, new Color3f(0f, 0.25f, 0.5f), new Color3f(0.1f, 0.5f, 1f))
                );
          //  ).maximize()).show(800,600);
    }
}
