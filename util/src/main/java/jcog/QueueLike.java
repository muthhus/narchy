package jcog;

import jcog.bag.Bag;
import org.jetbrains.annotations.Nullable;

import java.util.Queue;
import java.util.function.Function;

/**
 * FIFO-like base interface
 * @param x
 * @return
 *  x non-null: non-null value (generally x itself) if queued, null if not
 *  x null: popped item if available, null if not
 */
@FunctionalInterface public interface QueueLike<X> extends Function<X,X> {

    static <X> QueueLike<X> of(Queue<X> q) {
        return new QueueLikeQueue<>(q);
    }
    static <X> QueueLike<X> of(Bag<?,X> q) {
        return new QueueLikeBag<>(q);
    }

    final class QueueLikeQueue<X> implements QueueLike<X> {

        private final Queue<X> q;

        public QueueLikeQueue(Queue<X> q) {
            this.q = q;
        }

        @Override
        @Nullable public X apply(@Nullable X x) {
            if (x!=null) {
                if (q.offer(x))
                    return x;
                return null;
            } else {
                return q.poll();
            }
        }
    }

    class QueueLikeBag<X> implements QueueLike<X> {
        private final Bag<?, X> q;

        public QueueLikeBag(Bag<?, X> q) {
            this.q = q;
        }

        @Override
        public X apply(X x) {
            if (x != null) {
                return q.put(x);
            } else {
                return q.pop();
            }
        }
    }
}
