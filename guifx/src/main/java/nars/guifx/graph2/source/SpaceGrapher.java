package nars.guifx.graph2.source;


import com.gs.collections.impl.map.mutable.UnifiedMap;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import nars.NAR;
import nars.guifx.Spacegraph;
import nars.guifx.graph2.*;
import nars.guifx.graph2.impl.CanvasEdgeRenderer;
import nars.guifx.graph2.layout.IterativeLayout;
import nars.guifx.graph2.layout.None;
import nars.guifx.util.Animate;
import nars.term.Term;
import nars.term.Termed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static javafx.application.Platform.runLater;

/**
 * Created by me on 8/6/15.
 */
public class SpaceGrapher extends Spacegraph {

    public static final Logger logger = LoggerFactory.getLogger(SpaceGrapher.class);

    final Map<Termed, TermNode> terms = new UnifiedMap();
    //new WeakValueHashMap<>();

    static final int defaultFramePeriodMS = 30; //~60hz/2

    public final SimpleObjectProperty<EdgeRenderer<TermEdge>> edgeRenderer = new SimpleObjectProperty<>();

    public final SimpleObjectProperty<IterativeLayout<TermNode>> layout = new SimpleObjectProperty<>();
    public static final IterativeLayout nullLayout = new None();


    //public final SimpleIntegerProperty maxNodes;
    public final SimpleObjectProperty<GraphSource> source = new SimpleObjectProperty<>();
    public final BiFunction<TermNode, TermNode, TermEdge> edgeVis;

    /**
     * graph source update period MS
     */


    private Animate animator; //TODO atomic reference


    //static final Random rng = new XORShiftRandom();


    public final SimpleObjectProperty<NodeVis> nodeVis = new SimpleObjectProperty<>();
    public TermNode[] displayed = new TermNode[0];
    private Set<TermNode> prevActive;

//    /**
//     * produces a spacegraph instance for a given collection of items
//     * and a method of rendering them
//     * TODO does not yet support collections which change but this is feasible
//     */
//    static public <X extends Object, K extends Termed, N extends TermNode<K>>
//    SpaceGrapher<K, N>  forCollection(
//            final Collection<X> c,
//            final Function<X, K> termize,
//            final BiConsumer<X, N> builder /* decorator actually */,
//            final IterativeLayout layout /* the initial one, it can be changed */
//    ) {
//
//        final Map<K, X> termObject = new HashMap();
//
//        int defaultCapacity = 128;
//
//        return new SpaceGrapher<>(
//
//                new GraphSource<K, N, Product>() {
//
//                    Set<N> nodes = Global.newHashSet(16);
//
//
//                    @Override
//                    public void forEachOutgoingEdgeOf(SpaceGrapher<K, N> sg, K src, Consumer<K> eachTarget) {
//
//                    }
//
//                    @Override
//                    public K getTargetVertex(SpaceGrapher<K, N> g, Product edge) {
//                        return (K)edge.term(1);
//                    }
//
//
//                    @Override
//                    public void start(SpaceGrapher<K, N> spaceGrapher) {
////
////                    }
////
////                    @Override
////                    public void start(SpaceGrapher<K,TermNode<K>> spaceGrapher) {
//
//                        c.forEach(o -> {
//                            if (o != null) {
//
//                                //TODO generalize this so term isnt needed here
//                                K term = termize.apply(o);
//                                termObject.put(term, o);
//
//                                N tn = spaceGrapher.getOrNewTermNode(term);
//                                if (tn != null) {
//                                    nodes.add(tn);
//                                }
//                            }
//                        });
//
//                        runLater(() -> {
//                            updateGraph(spaceGrapher);
//                            spaceGrapher.layout.set(layout);
//                            spaceGrapher.rerender();
//                        });
//
//
//                    }
//
//                    /*{
//                        return Atom.the(o.toString(),true);
//                    }*/
//
////                    @Override
////                    public void accept(SpaceGrapher<K,?> g) {
////                        //a cache would need to be invalidated here if this will support mutable collections
////                        g.setVertices(nodes);
////                    }
//
//                    @Override
//                    public void updateGraph(SpaceGrapher<K, N> g) {
//                        g.setVertices(nodes.toArray(new TermNode[nodes.size()]));
//                    }
//                },
//                new NodeVis<K, N>() {
//
//                    @Override
//                    public void accept(N termNode) {
//
//
//                        //t.update();
//
//                        //t.scale(minSize + (maxSize - minSize) * t.priNorm);
//                    }
//
//                    @Override
//                    public N newNode(K t) {
//                        N tn = (N)new TermNode(t);
//                        builder.accept(
//                                termObject.get(t),
//                                tn
//                        );
//                        return tn;
//                    }
//                },
//
//                defaultCapacity,
//
//                (S,T) -> {
//                    return new TermEdge(S,T) {
//
//                        @Override public double getWeight() {
//                            return 0.5;
//                        }
//                    };
//                },
//
//                new CanvasEdgeRenderer()
//            );
//    }


