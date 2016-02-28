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
import nars.guifx.util.Animate;
import nars.guifx.util.TabX;
import nars.nar.Default;
import nars.term.Termed;
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
abstract public class HighDim<T extends Termed> extends Spacegraph {

    final List<TermGroup> node = Global.newArrayList();
    //SimpleIntDeque free;
    final Deque<TermGroup> free = new ArrayDeque();
    private int capacity;
    final int input, output;


    /** infer a feature vector from the visualizable instance */
    abstract public float[] vectorize(@NotNull BLink<? extends T> t, @NotNull float[] tmpIn);

    /** learn and classify the vector */
    abstract public void project(float[] tmpIn, TermNode target);

    private class TermGroup extends TermNode /* Group*/  {

        Label l = new Label();
        private BLink<? extends T> bterm;

        public TermGroup() {
            super(8);
            this.term = null;
            getChildren().add(l);
        }

        public void set(BLink<? extends T> next) {
            Object prev = this.bterm;
            if (prev == next) return;

            this.term = ((this.bterm = next)!=null) ? bterm.get() : null;


            //..
        }

        float tmp[];

        public void update() {
            BLink<? extends T> bterm = this.bterm;
            if (bterm == null)
                return;

            float[] tmp = this.tmp;
            if ((tmp == null) || (tmp.length!=input))
                tmp = this.tmp = new float[input];

            vectorize(bterm, tmp);

            //learn(tmpIn);
            project(tmp, this);
        }

        /** render */
        public void commit() {
            if (term != null) {
                l.setText(term.toString());
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


    public HighDim(int capacity, int inputs, int outputs) {
        super();

        this.input = inputs;
        this.output = outputs;

        resize(capacity);
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
        n.input("<c --> d>.");

        NARide.show(n.loop(), ide -> {


            HighDim<Concept> dim = new HighDim<Concept>(32, 4, 2){


                @Override
                public float[] vectorize(@NotNull BLink<? extends Concept> concept, float[] x) {
                    x[0] = 100f*(concept.hashCode() % 8192) / 8192.0f;
                    float cpri = concept.pri();
                    x[1] = cpri;
                    x[2] = 100f * cpri;
                    return x;
                }

                @Override
                public void project(float[] x, TermNode target) {
                    target.move(x[0], x[1]);
                    target.setScaleX(x[2]);
                    target.setScaleY(x[2]);
                }
            };

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
            } else {

            }
        }
    }
}

