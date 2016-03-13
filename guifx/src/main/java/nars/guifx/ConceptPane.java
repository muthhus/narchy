package nars.guifx;

import com.gs.collections.impl.set.mutable.UnifiedSet;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import nars.Global;
import nars.NAR;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.budget.UnitBudget;
import nars.concept.Concept;
import nars.guifx.chart.Plot2D;
import nars.guifx.graph2.TermEdge;
import nars.guifx.graph2.impl.BlurCanvasEdgeRenderer;
import nars.guifx.graph2.impl.HexButtonVis;
import nars.guifx.graph2.source.ConceptNeighborhoodSource;
import nars.guifx.graph2.source.DefaultGrapher;
import nars.guifx.graph3.SpaceNet;
import nars.guifx.graph3.Xform;
import nars.guifx.nars.NARActionButton;
import nars.guifx.nars.TaskButton;
import nars.guifx.util.ColorArray;
import nars.guifx.util.ColorMatrix;
import nars.guifx.util.SimpleMenuItem;
import nars.nar.Default;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.TruthWave;
import nars.util.event.FrameReaction;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toCollection;
import static javafx.application.Platform.runLater;
import static nars.$.neg;
import static nars.nal.Tense.ETERNAL;


/**
 * Created by me on 8/10/15.
 */
public class ConceptPane extends BorderPane implements ChangeListener {

    private final NAR nar;
    private final BeliefTablePane beliefChart;
    private final Plot2D budgetGraph;
    private final Term term;
    private final BagView<Termed> termlinkView;
    private final BagView<Task> tasklinkView;

    //    private final Scatter3D tasks;
//    private final BagView<Task> taskLinkView;
//    private final BagView<Term> termLinkView;
    private FrameReaction reaction;

    private transient Concept currentConcept; //caches current value, updated on each frame

    public abstract static class Scatter3D<X> extends SpaceNet {

        final ColorArray ca = new ColorArray(32, Color.BLUE, Color.RED);

        @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
        public Scatter3D() {


            frame();
            setPickOnBounds(true);
            setMouseTransparent(false);
        }

        @Override
        public Xform getRoot() {
            return new Xform();
        }



        public class DataPoint extends Group {

            public final X x;
            private final Box shape;
            private final PhongMaterial mat;
            private Color color = Color.WHITE;

            public DataPoint(X tl) {

                shape = new Box(0.8, 0.8, 0.8);
                mat = new PhongMaterial(color);
                shape.setMaterial(mat);
                //shape.onMouseEnteredProperty()

                getChildren().add(shape);

                x = tl;

                frame();

                shape.setOnMouseEntered(e -> {
                    X x = ((DataPoint)e.getSource()).x;
                    System.out.println("enter " + x);
                });
                shape.setOnMouseClicked(e -> {
                    X x = ((DataPoint)e.getSource()).x;
                    System.out.println("click " + x);
                });
                shape.setOnMouseExited(e -> {
                    X x = ((DataPoint)e.getSource()).x;
                    System.out.println("exit " + x);
                });

            }

            public void setColor(Color nextColor) {
                color = nextColor;
            }

            public void frame() {
                mat.setDiffuseColor(color);
            }



        }


        abstract Iterable<X>[] get();
        protected abstract void update(X tl, double[] position, double[] size, Consumer<Color> color);

        //ca.get(x.getPriority())
        //concept.getTermLinks()

        final Map<X, DataPoint> linkShape = new LinkedHashMap();

        final Set<X> dead = new UnifiedSet();
        double n;

        float spaceScale = 10;