    /**
     * assumes that 's' and 't' are already ordered
     *
     * @param s
     * @param t
     * @param edgeBuilder
     */
    public final TermEdge getConceptEdgeOrdered(TermNode s, TermNode t, BiFunction<TermNode, TermNode, TermEdge> edgeBuilder) {
        return getEdge(s, t.term, edgeBuilder);
    }

    public final TermEdge getEdge(TermNode A, Termed b, BiFunction<TermNode, TermNode, TermEdge> builder) {

        TermEdge newEdge = null;
        if (A != null) {
            newEdge = A.getEdge(b);
        }

        if (newEdge == null) {
            newEdge = builder.apply(A, getTermNode(b));
            addEdge(A, b, newEdge);
        }

        return newEdge;
    }

    public final boolean addEdge(TermNode A, Termed b, TermEdge e) {
        return A.putEdge(b, e) == null;
    }

    public final TermNode getTermNode(Termed t) {
        return terms.get(t);
    }

    public final TermNode getOrNewTermNode(Termed t/*, boolean createIfMissing*/) {
        TermNode tn = terms.computeIfAbsent(t, this::newNode);
        tn.edge.clear(); //they will be re-calculated shortly
        return tn;
    }


    protected final TermNode newNode(Termed k) {

        TermNode n = nodeVis.get().newNode(k);
        if (n != null) {
            IterativeLayout<TermNode> l = layout.get();
            if (l != null)
                l.init(n);
        }
        return n;
    }

//    /**
//     * synchronizes an active graph with the scenegraph nodes
//     */
//    public void commit(final Collection<TermNode> active /* should probably be a set for fast .contains() */,
//                       ) {
//
//        final List<TermNode> toDetach = Global.newArrayList();
//
//        termList.clear();
//
//
//        runLater(() -> {
//
//
//            for (final TermNode tn : active) {
//                termList.add(tn);
//            }
//
//
//            //List<TermEdge> toDetachEdge = new ArrayList();
//            addNodes(x);
//
//            getVertices().forEach(nn -> {
//                if (!(nn instanceof TermNode)) return;
//
//                TermNode r = (TermNode) nn;
//                if (!active.contains(r.term)) {
//                    TermNode c = terms.remove(r.term);
//
//                    if (c != null) {
//                        c.setVisible(false);
//                        toDetach.add(c);
//                        //Map<Term, TermEdge> edges = c.edge;
//                        /*if (edges != null && edges.size() > 0) {
//                            //iterate the map, because the array snapshot may differ until its next update
//                            toDetachEdge.addAll(edges.values());
//                        }*/
//                    }
//                }
//
//            });
//
//            removeNodes((Collection) toDetach);
//
//            //removeEdges((Collection) toDetachEdge);
//
//            termList.clear();
//            termList.addAll(terms.values());
//
//            //print();
//            toDetach.clear();
//
//        });
//    }


