package nars.gui;

import jcog.event.On;
import jcog.pri.Prioritized;
import nars.NAR;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

abstract public class NARChart<X extends Prioritized> extends BagChart<X> implements Consumer<NAR> {
    private final On on;
    final NAR nar;
    long now;
    int dur;

    public NARChart(Iterable<X> b, NAR nar) {
        super(b);
        this.now = nar.time();
        this.nar = nar;
        on = nar.onCycle(this);
    }

    @Override
    public void stop() {
        super.stop();
        on.off();
    }

   @Override
    public void update(double width, double height, Iterable<? extends X> children, BiConsumer<X, ItemVis<X>> update) {
        long now = nar.time();
        if (now == this.now)
            return;
        this.now = now;

        dur = nar.dur();
        super.update(width, height, children, update);
    }

    @Override
    public void accept(NAR nar) {
        update();
    }
}
