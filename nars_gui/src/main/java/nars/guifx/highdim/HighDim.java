package nars.guifx.highdim;

import com.sun.tools.javac.util.Log;
import javafx.scene.control.Label;
import nars.Global;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.bag.impl.CurveBag;
import nars.concept.Concept;
import nars.guifx.Spacegraph;
import nars.guifx.demo.NARide;
import nars.guifx.graph2.TermNode;
import nars.guifx.graph2.impl.HexButtonVis;
import nars.guifx.util.Animate;
import nars.guifx.util.TabX;
import nars.nar.Default;
import nars.term.Term;
import nars.term.Termed;
import nars.term.variable.Variable;
import nars.util.Texts;
import nars.util.data.Util;
import nars.util.data.bit.BitVector;
import nars.util.data.random.XorShift128PlusRandom;
import nars.util.io.StringUtil;
import nars.util.signal.Autoencoder;
import org.bridj.ann.Bits;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Arrays;
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

        abstract public int inputs();

        abstract public void project(float[] x, TermNode target);

        abstract public float[] vectorize(@NotNull BLink<? extends C> item, float[] x);
    }

    private static class ScatterPlot1 extends HighDimProjection<Concept> {
        @Override
        public float[] vectorize(@NotNull BLink<? extends Concept> concept, float[] x) {
            x[0] = 100f*(concept.hashCode() % 8192) / 8192.0f;
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

    private class TermGroup extends TermNode /* Group*/  {


        private BLink<? extends T> bterm;

        public TermGroup() {
            super(8);

            l = new Label();
            l.setCache(true);

            this.term = null;
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




            HighDimProjection<Concept> autoenc1 = new HighDimProjection<Concept>() {
                final int inputs = 20;
                final int inner = 8;


                Autoencoder enc = new Autoencoder(inputs, inner, new XorShift128PlusRandom(1));
                Autoencoder enc2 = new Autoencoder(inner, 2, new XorShift128PlusRandom(1));

                float[] y0 = new float[enc.n_hidden];
                float[] y = new float[inner];

                @Override
                public int inputs() {
                    return inputs;
                }

                @Override
                public float[] vectorize(BLink<? extends Concept> clink, float[] x) {
                    Concept c = clink.get();

                    Term t = c.term();

                    //x[0] = c.op().ordinal() / 16f; //approx;
                    //x[1] = clink.pri();


                    x[0] = (c.hashCode() % 8) / 8.0f;
                    x[1] = (t.isCompound() ? 1f : 0f);
                    x[2] = clink.pri();
                    x[3] = clink.dur();
                    x[4] = clink.qua();
                    Util.writeBits(t.op().ordinal() +1, 5, x, 5); //+5
                    Util.writeBits(t.volume()+1, 5, x, 10); //+5
                    Util.writeBits(t.size()+1, 5, x, 15); //+5


                    //System.out.println(Arrays.toString(x));

                    return x;
                }

                @Override
                public void project(float[] x, TermNode target) {
                    float err = enc.train(x, 0.05f, 0.03f, 0.03f, false);
                    enc.encode(x, y0, true, true);
                    float err2 = enc2.train(y0, 0.05f, 0.03f, 0.03f, false);
                    enc2.encode(y0, y, false, true);

                    //System.out.println(Arrays.toString(x) + " " + Arrays.toString(y));
                    float pri = x[2]; /* use original value directly */
                    //target.move( (y[0]-y[1]) *250, pri*250);
                    target.move( y[0] * 50, y[1] * 50);
                    target.scale(1f+pri);
                }


            };

            HighDim<Concept> dim = new HighDim<Concept>(16, autoenc1);

            n.onFrame(N -> {
                dim.commit(((Default) N).core.active);
                    //System.out.println(dim.node + " free=" + dim.free.size());
            });
            new Animate(10, (a)->{
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