    public final AtomicBoolean ready = new AtomicBoolean(true);

    protected final Runnable clear = () -> {
        displayed = TermNode.empty;
        //getVertices().clear();
        edgeRenderer.get().reset(this);
        ready.set(true);
    };


    public void setVertices(Collection<Termed> v) {


        GraphSource ss = source.get();

        NodeVis vv = nodeVis.get();

        SpaceGrapher ths = this;

        Iterator<Termed> cc = v.iterator();


        Set<TermNode> active = new HashSet(v.size()); //Global.newHashSet(maxNodes);


        while (cc.hasNext()) {

            Termed k = cc.next();
            TermNode t = getOrNewTermNode(k);
            if (t != null) {
                active.add(t);

                ss.updateNode(ths, k, t);
                vv.updateNode(t);
            } else {
                cc.remove();
            }
        }

        if (!Objects.equals(prevActive, active)) {
            TermNode[] active1 = active.toArray(new TermNode[active.size()]); //final Set<? extends V> active) {
            runLater( (active1.length == 0) ?
                clear :
                () -> {
                    getVertices().setAll(this.displayed = active1);
                    ready.set(true);
                });
        } else {
            prevActive = active;
            ready.set(true);
        }

    }

    public final boolean getReady() {
        return ready.compareAndSet(true, false);
    }


//    @FunctionalInterface
//    public interface PreallocatedResultFunction<X, Y> {
//        public void apply(X x, Y setResultHereAndReturnIt);
//    }

//    @FunctionalInterface
//    public interface PairConsumer<A, B> {
//        public void accept(A a, B b);
//    }


//    protected void updateNodes() {
//        if (termList != null)
//            termList.forEach(n -> {
//                if (n != null) n.update();
//            });
//    }


    public SpaceGrapher(NAR nar, GraphSource g,
                        NodeVis vv,
                        BiFunction<TermNode, TermNode, TermEdge> edgeVis,
                        CanvasEdgeRenderer edgeRenderer) {

        source.set(g);


        source.addListener((e, c, v) -> {

            runLater(()-> {
                logger.info("source {} <- {}", v, c);
                if (c != null)
                    c.stop();
                if (v!=null)
                    v.start(this);
            });

            /*else {
                System.out.println("no signal");
            }*/
        });

        nodeVis.addListener((l, c, v) -> {
            runLater(()-> {
                SpaceGrapher gg = SpaceGrapher.this;
                logger.info("nodeVis {} <- {}", v, c);
                if (c != null)
                    c.stop(gg);
                if (v!=null)
                    v.start(gg, nar);
            });

        });


        //.onEachNthFrame(this::updateGraph, 1);

                /*.forEachCycle(() -> {
                    double[] dd = new double[4];
                    nar.memory.getControl().conceptPriorityHistogram(dd);
                    System.out.println( Arrays.toString(dd) );

                    System.out.println(
                            nar.memory.getActivePrioritySum(true, false, false) +
                            " " +
                            nar.memory.getActivePrioritySum(false, true, false) +
                            " " +
                            nar.memory.getActivePrioritySum(false, false, true)  );

                })*/


        //runLater(() -> checkVisibility());


        //this.maxNodes = new SimpleIntegerProperty(maxNodes);
        nodeVis.set(vv); //set vis before source
        this.edgeVis = (edgeVis);
        this.edgeRenderer.set(edgeRenderer);


        //TODO add enable override boolean switch
        runLater(() -> {
            sceneProperty().addListener(v -> checkVisibility());
            parentProperty().addListener(v -> checkVisibility());
            visibleProperty().addListener(v -> checkVisibility());
        });
    }

