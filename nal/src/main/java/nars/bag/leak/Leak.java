package nars.bag.leak;

import jcog.bag.Bag;
import jcog.data.FloatParam;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

import static nars.time.Tense.ETERNAL;

/**
 * drains items from a Bag at
 */
public class Leak<X, Y> {

    //private static final Logger logger = LoggerFactory.getLogger(MutaTaskBag.class);

    @NotNull
    public final Bag<X, Y> bag;


    /**
     * rate = max successful leaks per duration
     */
    public Leak(@NotNull Bag<X, Y> bag) {
        this.bag = bag;
    }

    /**
     * minimum bag size allowed before leak
     */
    public int min() {
        return 1;
    }

    public void setCapacity(int capacity) {
        bag.setCapacity(capacity);
    }

    public void clear() {
        bag.clear();
    }
}
