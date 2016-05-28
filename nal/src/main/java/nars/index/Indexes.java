package nars.index;

import javassist.scopedpool.SoftValueHashMap;
import nars.nar.util.DefaultConceptBuilder;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * yes i know plural is 'indices' but Indexes seems better
 */
public enum Indexes {
    ;


    //    public static class DefaultTermIndex extends MapIndex2 {
    //
    //        public DefaultTermIndex(int capacity, @NotNull Random random) {
    //            super(new HashMap(capacity, 0.9f),
    //                    new DefaultConceptBuilder(random, 8, 24));
    //
    //        }
    //    }
        public static class DefaultTermIndex extends SimpleMapIndex2 {

            public DefaultTermIndex(int capacity, @NotNull Random random) {
                super(Terms.terms,
                        new DefaultConceptBuilder(random),
                        new HashMap(capacity),
                        new HashMap(capacity)
                        //new ConcurrentHashMapUnsafe(capacity)
                );
            }
        }

    public static class WeakTermIndex2 extends GroupedMapIndex {

            public WeakTermIndex2(int capacity, @NotNull Random random) {
                super(new WeakHashMap<>(capacity),
                        new DefaultConceptBuilder(random));

            }
        }

    public static class WeakTermIndex extends SimpleMapIndex2 {

            public WeakTermIndex(int capacity, @NotNull Random random) {
                super(Terms.terms,
                        new DefaultConceptBuilder(random),
                        //new SoftValueHashMap(capacity)
                        new WeakHashMap<>(capacity),
                        new WeakHashMap<>(capacity)
                );

            }
        }

    public static class SoftTermIndex extends SimpleMapIndex2 {

            public SoftTermIndex(int capacity, @NotNull Random random) {
                super(Terms.terms,
                        new DefaultConceptBuilder(random),
                        new SoftValueHashMap(capacity),
                        new WeakHashMap(capacity)
                        //new WeakHashMap<>(capacity)
                );

            }
        }
}