        public void frame() {

            //if (!isVisible()) return;

            dead.addAll(linkShape.keySet());

            n = 0;

            double[] d = new double[3];
            double[] s = new double[3];

            List<DataPoint> toAdd = new ArrayList();


            Iterable<X>[] collects = get();
            if (collects != null) {
                for (Iterable<X> ii : collects) {
                    if (ii == null) continue;
                    ii.forEach(tl -> {

                        dead.remove(tl);

                        DataPoint b = linkShape.get(tl);
                        if (b == null) {
                            b = new DataPoint(tl);
                            linkShape.put(tl, b);

                            DataPoint _b = b;
                            update(tl, d, s, _b::setColor);
                            b.setTranslateX(d[0] * spaceScale);
                            b.setTranslateY(d[1] * spaceScale);
                            b.setTranslateZ(d[2] * spaceScale);
                            b.setScaleX(s[0]);
                            b.setScaleY(s[1]);
                            b.setScaleZ(s[2]);

                            toAdd.add(b);
                        }

                        b.frame();

                    });
                }
            }

            linkShape.keySet().removeAll(dead);
            Object[] deads = dead.toArray(new Object[dead.size()]);

            dead.clear();

            runLater(() -> {
                getChildren().addAll(toAdd);
                toAdd.clear();

                for (Object x : deads) {
                    DataPoint shape = linkShape.remove(x);
                    if (shape!=null && shape.getParent()!=null) {
                        getChildren().remove(shape);
                    }
                }
            });

        }
    }

    public static class BagView<X> extends VBox /* FlowPane */ implements Runnable {

        final Map<BLink<X>,Node> componentCache = new WeakHashMap<>();
        private final Supplier<Bag<X>> bag;
        private final Function<BLink<X>, Node> builder;
        final List<BLink<X>> pending = Global.newArrayList();
        final AtomicBoolean queued = new AtomicBoolean();
        private final int limit;


        public BagView(Supplier<Bag<X>> bag, Function<BLink<X>,Node> builder, int limit) {
            this.bag = bag;
            this.builder = builder;
            this.limit = limit;

            setCache(true);

            update();
        }

        Node getNode(BLink<X> n) {
            Node existing = componentCache.computeIfAbsent(n, builder::apply);
//            Node existing = componentCache.get(n);
//            if (existing == null) {
//                componentCache.put(n, existing = builder.apply(n));
//            } else {
                //since it will already have been run as part of the builder
                if (existing instanceof Runnable)
                    ((Runnable) existing).run();
//            }
            return existing;
        }

        public void update() {

            Bag<X> bLinks = bag.get();
            if (bLinks == null) {
                return;
            }

            if (!queued.compareAndSet(false, true)) {
                List<BLink<X>> p = this.pending;
                p.clear();
                bLinks.forEach(limit, p::add);

                //if (!getChildren().equals(p)) {
                    runLater(this);
                /*} else {
                    queued.set(false);
                }*/
            }

        }

        @Override
        public void run() {
            //synchronized (pending) {
            if (!queued.get())
                return;

            ObservableList<Node> ch = getChildren();
            ch.clear();
            pending.stream().map(this::getNode).collect(toCollection(()->ch));

//            getChildren().forEach(n -> {
//                if (n instanceof Runnable)
//                    ((Runnable) n).run();
//            });

            queued.set(false);

        }
    }

