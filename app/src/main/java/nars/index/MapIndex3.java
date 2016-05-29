//package nars.index;
//
//import com.github.benmanes.caffeine.cache.Cache;
//import com.github.benmanes.caffeine.cache.Caffeine;
//import nars.concept.Concept;
//import nars.term.TermBuilder;
//import nars.term.container.TermContainer;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.util.Collections;
//import java.util.HashMap;
//
///**
// * see: https://github.com/ben-manes/caffeine/wiki
// */
//public class MapIndex3 extends GroupedMapIndex {
//
//    //final RUCache<Termed,Termed> front = new RUCache(256);
//
//    final Cache<TermContainer, GroupedMapIndex.SubtermNode> cache;
//
//    public MapIndex3(int capacity, TermBuilder termBuilder, Concept.ConceptBuilder conceptBuilder) {
//        super(
//                Collections.unmodifiableMap(new HashMap()) /* dummy */,
//                termBuilder, conceptBuilder);
//
//        cache = Caffeine.newBuilder()
//                //.expireAfterWrite(10, TimeUnit.MINUTES)
//                .initialCapacity(capacity)
//                .maximumSize(capacity*1024)
//
////                .build(new CacheLoader<TermContainer, MapIndex2.SubtermNode>() {
////
////                    @Override
////                    public SubtermNode load(@Nonnull TermContainer key) throws Exception {
////                        return null;
////                    }
////                });
//                .build();
//
//// Lookup an entry, or null if not found
//        //Graph graph = cache.getIfPresent(key);
//// Lookup and compute an entry if absent, or null if not computable
//        //graph = cache.get(key, k -> createExpensiveGraph(key));
//// Insert or update an entry
//        //cache.put(key, graph);
//// Remove an entry
//        //cache.invalidate(key);
//    }
//
////    @Override
////    public
////    @Nullable
////    Termed get(@NotNull Termed t) {
////        Termed f = front.tryGet(t);
////        if (f==null) {
////            f = super.get(t);
////            front.set(f, f);
////        }
////        return f;
////    }
//
//    @Override
//    public @Nullable
//    GroupedMapIndex.SubtermNode getNode(TermContainer s) {
//        return cache.getIfPresent(s);
//    }
//
//    @Override
//    public @NotNull
//    GroupedMapIndex.SubtermNode getOrAddNode(TermContainer s) {
//        @Nullable TermContainer kk = normalize(s);
//        return cache.get(kk, k -> {
//            return new SubtermNodeWithArray(k);
//        });
//
////        SubtermNode existing = getNode(s);
////        if (existing==null) {
////            s = normalize(s);
////            existing =
////                    //new MapIndex2.SubtermNode(normalize(s));
////                    new MapIndex2.SubtermNodeWithArray(normalize);
////
////            cache.put(s, existing);
////        }
////        return existing;
//    }
//
//    @Override
//    public int size() {
//        return (int)cache.estimatedSize();// + atoms.size(); //HACK
//    }
//
//    @Override
//    public int subtermsCount() {
//        return (int)cache.estimatedSize();
//    }
//
//    @Override
//    public void clear() {
//        super.clear();
//        cache.invalidateAll();
//        //atoms.clear();
//
//    }
//}
