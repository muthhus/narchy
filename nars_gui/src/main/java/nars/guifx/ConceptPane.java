package nars.guifx;

import com.gs.collections.impl.set.mutable.UnifiedSet;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import nars.NAR;
import nars.bag.Bag;
import nars.budget.UnitBudget;
import nars.concept.Concept;
import nars.concept.util.BeliefTable;
import nars.concept.util.DefaultBeliefTable;
import nars.guifx.chart.Plot2D;
import nars.guifx.demo.SubButton;
import nars.guifx.graph2.TermEdge;
import nars.guifx.graph2.TermNode;
import nars.guifx.graph2.impl.BlurCanvasEdgeRenderer;
import nars.guifx.graph2.scene.DefaultNodeVis;
import nars.guifx.graph2.source.ConceptNeighborhoodSource;
import nars.guifx.graph2.source.DefaultGrapher;
import nars.guifx.graph3.SpaceNet;
import nars.guifx.graph3.Xform;
import nars.guifx.util.ColorArray;
import nars.guifx.util.ColorMatrix;
import nars.guifx.util.SimpleMenuItem;
import nars.nar.Default;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.util.event.FrameReaction;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

import static javafx.application.Platform.runLater;


/**
 * Created by me on 8/10/15.
 */
public class ConceptPane extends BorderPane implements ChangeListener {

    private final NAR nar;
    private final BeliefTablePane tasks;
    private final Plot2D budgetGraph;
    private final Term term;
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

    public static class BagView<X> extends FlowPane implements Runnable {

        final Map<X,Node> componentCache = new WeakHashMap<>();
        private final Bag<X> bag;
        private final Function<X, Node> builder;
        final List<Node> pending = new ArrayList();
        final AtomicBoolean queued = new AtomicBoolean();

        public BagView(Bag<X> bag, Function<X,Node> builder) {
            this.bag = bag;
            this.builder = builder;
            frame();
        }

        Node getNode(X n) {
            Node existing = componentCache.get(n);
            if (existing == null) {
                componentCache.put(n, existing = builder.apply(n));
            }
            return existing;
        }

        public void frame() {
            synchronized (pending) {
                pending.clear();
                bag.forEach(b -> pending.add(getNode(b.get())));
            }

            if (!getChildren().equals(pending) && queued.compareAndSet(false, true)) {
                runLater(this);
            }
        }

        @Override
        public void run() {
            synchronized (pending) {
                getChildren().setAll(pending);
                queued.set(false);
            }

            getChildren().stream().filter(n -> n instanceof Runnable).forEach(n -> ((Runnable) n).run());
        }
    }

    public ConceptPane(NAR nar, Termed cconceptTerm) {
        super();



        Term conceptTerm = cconceptTerm.term();
        this.term = conceptTerm;
        this.nar = nar;


        tasks = new BeliefTablePane();

        budgetGraph = new Plot2D(Plot2D.BarWave, 128, 64);
        budgetGraph.add("Pri", () -> {
            Concept cc = currentConcept;
            //if (cc == null)
              //  throw new RuntimeException("missing concept");
            return cc != null ? nar.conceptPriority(cc, 0) : 0;
        }, 0, 1f);


        Button activateButton = new Button("+");
        activateButton.setOnMouseClicked(e->{
            nar.runLater(()->{
                nar.conceptualize(conceptTerm, new UnitBudget(1f, 0.75f, 0.75f), 1f);
            });
        });
        Button goalButton = new Button("!");
        activateButton.setOnMouseClicked(e->{
            nar.runLater(()->{
                nar.input(new MutableTask(conceptTerm, '!').present(nar.memory).log("GUI"));
            });
        });

        Menu conceptMenu = new Menu(conceptTerm.toString());
        conceptMenu.getItems().add(new SimpleMenuItem("Dump",()->{
            nar.runLater(()-> {

                System.out.println(conceptTerm + ": " + nar.conceptPriority(conceptTerm, Float.NaN));
                nar.concept(conceptTerm).print();

                //getChildren().clear();
                budgetGraph.requestLayout();
                //getChildren().addAll( tasks, budgetGraph );
            });
        }));

        setTop(new FlowPane(new MenuBar(conceptMenu), activateButton, goalButton));




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
//        //TilePane links = new TilePane(links.content);
//
////        Label beliefs = new Label("Beliefs diagram");
////        Label goals = new Label("Goals diagram");
////        Label questions = new Label("Questions diagram");
//
////        SplitPane links = new SplitPane(
////                scrolled(termLinkView = new BagView<>(c.getTermLinks(),
////                                (t) -> new ItemButton(t, Object::toString,
////                                        (i) -> {
////
////                                        }
////
////                                )
////                        )
////                ),
////                scrolled(taskLinkView = new BagView<>(c.getTaskLinks(),
////                        (t) -> new TaskLabel(t.getTask(), null)
////
////                ))
////        );
////        links.setOrientation(Orientation.VERTICAL);
//        //links.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);

//        BorderPane links = new BorderPane();
//        setCenter(new SplitPane(new BorderPane(links), tasks.content));






        runLater(()->{

            setCenter(new VBox(
                    budgetGraph,
                    tasks
            ));
            layout();


            //setCenter(new ConceptNeighborhoodGraph(nar, concept));

        /*Label controls = new Label("Control Panel");
        setBottom(controls);*/

            visibleProperty().addListener(this);
            changed(null,null,null);
        });


    }

    public static class ConceptNeighborhoodGraph extends BorderPane {