    public ConceptPane(NAR nar, Termed cconceptTerm) {
        super();



        Term term = cconceptTerm.term();
        this.term = term;
        this.nar = nar;


        beliefChart = new BeliefTablePane();

        int budgetGraphWidth = 0;
        int budgetGraphHeight = 32;

        budgetGraph = new Plot2D(Plot2D.BarWave, 128, budgetGraphWidth, budgetGraphHeight);
        budgetGraph.add("Pri", () -> {
            Concept cc = currentConcept;
            //if (cc == null)
              //  throw new RuntimeException("missing concept");
            return cc != null ? nar.conceptPriority(cc, 0) : 0;
        }, 0, 1f);


        Button activateButton = new NARActionButton(nar, "+", (n) -> n.conceptualize(term, new UnitBudget(1f, 0.75f, 0.75f), 1f));
        Button yesGoalButton = new NARActionButton(nar, "+!", (n) -> n.input(new MutableTask(term, '!').present(nar).log("GUI Goal")));
        Button noGoalButton = new NARActionButton(nar, "-!", (n) -> n.input(new MutableTask(term, '!').truth(0f, (nar).getDefaultConfidence('!')).present(nar).log("GUI Goal")));
        Button trueButton = new NARActionButton(nar, "T", (n) -> n.input(new MutableTask(term, '.').present(nar).log("GUI True")));
        Button falseButton = new NARActionButton(nar, "F", (n) -> n.input(new MutableTask(neg(term), '.').present(nar).log("GUI False")));
        Button isTrueButton = new NARActionButton(nar, "?", (n) -> n.input(new MutableTask(term, '?').present(nar).log("GUI Question")));

        if (!(term instanceof Compound)) {
            trueButton.setVisible(false); //TODO dont create these task buttons in the first place
            falseButton.setVisible(false);
            yesGoalButton.setVisible(false);
            noGoalButton.setVisible(false);
            isTrueButton.setVisible(false);
        }

        Menu conceptMenu = new Menu(term.toString());
        conceptMenu.getItems().add(new SimpleMenuItem("Dump",()-> nar.runLater(()-> {

            System.out.println(term + ": " + nar.conceptPriority(term, Float.NaN));
            nar.concept(term).print();

        })));







//        Label beliefs = new Label("Beliefs diagram");
//        Label goals = new Label("Goals diagram");
//        Label questions = new Label("Questions diagram");



//        //links.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);

//        BorderPane links = new BorderPane();
//        setCenter(new SplitPane(new BorderPane(links), tasks.content));

        int maxDisplayedBagItems = 16;
        
        termlinkView = new BagView<>(
                () -> currentConcept != null ? currentConcept.termlinks() : null,
                //(t) -> SubButton.make(nar, t.term())
                (t) -> new TaskButton(nar, t),
                maxDisplayedBagItems
        );
        tasklinkView = new BagView<>(
                () -> currentConcept != null ? currentConcept.tasklinks() : null,
                (t) -> new TaskButton(nar, t),
                maxDisplayedBagItems
        );

//        GridPane bags = new GridPane();
//        ColumnConstraints column1 = new ColumnConstraints();
//        column1.setPercentWidth(50);
//        ColumnConstraints column2 = new ColumnConstraints();
//        column2.setPercentWidth(50);
//        bags.getColumnConstraints().addAll(column1, column2); // each get 50% of width
//        bags.addRow(0,termlinkView, tasklinkView);
//        bags.maxWidth(Double.MAX_VALUE);

        //BorderPane bbb = new BorderPane();
        FlowPane menu = new FlowPane(new MenuBar(conceptMenu), activateButton, trueButton, falseButton, yesGoalButton, noGoalButton, isTrueButton);

        beliefChart.setMouseTransparent(true);
        beliefChart.setPickOnBounds(false);

        budgetGraph.setMouseTransparent(true);
        budgetGraph.setPickOnBounds(false);




        //vb.setMouseTransparent(true);






        budgetGraph.setAlignment(Pos.BOTTOM_CENTER);
        beliefChart.setAlignment(Pos.TOP_RIGHT);
        termlinkView.setAlignment(Pos.CENTER_LEFT);
        tasklinkView.setAlignment(Pos.CENTER_RIGHT);

        DefaultGrapher neighborhood = ConceptNeighborhoodGraph(nar, cconceptTerm);

        neighborhood.maxWidth(Double.MAX_VALUE);
        neighborhood.maxHeight(Double.MAX_VALUE);

        maxWidth(Double.MAX_VALUE);
        maxHeight(Double.MAX_VALUE);

        menu.setAlignment(Pos.TOP_LEFT);

        //setCenter(bbb);


        StackPane content = new StackPane(
                neighborhood,

                //overlay
                budgetGraph, beliefChart, termlinkView, tasklinkView
        );
        for (Node x : content.getChildren()) {
            if (x == neighborhood) continue;
            x.setMouseTransparent(true);
            x.setPickOnBounds(false);
            x.setOpacity(0.8f);
        }

        setTop(menu);
        setCenter(content);

        runLater(()->{
            visibleProperty().addListener(this);
            changed(null,null,null);
        });


    }

