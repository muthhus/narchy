package nars.guifx.highdim;

import javafx.scene.control.Label;
import nars.Global;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.concept.Concept;
import nars.guifx.Spacegraph;
import nars.guifx.demo.NARide;
import nars.guifx.graph2.TermNode;
import nars.guifx.graph2.layout.IterativeLayout;
import nars.guifx.util.Animate;
import nars.guifx.util.TabX;
import nars.nar.Default;
import nars.term.Termed;
import nars.util.data.Util;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.signal.Autoencoder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static javafx.application.Platform.runLater;

/**
 * Created by me on 2/28/16.
 */
public class HighDim<T extends Termed> extends Spacegraph {

    final List<TermGroup> node = Global.newArrayList();
    //SimpleIntDeque free;
    final Deque<TermGroup> free = new ArrayDeque();
    private int capacity;

    private HighDimProjection model;



    private abstract static class HighDimProjection<C> {

        /** inputs this projection demands */
        abstract public int inputs();


        /** applies the vectorized representation to a component */
        abstract public void project(float[] x, TermNode target);

        /** accepts input link and vectorizes it by some encoding to the dimensionality returned by inputs() */
        abstract public float[] vectorize(@NotNull BLink<? extends C> item, float[] x);
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

    private static class ScatterPlot1 extends HighDimProjection<Concept> {
        @Override
        public float[] vectorize(@NotNull BLink<? extends Concept> concept, float[] x) {
            x[0] = 100f*(concept.hashCode() % 16) / 16.0f;
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
        }
    }


    /** hardwired 2 layer autoencoder projection */
    abstract public static class AutoEnc2Projection extends HighDimProjection<Concept> {
        final int inputs;
        final int inner;

        final Autoencoder enc0;
        final Autoencoder enc1;

        final float[] y0;
        final float[] y1;

        public AutoEnc2Projection(int inputs, int inner, int outputs) {
            this.inputs = inputs;
            this.inner = inner;
            this.enc0 = new Autoencoder(inputs, inner, new XorShift128PlusRandom(1));
            this.y0 = new float[inner];
            this.enc1 = new Autoencoder(inner, outputs, new XorShift128PlusRandom(1));;
            this.y1 = new float[inner];
        }

        @Override
        public int inputs() {
            return inputs;
        }

        @Override
        public float[] vectorize(BLink<? extends Concept> clink, float[] x) {
            vectorizeIt(clink, x);
            return x;
        }

        protected abstract void vectorizeIt(BLink<? extends Concept> clink, float[] x);

        @Override
        public void project(float[] x, TermNode target) {
            float err = enc0.train(x, 0.004f, 0.05f, 0.05f, false);
            enc0.encode(x, y0, true, true);
            float err2 = enc1.train(y0, 0.008f, 0.05f, 0.05f, false);
            enc1.encode(y0, y1, false, true);

            applyIt(x, target);

        }

        protected abstract void applyIt(float[] x, TermNode target);


    }

    private class TermGroup extends TermNode /* Group*/  {


        private BLink<? extends T> bterm;

        public TermGroup() {
            super(8);

            this.term = null;

            l = new Label();
            setCache(true);

            getChildren().add(l);
        }

        public void set(BLink<? extends T> next) {
            Object prev = this.bterm;
            if (prev == next) return;

            Termed term = ((this.bterm = next) != null) ? bterm.get() : null;
            this.term = term;

            if (term!=null) {
                runLater(()->{
                    update(term);
                });
            }

            //..
        }

        float tmp[];

        /** when term changes */
        Label l;

        /* TODO abstract */ protected void update(Termed newTerm) {
            l.setText(
                Util.s(newTerm.toString(), 8)
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

        /** cycle update, same term */
        public void update() {
            BLink<? extends T> bterm = this.bterm;
            if (bterm == null)
                return;

            HighDimProjection m = HighDim.this.model;
            int input = m.inputs();

            float[] tmp = this.tmp;
            if ((tmp == null) || (tmp.length!=input))
                tmp = this.tmp = new float[input];

            m.project(m.vectorize(bterm, tmp), this);
        }

        /** render */
        public void commit() {
            if (term != null) {
                setVisible(true);
            } else {
                setVisible(false);
            }
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
        if (this.capacity == capacity) return;

        this.capacity = capacity;
        runLater(() -> {
            getVertices().clear();
            free.clear();

            IntStream.range(0, capacity).mapToObj(i -> {
                TermGroup g = new TermGroup();
                g.setVisible(false);
                free.push(g);
                return g;
            }).collect(Collectors.toCollection(() -> node));
            verts.getChildren().setAll(node);
        });
    }

    public boolean commit(Bag<T> items) {
        int cap = capacity;

        final boolean[] change = {false};

        float priThresh = ((CurveBag) items).priAt(cap);

        node.forEach((TermGroup n) -> {

            T nt = (T) n.term;
            if (nt != null) {
                BLink<T> bc = items.get(nt);

                if ( /* in bag */ (bc != null) &&
                        /* but below threshold */ (bc.pri() < priThresh)) {

                    //transition from display to undisplay:
                    //  this will be recycled and undisplayed
                    free.addLast(n);
                    n.set(null);
                    change[0] = true;
                    ((Concept)(bc.get())).put(this, null);

                }
            }
            //else: it continues visibility or invisibility
        });

        items.forEach(cap, cLink -> {
            T c = cLink.get();
            //TODO find somethign more efficient
            ((Concept) c).putCompute(this, (C, vis) -> {
                if (vis == null) {
                    //requires one
                    if (free.isEmpty()) {
                        //System.out.println("could not show: " + c);
                        //but maybe it will be visible next cycle
                        return vis;
                    }
                    TermGroup g = free.removeFirst();
                    g.set(cLink);
                    change[0] = true;
                    return g;
                } else {
                    //already has
                    TermGroup g = (TermGroup) vis;
                    return g;
                }
            });
        });
        //System.out.println("\tend=" + free.size());

        boolean changed = change[0];
        if (changed) {
            runLater(() -> {
                verts.getChildren().setAll(node);
            });
        }

        return changed;
    }


    public static void main(String[] args) {

        Default n = new Default();
        n.input("<a --> b>.");
        n.input("<b --> c>.");
        n.input("(c:d,b).");

        NARide.show(n.loop(), ide -> {


            HighDim<Concept> dim = new HighDim<Concept>(64, new AEConcept1());

            n.onFrame(N -> {
                dim.commit(((Default) N).core.active);
                    //System.out.println(dim.node + " free=" + dim.free.size());
            });
            new Animate(30, (a)->{
               dim.update();
            }).start();


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

    /** compute the values for the next iteration */
    public void update() {

        List<TermGroup> node = this.node;
        for (int i = 0, nodeSize = node.size(); i < nodeSize; i++) {
            TermGroup n = node.get(i);
            if (n.term != null) {
                n.update();
                n.commit();
            }
        }
    }
}