    /**
     * called when layout changes to restart the source & layout
     */
    public synchronized void setLayout(IterativeLayout il) {

        long lastAnimPeriodMS;
        if (animator!=null) {
            lastAnimPeriodMS = animator.getPeriod();
            stop();
        } else {
            lastAnimPeriodMS = defaultFramePeriodMS;
        }


        //reset visiblity state to true for all, in case previous layout had hidden then
        //ObservableList<Node> verts = getVertices();
        //verts.forEach(t -> t.setVisible(true));

        logger.info("layout {}", il);
        layout.set(il);

        //rerender();

        //if (lastAnimPeriodMS != -1)
        start(lastAnimPeriodMS);

        //logger.info("setLayout {} on {} vertices", il, verts.size());

    }


    protected void checkVisibility() {
        if (getParent() != null && isVisible() /*&& getScene() != null*/) {
            start(defaultFramePeriodMS);
        } else
            stop();
    }


//    final static ExecutorService updater = Executors.newFixedThreadPool(1);
//    final AtomicBoolean updated = new AtomicBoolean(true);
//    final Callable<Void> updateTask = ()-> {
//

//        runLater(this::rerender);
//
//        updated.set(true);
//
//        return null;
//    };

    public void reupdate() {
        /** apply layout */
        IterativeLayout<TermNode> l;
        if ((l = layout.get()) != null) {
            l.run(this, 1);
        } else {
            //System.err.println(this + " has no layout");
        }
    }

    public void rerender() {

        EdgeRenderer<TermEdge> er = edgeRenderer.get();
        er.reset(this);

        /** apply vis properties */
        NodeVis v = nodeVis.get();
        for (TermNode n : displayed) {
            n.commit();
            v.accept(n);

            //termList.forEach((Consumer<TermNode>) n -> {

//        for (int i = 0, termListSize = termList.size(); i < termListSize; i++) {
//            final TermNode n = termList.get(i);
//            for (final TermEdge e : n.getEdges()) {
//                removable.remove(e);
//            }
//        });
//
//
//
//        termList.forEach((Consumer<TermNode>)n -> {
//        for (int i = 0, termListSize = termList.size(); i < termListSize; i++) {
//            final TermNode n = termList.get(i);

            for (TermEdge e : n.edges)
                /*if (e != null) */er.accept(e);

        }

//        removable.forEach(x -> {
//            edges.getChildren().remove(x);
//        });
//        edges.getChildren().removeAll(removable);

//        removable.clear();


    }

    final Executor updater = Executors.newSingleThreadExecutor();

    public synchronized void start(long layoutPeriodMS) {

        if (layoutPeriodMS > 0) {

            logger.info("start @ period={}ms", layoutPeriodMS);

            if (animator != null) {
                throw new RuntimeException("already started");
            }

            GraphSource src = source.get();

            animator = new Animate(layoutPeriodMS, a -> {
                //                if (updated.compareAndSet(true, false)) {
                //                    updater.execute(
                //                        new javafx.concurrent.Task<Void>() {
                //
                //                            @Override
                //                            protected Void call() throws Exception {
                //                                return updateTask.call();
                //                            }
                //                        }
                //
                //                    );
                //                }


                updater.execute(this::reupdate);
                //reupdate();

                rerender();
            });

            src.start(this);

            animator.start();
        } else {

            logger.info("stop");

            if (animator == null) {
                //throw new RuntimeException("already stopped");
                return;
            }

            GraphSource s = source.get();
            if (s != null) {
                s.stop();
            }

            animator.stop();
            animator = null;


        }
    }

    public void stop() {
        start(-1);
    }

    //    private class TermEdgeConsumer implements Consumer<TermEdge> {
//        private final Consumer<TermNode> updateFunc;
//        private final TermNode nodeToQuery;
//
//        public TermEdgeConsumer(Consumer<TermNode> updateFunc, TermNode nodeToQuery) {
//            this.updateFunc = updateFunc;
//            this.nodeToQuery = nodeToQuery;
//        }
//
//        @Override
//        public void accept(TermEdge te) {
//            if (te.isVisible())
//                updateFunc.accept(te.otherNode(nodeToQuery));
//        }
//    }
}