    public static DefaultGrapher ConceptNeighborhoodGraph(NAR n, Termed c) {
            DefaultGrapher g = new DefaultGrapher(n,

                    //new ConceptsSource(n),
                    new ConceptNeighborhoodSource(n, c),

                    new HexButtonVis(n),
                    //new DefaultNodeVis.HexTermNode(),

                    (A, B) -> {
                        return new TermEdge(A, B) {
                            @Override
                            public double getWeight() {
                                //return ((Concept)A.term).getPriority();
                                return pri;
                            }
                        };
                        //return $.pro(A.getTerm(), B.getTerm());
                    },

                    new BlurCanvasEdgeRenderer()
            );
            //setCenter(g);
        return g;
        }

//        private static class SubButtonGraphNode extends DefaultNodeVis.LabeledCanvasNode {
//            private final NAR n;
//
//            public SubButtonGraphNode(NAR n, Termed term) {
//                super(n, term, 32, null, null);
//                this.n = n;
//            }
//
//            @Override
//            protected Node newBase() {
//                SubButton s = SubButton.make(n, (Concept) term);
//
//                s.setScaleX(0.02f);
//                s.setScaleY(0.02f);
//                s.shade(1f);
//
//                s.setManaged(false);
//                s.setCenterShape(false);
//
//                return s;
//            }
//        }




    protected void frame(long now) {
        //tasks.frame();
        /*taskLinkView.frame();
        termLinkView.frame();*/

        Concept c = currentConcept = nar.concept(this.term);

        budgetGraph.update();

        beliefChart.frame(c, now);

        termlinkView.update();
        tasklinkView.update();

    }

    @Override
    public void changed(ObservableValue observable, Object oldValue, Object newValue) {

        if (isVisible()) {
            if (reaction==null) {
                reaction = new FrameReaction(nar) {
                    @Override
                    public void onFrame() {
                        frame(nar.time());
                    }
                };
            }
        }
        else {
            if (reaction!=null) {
                reaction.off();
                reaction = null;
            }
        }
    }


    /* test example */
    public static void main(String[] args) {
        NAR n = new Default();
        n.input("<a-->b>. <b-->c>. <c-->a>. :|:");
        n.input("<a --> b>!");
        n.input("<a --> b>?");
        n.input("<a --> b>. %0.10;0.95%");
        n.input("<a --> b>! %0.35%");
        n.input("<a --> b>. :/: %0.75%");

        n.run(16);

        NARfx.run((a,s) -> {
            NARfx.newWindow(n, n.concept("<a-->b>"));
//            s.setScene(new ConsolePanel(n, n.concept("<a-->b>")),
//                    800,600);

            new Thread(() -> n.loop(10)).start();
        });




    }

    public static class BeliefTablePane extends HBox implements Runnable {
        final Canvas eternal, temporal;
        final TruthWave beliefs = new TruthWave(0), goals = new TruthWave(0);

        final static ColorMatrix beliefColors = new ColorMatrix(8,8,(f,c)->
            new Color(0.6f+0.38f*c, 0.2f, 1f, 0.15f + 0.8f * c)
        );
        final static ColorMatrix goalColors = new ColorMatrix(8,8,(f,c)->
            new Color(0.2f + 0.4f*c, 1f, 0f, 0.15f + 0.8f * c)
        );

        //horizontal block
        final static TaskRenderer beliefRenderer = (ge, f, c, w, h, x, y) -> {
            ge.setFill(beliefColors.get(f, c));
            ge.fillRect(x-w/2,y-h/4,w,h/2);
        };
        //vertical block
        final static TaskRenderer goalRenderer = (ge, f, c, w, h, x, y) -> {
            ge.setFill(goalColors.get(f, c));
            ge.fillRect(x-w/4,y-h/2,w/2,h);
        };
        private long now;

        public BeliefTablePane() {
            super();

            getChildren().addAll(
                eternal = new Canvas(75, 75),
                temporal = new Canvas(200, 75)
            );

            //setCenter(temporal);
            //setLeft(eternal);
        }

