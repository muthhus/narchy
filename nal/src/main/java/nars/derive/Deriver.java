package nars.derive;

import nars.control.premise.Derivation;
import nars.derive.rule.PremiseRuleSet;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

/**
 * Implements a strategy for managing submitted derivation processes
 * ( via the run() methods )
 * <p>
 * Created by patrick.hammer on 30.07.2015.
 *
 * TODO remove Deriver and just consider any BoolPred<Derivation> a deriver
 */
public interface Deriver extends Predicate<Derivation> {

    //@Deprecated public static final TermIndex terms = TermIndex.memory(16384);

//    TrieDeriver defaultDeriver;
//
//    PremiseRuleSet defaultRules;

    Logger logger = LoggerFactory.getLogger(Deriver.class);

//    Cache<String, Deriver> derivers = Caffeine.newBuilder().build();
//    Function<String,Deriver> loader = (s) -> new TrieDeriver(PremiseRuleSet.rules(s));

    @NotNull
    static Deriver get(String... path) {
        return TrieDeriver.get(PremiseRuleSet.rules(true, path));
    }



    //    @NotNull
//    static Deriver[] get(String... paths) {
//        return Lists.newArrayList(paths).stream().map(path -> derivers.get(path,loader)).toArray(Deriver[]::new);
//    }


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
