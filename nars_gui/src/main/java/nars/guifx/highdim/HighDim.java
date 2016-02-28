package nars.guifx.highdim;

import javafx.scene.Group;
import javafx.scene.control.Label;
import nars.Global;
import nars.bag.BLink;
import nars.bag.Bag;
import nars.bag.impl.ArrayBag;
import nars.bag.impl.CurveBag;
import nars.concept.Concept;
import nars.guifx.Spacegraph;
import nars.guifx.demo.NARide;
import nars.guifx.util.TabX;
import nars.nar.Default;
import nars.term.Term;
import nars.term.Termed;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static javafx.application.Platform.runLater;

/**
 * Created by me on 2/28/16.
 */
public class HighDim<T extends Termed> extends Spacegraph {


    private class TermGroup extends Group implements Runnable {

        private T term;
        Label l = new Label();

        public TermGroup() {
            this.term = null;
            getChildren().add(l);
        }

        public void set(T next) {
            T prev = this.term;
            if (prev == next) return;

            this.term = next;

            runLater(this);

            //..
        }

        /** render */
        public void run() {
            if (term != null) {
                l.setText(term.toString());
                setVisible(true);
            } else {
                setVisible(false);
            }
        }

        public void update() {

        }

        @Override
        public String toString() {
            return "TermNode[" + term + ']';
        }

        public Termed get() {
            return term;
        }
    }

    final List<TermGroup> node = Global.newArrayList();
    //SimpleIntDeque free;
    final Deque<TermGroup> free = new ArrayDeque();
    private int capacity;

    public HighDim(int capacity, int inputs, int outputs) {
        super();

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

            T nt = n.term;
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
        //System.out.print("\tmid=" + free.size() + "\n");
        items.forEach(cap, next -> {
            T c = next.get();
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
                    g.set(c);
                    change[0] = true;
                    return g;
                } else {
                    //already has
                    ((TermGroup) vis).update();
                    return vis;
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


            HighDim<Concept> dim = new HighDim(4, 1, 1);
            n.onFrame(N -> {
                if (dim.commit(((Default) N).core.active))
                    System.out.println(dim.node + " free=" + dim.free.size());
            });


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
}