        /** redraw */
        @Override public void run() {
            if (!redraw.compareAndSet(false, true)) {
                return;
            }

            //redraw
            GraphicsContext ge = eternal.getGraphicsContext2D();
            float gew = (float) ge.getCanvas().getWidth();
            float geh = (float) ge.getCanvas().getHeight();
            ge.clearRect(0, 0, gew, geh);

            GraphicsContext te = temporal.getGraphicsContext2D();
            float tew = (float) te.getCanvas().getWidth();
            float teh = (float) te.getCanvas().getHeight();
            te.clearRect(0, 0, tew, teh);

            //compute bounds from combined min/max of beliefs and goals so they align correctly
            long minT = Long.MAX_VALUE;
            long maxT = Long.MIN_VALUE;
            {

                if (!beliefs.isEmpty()) {
                    minT = beliefs.start();
                    maxT = beliefs.end();
                }
                if (!goals.isEmpty()) {

                    long min = goals.start();
                    if (min != ETERNAL) {
                        minT = Math.min(min, minT);
                        maxT = Math.max(goals.end(), maxT);
                    }

                }

            }


            try {
                if (!beliefs.isEmpty())
                    renderTable(minT, maxT, now, ge, gew, geh, te, tew, teh, beliefs, beliefRenderer);
                if (!goals.isEmpty())
                    renderTable(minT, maxT, now, ge, gew, geh, te, tew, teh, goals, goalRenderer);
            }
            catch (Throwable t) {
                //HACK
                t.printStackTrace();
            }


            //borders
            {
                ge.setStroke(Color.WHITE);
                te.setStroke(Color.WHITE);
                ge.strokeRect(0, 0, gew, geh);
                te.strokeRect(0, 0, tew, teh);
                ge.setStroke(null);
                te.setStroke(null);
            }


        }

        final AtomicBoolean redraw = new AtomicBoolean(true);

        public void frame(Concept concept, long now) {

            if (concept == null) return;

            this.now = now;
            if (concept.hasBeliefs()) beliefs.set(concept.beliefs());
            if (concept.hasGoals()) goals.set(concept.goals());


            if (redraw.compareAndSet(true, false)) {
                runLater(this);
            }
        }

        private void renderTable(long minT, long maxT, long now, GraphicsContext ge, float gew, float geh, GraphicsContext te, float tew, float teh, TruthWave table, TaskRenderer r) {
            float b = 4; //border

            //Present axis line
            if ((now <= maxT) && (now >= minT)) {
                float nowLineWidth = 3;
                float nx = xTime(tew, b, minT, maxT, now, nowLineWidth);
                te.setFill(Color.WHITE);
                te.fillRect(nx - nowLineWidth / 2f, 0, nowLineWidth, teh);
            }

            float w = 10;
            float h = 10;
            table.forEach((f,cc,o)->{

                boolean eternal = !Float.isFinite(o);
                float eh, x;
                GraphicsContext g;
                if (eternal) {
                    eh = geh;
                    x = b + (gew - b - w) * cc;
                    g = ge;
                } else  {
                    eh = teh;
                    x = xTime(tew, b, minT, maxT, o, w);
                    g = te;
                }
                float y = b + (eh - b - h) * (1 - f);
                r.renderTask(g, f, cc, w, h, x, y);

            });
        }

        @FunctionalInterface interface TaskRenderer {
            void renderTask(GraphicsContext ge, float f, float c, float w, float h, float x, float y);
        }


        private float xTime(float tew, float b, float minT, float maxT, float o, float w) {
            float p = minT == maxT ? 0.5f : (o - minT) / (maxT - minT);
            return b + p * (tew-b-w);
        }
    }



//        //Label termlinks = new Label("Termlinks diagram");
//        //Label tasklinks = new Label("Tasklnks diagram");
//        tasks = new Scatter3D<Task>() {
//
//            @Override
//            Iterable<Task>[] get() {
//                return new Iterable[] { c.getBeliefs(),
//                        c.getGoals(),
//                        c.getQuestions(),
//                        c.getQuests() };
//            }
//
//            @Override
//            protected void update(Task tl, double[] position, double[] size, Consumer<Color> color) {
//
//                System.out.println("update: " + tl);
//                if (tl.isQuestOrQuestion()) {
//                    position[0] = -1;
//                    position[1] = tl.getBestSolution() != null ? tl.getBestSolution().getConfidence() : 0;
//                }
//                else {
//                    Truth t = tl.getTruth();
//                    position[0] = t.getFrequency();
//                    position[1] = t.getConfidence();
//                }
//                float pri = tl.getPriority();
//                position[2] = pri;
//
//                size[0] = size[1] = size[2] = 0.5f + pri;
//                color.accept(ca.get(pri));
//            }
//        };
//



}
