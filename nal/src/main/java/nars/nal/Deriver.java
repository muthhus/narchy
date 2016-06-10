package nars.nal;

import nars.nal.meta.PremiseEval;
import nars.nal.meta.PremiseRuleSet;
import nars.nal.meta.TrieDeriver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Implements a strategy for managing submitted derivation processes
 * ( via the run() methods )
 *
 * Created by patrick.hammer on 30.07.2015.
 */
public abstract class Deriver  {

    //@Deprecated public static final TermIndex terms = TermIndex.memory(16384);

    private static TrieDeriver defaultDeriver;

    private static PremiseRuleSet defaultRules;

    final static Logger logger = LoggerFactory.getLogger(Deriver.class);

    @NotNull
    public synchronized static TrieDeriver getDefaultDeriver() {
        if (defaultRules == null) {
            //synchronized(logger) {
                if (defaultDeriver == null) { //double boiler
                    try {
                        defaultRules = new PremiseRuleSet();
                        defaultDeriver = new TrieDeriver( defaultRules );
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error("getDefaultDeriver: {}",e.getCause());
                        System.exit(1);  //e.printStackTrace();
                    }
                }
            //}

        }
        return defaultDeriver;
    }



    /**
     * default set of rules, statically available
     */
    @Nullable
    public final PremiseRuleSet rules;


    public Deriver() {
        this.rules = null;
    }

    public Deriver(PremiseRuleSet rules) {
        this.rules = rules;
    }


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

    /** run an initialized rule matcher */
    public abstract void run(PremiseEval matcher);




//    public void load(Memory memory) {
//        DerivationRules r = this.rules;
//        int s = r.size();
//        for (int i = 0; i < s; i++) {
//            r.get(i).index(memory.index);
//        }
//    }
}
