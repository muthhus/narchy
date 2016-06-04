package nars.guifx.concept;

import impl.org.controlsfx.table.MappedList;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import nars.Global;
import nars.bag.Bag;
import nars.link.BLink;
import org.reactfx.collection.LiveArrayList;
import org.reactfx.collection.LiveList;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toCollection;
import static javafx.application.Platform.runLater;

/**
 * Created by me on 3/18/16.
 */
public class BagView<X> extends VBox /* FlowPane */  {

    final Map<BLink<X>, Node> componentCache = new WeakHashMap<>();
    private final Supplier<Bag<X>> bag;
    private final Function<BLink<X>, Node> builder;
    //final Collection<BLink<X>> pending = Global.newHashSet(1); //Global.newArrayList();
    //final AtomicBoolean queued = new AtomicBoolean();
    private final int limit;

    public BagView(Bag<X> bag, Function<BLink<X>, Node> builder, int limit) {
        this(()->bag, builder, limit);
    }

    public BagView(Supplier<Bag<X>> bag, Function<BLink<X>, Node> builder, int limit) {
        this.bag = bag;
        this.builder = builder;
        this.limit = limit;


        setCache(true);

        update();


    }


    Node getNode(BLink<X> n) {
        Node existing = componentCache.computeIfAbsent(n, builder);
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

//        nodes.removeIf(n ->
//            !bLinks.contains(...)
//        );


        int size = bLinks.size();
        List<Node> x;
        if (size == 0) {
            x = null;
        } else {
            x = Global.newArrayList(size);
            bLinks.forEach(limit, a -> {
                x.add(getNode(a));
            });
        }

        runLater(() -> {
            if (x != null)
                getChildren().setAll(x);
            else
                getChildren().clear();
            layout();
        });


//        if (!queued.compareAndSet(false, true)) {
//            Collection<BLink<X>> p = this.pending;
//            p.clear();
//            bLinks.forEach(limit, p::add);
//
//            //if (!getChildren().equals(p)) {
//            runLater(this);
//            /*} else {
//                queued.set(false);
//            }*/
//        }

    }

//    /** dont call directly, use update() instead which will invoke this indirectly if necessary */
//    @Override public void run() {
//        //synchronized (pending) {
//        if (!queued.get())
//            return;
//
//        try {
//            ObservableList<Node> ch = getChildren();
//            ch.clear();
//            pending.stream().map(this::getNode).collect(toCollection(() -> ch));
//        } catch (Exception e) {
//            System.err.println("BagView: " + e);
//        }
//
////            getChildren().forEach(n -> {
////                if (n instanceof Runnable)
////                    ((Runnable) n).run();
////            });
//
//        queued.set(false);
//
//    }
}