        public ConceptNeighborhoodGraph(NAR n, Concept c) {
            DefaultGrapher g = new DefaultGrapher(

                    //new ConceptsSource(n),
                    new ConceptNeighborhoodSource(n,
                            c
                    ),

                    new DefaultNodeVis() {

                        @Override
                        public TermNode newNode(Termed term) {
                            return new LabeledCanvasNode(term, 32, e-> { }, e-> { }) {
                                @Override
                                protected Node newBase() {
                                    SubButton s = SubButton.make(n, (Concept) term);

                                    s.setScaleX(0.02f);
                                    s.setScaleY(0.02f);
                                    s.shade(1f);

                                    s.setManaged(false);
                                    s.setCenterShape(false);

                                    return s;
                                }
                            };
                            //return new HexTermNode(term.term(), 32, e-> { }, e-> { });
                            //return super.newNode(term);
                        }
                    },
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
            setCenter(g);
        }
    }



    protected void frame(long now) {
        //tasks.frame();
        /*taskLinkView.frame();
        termLinkView.frame();*/

        Concept c = currentConcept = nar.concept(this.term);

        budgetGraph.update();

        tasks.frame(c, now);

    }

    @Override
    public synchronized void changed(ObservableValue observable, Object oldValue, Object newValue) {
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
            NARfx.newConceptWindow(n, n.concept("<a-->b>"));
//            s.setScene(new ConsolePanel(n, n.concept("<a-->b>")),
//                    800,600);

            new Thread(() -> n.loop(10)).start();
        });




    }

    public static class BeliefTablePane extends HBox {
        final Canvas eternal, temporal;

        final static ColorMatrix beliefColors = new ColorMatrix(8,8,(f,c)->
            new Color(0.6f, 0.25f, 1f, 0.25f + 0.6f * c)
        );
        final static ColorMatrix goalColors = new ColorMatrix(8,8,(f,c)->
            new Color(0.5f, 1f, 0f, 0.25f + 0.6f * c)
        );

        //horizontal block
        final static TaskRenderer beliefRenderer = new TaskRenderer() {
            @Override public void renderTask(GraphicsContext ge, float f, float c, float w, float h, float x, float y) {
                ge.setFill(beliefColors.get(f, c));
                ge.fillRect(x-w/2,y-h/4,w,h/2);
            }
        };
        //vertical block
        final static TaskRenderer goalRenderer = new TaskRenderer() {
            @Override public void renderTask(GraphicsContext ge, float f, float c, float w, float h, float x, float y) {
                ge.setFill(goalColors.get(f, c));
                ge.fillRect(x-w/4,y-h/2,w/2,h);
            }
        };

        public BeliefTablePane() {
            super();

            getChildren().addAll(
                eternal = new Canvas(75, 75),
                temporal = new Canvas(200, 75)
            );

            //setCenter(temporal);
            //setLeft(eternal);
        }

        public void frame(Concept concept, long now) {

            //redraw
            GraphicsContext ge = eternal.getGraphicsContext2D();
            float gew = (float) ge.getCanvas().getWidth();
            float geh = (float) ge.getCanvas().getHeight();
            ge.clearRect(0, 0, gew, geh);

            GraphicsContext te = temporal.getGraphicsContext2D();
            float tew = (float) te.getCanvas().getWidth();
            float teh = (float) te.getCanvas().getHeight();
            te.clearRect(0, 0, tew, teh);

            if (concept == null) return;

            //compute bounds from combined min/max of beliefs and goals so they align correctly
            long minT = Long.MAX_VALUE;
            long maxT = Long.MIN_VALUE;
            {

                if (concept.hasBeliefs()) {
                    minT = ((DefaultBeliefTable) concept.beliefs()).getMinT();
                    maxT = ((DefaultBeliefTable) concept.beliefs()).getMaxT();
                }
                if (concept.hasGoals()) {
                    minT = Math.min(minT, ((DefaultBeliefTable) concept.goals()).getMinT());
                    maxT = Math.max(maxT, ((DefaultBeliefTable) concept.goals()).getMaxT());
                }
            }


            try {
                if (concept.hasBeliefs())
                    renderTable(minT, maxT, now, ge, gew, geh, te, tew, teh, concept.beliefs(), beliefRenderer);
                if (concept.hasGoals())
                    renderTable(minT, maxT, now, ge, gew, geh, te, tew, teh, concept.goals(), goalRenderer);
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

        private void renderTable(long minT, long maxT, long now, GraphicsContext ge, float gew, float geh, GraphicsContext te, float tew, float teh, BeliefTable table, TaskRenderer r) {
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
            for (Task t : table) {
                float f = t.freq();
                float cc = t.conf();
                float eh, x;
                GraphicsContext g;
                if (t.isEternal()) {
                    eh = geh;
                    x = b + (gew - b - w) * cc;
                    g = ge;
                } else  {
                    eh = teh;
                    float o = t.occurrence();
                    x = xTime(tew, b, minT, maxT, o, w);
                    g = te;
                }
                float y = b + (eh - b - h) * (1 - f);
                r.renderTask(g, f, cc, w, h, x, y);

            }
        }

        @FunctionalInterface interface TaskRenderer {
            void renderTask(GraphicsContext ge, float f, float c, float w, float h, float x, float y);
        }


        private float xTime(float tew, float b, float minT, float maxT, float o, float w) {
            float p = minT == maxT ? 0.5f : (o - minT) / (maxT - minT);
            return b + p * (tew-b-w);
        }
    }
}
