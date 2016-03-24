package nars.guifx.highdim;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Joiner;
import com.google.common.cache.CacheBuilder;
import com.thoughtworks.xstream.core.util.WeakCache;
import javafx.scene.Node;
import javafx.scene.control.Button;
import nars.Global;
import nars.bag.BLink;
import nars.bag.Table;
import nars.concept.Concept;
import nars.guifx.Spacegraph;
import nars.guifx.demo.NARide;
import nars.guifx.graph2.TermNode;
import nars.guifx.util.Animate;
import nars.guifx.util.TabX;
import nars.nar.Default;
import nars.util.data.Util;
import nars.util.data.list.FasterList;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.signal.Autoencoder;
import org.jetbrains.annotations.NotNull;
import org.reactfx.collection.LiveArrayList;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static javafx.application.Platform.runLater;

/**
 * Created by me on 2/28/16.
 */
public class HighDim<T> extends Spacegraph {

    //private FasterList<TermGroup> node = null;
    private List<TermGroup> node;

    //SimpleIntDeque free;
    //final Deque<TermGroup> free = new ArrayDeque();
    private final Cache<T, TermGroup> map = Caffeine.newBuilder()
            .weakValues()
            .build();

    private int capacity;

    private HighDimProjection model;
    private List<TermGroup> showing = new FasterList();

    public boolean hasCapacity() {
        return node.size() < capacity;
    }


    private abstract static class HighDimProjection<C> {

        /**
         * inputs this projection demands
         */
        abstract public int inputs();


        /**
         * applies the vectorized representation to a component
         */
        abstract public void project(float[] x, TermNode<C> target);

        /**
         * accepts input link and vectorizes it by some encoding to the dimensionality returned by inputs()
         */
        abstract public float[] vectorize(C item, float[] x);
    }

//    static class IteratveLayoutAdapter extends HighDimProjection<Concept> {
//
//        private final IterativeLayout layout;
//
//        public IteratveLayoutAdapter(IterativeLayout l) {
//            this.layout = l;
//        }
//
//        /** inputs this projection demands */
//        public int inputs() {
//            return 2;
//        }
//
//
//
//        /** applies the vectorized representation to a component */
//        public void project(float[] x, TermNode target) {
//
//        }
//
//        /** accepts input link and vectorizes it by some encoding to the dimensionality returned by inputs() */
//        public float[] vectorize(@NotNull BLink<? extends Concept> item, float[] x) {
//
//        }
//    }

    public static class ScatterPlot1 extends HighDimProjection<BLink<? extends Concept>> {
        @Override
        public float[] vectorize(@NotNull BLink<? extends Concept> concept, float[] x) {
            x[0] = 100f * (concept.hashCode() % 16) / 16.0f;
            float cpri = concept.pri();
            x[1] = 400 * cpri;
            x[2] = 1 + 0.1f * cpri;
            return x;
        }

        @Override
        public int inputs() {
            return 3;
        }

        @Override
        public void project(float[] x, TermNode target) {
            target.move(x[0], x[1]);
            target.scale(x[2]);
            target.commit();
        }
    }


    /**
     * hardwired 2 layer autoencoder projection
     */
    abstract public static class AutoEnc2Projection<C> extends HighDimProjection<C> {
        final int inputs;
        final int inner;

        final Autoencoder enc0;
        final Autoencoder enc1;


        float[] y1;

        public AutoEnc2Projection(int inputs, int inner, int outputs) {
            this.inputs = inputs;
            this.inner = inner;
            this.enc0 = new Autoencoder(inputs, inner, new XorShift128PlusRandom(1));
            this.enc1 = new Autoencoder(inner, outputs, new XorShift128PlusRandom(1));
        }

        @Override
        public int inputs() {
            return inputs;
        }


        public abstract float[] vectorize(C clink, float[] x);

        @Override
        public void project(float[] x, TermNode<C> target) {
            float err = enc0.train(x, 0.001f, 0.02f, 0.0f, false);
            float err2 = enc1.train(enc0.y, 0.001f, 0.02f, 0.0f, false);
            this.y1 = enc1.y;

            applyIt(x, target);

        }

        protected abstract void applyIt(float[] x, TermNode<C> target);


    }

    private class TermGroup extends TermNode<T> /* Group*/ {


        public TermGroup() {
            super(8);

            this.term = null;

            l = new Button();
            setCache(true);

            getChildren().add(l);
        }

        boolean needsLabeled = false;

        public TermGroup set(@NotNull T next) {

            T prev = this.term;
            if (prev == next) return this;

            //T term = ((this.term = next) != null) ? this.term : null;
            this.term = next;

            //if (term != null) {
                needsLabeled = true;
            //}

            return this;
            //..
        }

        float tmp[];

        /**
         * when term changes
         */
        Button l;


        /* TODO abstract */
        protected void update(T t) {
            l.setText(
                    Util.s(t.toString(), 8)
            );
//            if (getChildren().size() > 1)
//                getChildren().remove(1);
//            if (newTerm!=null) {
//                HexButtonVis.HexButton<Termed> b = new HexButtonVis.HexButton<>(newTerm);
//                b.setManaged(true);
//                getChildren().add(
//                        b
//                    //new Button(newTerm.toString())
//                );
//            }
        }

