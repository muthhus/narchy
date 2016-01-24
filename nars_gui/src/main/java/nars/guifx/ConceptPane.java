package nars.guifx;

import com.gs.collections.impl.set.mutable.UnifiedSet;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import nars.Global;
import nars.NAR;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.concept.util.DefaultBeliefTable;
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
import nars.nar.Default;
import nars.task.Task;
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
    //    private final Scatter3D tasks;
//    private final BagView<Task> taskLinkView;
//    private final BagView<Term> termLinkView;
    private FrameReaction reaction;

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
                Platform.runLater(this);
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

    public ConceptPane(NAR nar, Concept c) {
        super();

        //concept = c;
        this.nar = nar;


        setTop(new Label(c.toString()));



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

        tasks = new BeliefTablePane(c);
        tasks.maxWidth(Double.MAX_VALUE);
        tasks.maxHeight(Double.MAX_VALUE);
        setCenter(tasks);

        //setCenter(new ConceptNeighborhoodGraph(nar, concept));

        /*Label controls = new Label("Control Panel");
        setBottom(controls);*/

        visibleProperty().addListener(this);
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

        tasks.frame(now);

    }

    @Override
    public void changed(ObservableValue observable, Object oldValue, Object newValue) {
        if (isVisible()) {
            reaction = new FrameReaction(nar) {
                @Override public void onFrame() {
                    frame(nar.time());
                }
            };
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
        n.input("<a-->b>. <b-->c>. <c-->a>.");
        n.input("<a --> b>!");
        n.input("<a --> b>?");
        n.input("<a --> b>. %0.10;0.95%");
        n.input("<a --> b>! %0.35%");
        n.input("<a --> b>. %0.75%");

        n.run(516);

        NARfx.run((a,s) -> {
            NARfx.newWindow(n, n.concept("<a-->b>"));
//            s.setScene(new ConsolePanel(n, n.concept("<a-->b>")),
//                    800,600);

            new Thread(() -> n.loop(10)).start();
        });




    }

    public static class BeliefTablePane extends BorderPane {
        final Canvas eternal, temporal;
        private final Concept concept;

        public BeliefTablePane(Concept c) {
            super();
            this.concept = c;
            eternal = new Canvas(300, 400);
            eternal.maxWidth(300);
            eternal.maxHeight(300);
            temporal = new Canvas(800, 400);
            temporal.maxWidth(500);
            temporal.maxHeight(300);

            setCenter(temporal);
            setLeft(eternal);
        }

        public void frame(long now) {
            //redraw
            GraphicsContext ge = eternal.getGraphicsContext2D();
            float gew = (float) ge.getCanvas().getWidth();
            float geh = (float) ge.getCanvas().getHeight();
            ge.clearRect(0,0, gew, geh);

            GraphicsContext te = temporal.getGraphicsContext2D();
            float tew = (float) te.getCanvas().getWidth();
            float teh = (float) te.getCanvas().getHeight();
            te.clearRect(0,0, tew, teh);

            float b = 10;

            List<Task> tt = Global.newArrayList();
            if (!(concept.getBeliefs() instanceof DefaultBeliefTable)) return;

            float minT = ((DefaultBeliefTable) concept.getBeliefs()).getMinT();
            float maxT = ((DefaultBeliefTable) concept.getBeliefs()).getMaxT();

            //Present axis line
            float nowLineWidth = 5;
            float nx = xTime(tew, b, minT, maxT, now, nowLineWidth);
            te.setFill(Color.WHITE);
            te.fillRect(nx-nowLineWidth/2f, 0, nowLineWidth, teh);


            for (Task t : concept.getBeliefs()) {
                if (t.isEternal() && t.truth()!=null) {
                    float f = t.freq();
                    float c = t.conf();
                    float w = 20;
                    float h = 20;
                    float x = b + (gew-2*b-w) * c;
                    float y = b + (geh-2*b-h) * (1-f);
                    float rank = c; //concept.getBeliefs().rankEternal(t);
                    ge.setFill(new Color( f,  c, 1f, rank));
                    ge.fillRect(x-w/2,y-h/2,w,h);
                } else if (!t.isEternal() && t.truth()!=null) {
                    float f = t.freq();
                    float cc = t.conf();
                    float o = t.occurrence();
                    float w = 15;
                    float h = 15;
                    float x = xTime(tew, b, minT, maxT, o, w);
                    float y = b + (teh-2*b-h) * (1-f);
                    float rank = cc; //BeliefTable.rankTemporal(t, now, now);
                    te.setFill(new Color( f,  cc, 1f, rank));
                    te.fillRect(x-w/2,y-h/2,w,h);
                    tt.add(t);
                }

            }

        }

        private float xTime(float tew, float b, float minT, float maxT, float o, float w) {
            return b + ((o - minT) / (maxT-minT)) * (tew-2*b-w);
        }
    }
}
