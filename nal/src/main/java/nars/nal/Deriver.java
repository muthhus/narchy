package nars.nal;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Lists;
import nars.nal.derive.TrieDeriver;
import nars.nal.rule.PremiseRuleSet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Implements a strategy for managing submitted derivation processes
 * ( via the run() methods )
 * <p>
 * Created by patrick.hammer on 30.07.2015.
 */
public interface Deriver extends Consumer<Derivation> {

    //@Deprecated public static final TermIndex terms = TermIndex.memory(16384);

//    TrieDeriver defaultDeriver;
//
//    PremiseRuleSet defaultRules;

    Logger logger = LoggerFactory.getLogger(Deriver.class);

    AsyncLoadingCache<String, Deriver> derivers = Caffeine.newBuilder().buildAsync((s) -> {
        try {
            return new TrieDeriver(PremiseRuleSet.rules(s));
        } catch (IOException e) {
            return (Deriver) null;
        }
    });

    @NotNull
    static Deriver get(String path) {
        return derivers.synchronous().get(path);
    }
    @NotNull
    static Deriver[] get(String... paths) {
        return Lists.newArrayList(paths).parallelStream().map(path -> derivers.synchronous().get(path)).toArray(Deriver[]::new);
    }


//    @NotNull public static TrieDeriver getDefaultDeriver() {
//        synchronized (Deriver.class) {
//            if (defaultRules == null) {
//                //synchronized(logger) {
//                if (defaultDeriver == null) { //double boiler
//                    Util.time(logger, "Rule parse", () -> {
//                        try {
//                            defaultRules = PremiseRuleSet
//                                    //.rulesCached("nal.nal");
//                                    .rules("nal.nal");
//                        } catch (Exception e) {
//                            logger.error("rule parse: {}", e);
//                            throw new RuntimeException(e);
//                        }
//                    });
//                    Util.time(logger, "Rule compile", () -> {
//                        defaultDeriver = new TrieDeriver(defaultRules);
//                    });
//                }
//                //}
//
//            }
//            return defaultDeriver;
//        }
//    }


//    /**
//     * default set of rules, statically available
//     */
//    @Nullable
//    public final PremiseRuleSet rules;
//
//
//    public Deriver(@Nullable PremiseRuleSet rules) {
//        this.rules = rules;
//    }


//    //not ready yet
//    static void loadCachedRules() {
//        final String key = "derivation_rules:standard";
//        Deriver.standard = TemporaryCache.computeIfAbsent(
//                key, new GenericJBossMarshaller(),
//                () -> {
//                    try {
////                        standard = new DerivationRules();
//
//                        return new DerivationRules();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        System.exit(1);
//                        return null;
//                    }
//                }
////                //TODO compare hash/checksum of the input file
////                //to what is stored in cached file
////                (x) -> {
////                    //this disables entirely and just creates a new one each time:
////                    return  ...
////                }
//        );
//    }

//    /** run an initialized rule matcher */
//    public abstract void run(@NotNull PremiseEval matcher);


//    public void load(Memory memory) {
//        DerivationRules r = this.rules;
//        int s = r.size();
//        for (int i = 0; i < s; i++) {
//            r.get(i).index(memory.index);
//        }
//    }

}