        /**
         * cycle update, same term
         */
        public void update() {
            T bterm = this.term;
            //if (bterm == null)
                //return;

            HighDimProjection m = HighDim.this.model;
            int input = m.inputs();

            float[] tmp = this.tmp;
            if ((tmp == null) || (tmp.length != input))
                tmp = this.tmp = new float[input];

            m.project(m.vectorize(bterm, tmp), this);
        }

        /**
         * render
         */
        @Override
        public void commit() {
            //if (term!=null) {
                if (needsLabeled) {
                    update(term);
                    needsLabeled = false;
                }
                super.commit();
                setVisible(true);
            /*} else {
                setVisible(false);
            }*/
        }


        @Override
        public String toString() {
            return "TermNode[" + term + ']';
        }


//        @Override
//        public int hashCode() {
//            return (term == null) ? 0 : super.hashCode();
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            return (term != null) && super.equals(o);
//        }
    }


    public HighDim(int capacity, HighDimProjection model) {
        super();

        resize(capacity);
        setModel(model);
    }

    private void setModel(HighDimProjection model) {

        this.model = model;
    }

    public synchronized void resize(int capacity) {
        //if (this.capacity == capacity) return;

        this.capacity = capacity;
        //this.node = new FasterList<>(capacity, (TermGroup[])Array.newInstance(TermGroup.class, capacity));
        this.node = new FasterList(capacity);

//        runLater(() -> {
//            getVertices().clear();
//
//            IntStream.range(0, capacity).mapToObj(i -> {
//                TermGroup g = new TermGroup();
//                g.setVisible(false);
//                return g;
//            }).collect(Collectors.toCollection(() -> node));
//            verts.getChildren().setAll(node);
//        });
    }

    public void clear() {
        //free.addAll(node);
        node.clear();
        //map.clear();
    }

    //TODO restore original early-terminating bag iterator
    public void commit(Table<?, T> items) {

        //final boolean[] change = {false};

        //float priThresh = ((CurveBag) items).priAt(cap);


//        node.forEach((TermGroup n) -> {
//
//            T nt = n.term;
//            if (nt != null) {
//                T bc = items.get(((BLink) nt).get()); //HACK
//
//                if ( /* in bag */ (bc != null)
//                        /* but below threshold */ /*(bc.pri() < priThresh)*/) {
//
//                    //transition from display to undisplay:
//                    //  this will be recycled and undisplayed
//                    //free.addLast(n);
//                    //n.set(null);
//                    change[0] = true;
//                    //((Concept)(bc.get())).put(this, null);
//
//                }
//            }
//            //else: it continues visibility or invisibility
//        });

        node.clear();
        items.topWhile(/*cap,*/ (T c) -> {
            //T c = (T)cLink;//.get();
            //TODO find somethign more efficient
            //change[0] |=
            visualize(c);
            return hasCapacity();
            //return !free.isEmpty();
        });
        //System.out.println("\tend=" + free.size());

        //boolean changed = change[0];
        //if (changed) {
            commit();
        //}

    }

    public void commit() {

        List<TermGroup> xnode = node;
        node = new FasterList<>();
        runLater(() -> {
            try {
                verts.getChildren().setAll(xnode); //next);
                this.showing = xnode;
            } catch (IllegalArgumentException e) {
                System.err.println(e);
                //System.err.println("Num nodes: " + next.length);
                //System.out.println(Joiner.on('\n').join(next));
                System.exit(1);
            }

        });


    }

    public TermGroup visualize(T c) {
        TermGroup g = map.get(c, (C) -> {
            return new TermGroup().set(C);
//            if (vis == null) {
//                //requires one
////                /*if (free.isEmpty()) {
////                    //System.out.println("could not show: " + c);
////                    //but maybe it will be visible next cycle
////                    return null /*vis*/;
////                }*/
//                TermGroup g = free.removeFirst();
//                g.set(c);
//                node.add(g);
//                return g;
//            } else {
//                //already has
//                return vis;
//            }
        });
        if (g!=null)
            node.add(g);
        return g;
    }


    public static void main(String[] args) {

        Default n = new Default();
        n.input("<a --> b>.");
        n.input("<b --> c>.");
        n.input("(c:d,b).");

        NARide.show(n.loop(), ide -> {


            HighDim<BLink<Concept>> dim = new HighDim(64, new AEConcept1());

            n.onFrame(N -> {
                dim.commit(((Default) N).core.active);
                //System.out.println(dim.node + " free=" + dim.free.size());
            });
            new Animate(30, (a) -> dim.update()).start();


            //ide.addView(new IOPane(n));
            ide.content.getTabs().setAll(
                    new TabX("Graph",
                            //newGraph(n)
                            dim
                    ));


            ide.setSpeed(150);
            //n.frame(5);


        });
    }

    /**
     * compute the values for the next iteration
     */
    public void update() {
        //TermGroup[] node = this.node.array();
        List<TermGroup> n = this.showing;
        for (int i = 0, nodeSize = n.size(); i < nodeSize; i++) {
            TermGroup nn = n.get(i);
            if (nn == null) break;
            //if (n.term != null) {
                nn.update();
                nn.commit();
            //}
        }
    }

}