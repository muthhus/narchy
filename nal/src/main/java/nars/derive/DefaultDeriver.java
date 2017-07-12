package nars.derive;

import nars.derive.rule.PremiseRuleSet;

/**
 * implementation of a simple distributor of derivation across rulesets
 *
 * Priority given to lower indexed items
 * These will be iterated first. the first tier return false to interupt ends the upward sweep
 *
 *  2481 0
 *  2482 0
 *  2482 2
 *  2482 1
 *  2482 0
 *  2482 1
 *  2482 0
 *  2482 0
 *  2483 2
 *  2483 1
 */
public class DefaultDeriver  {

    public static final PremiseRuleSet rules =
            PremiseRuleSet.rules(true,
                        "nal1.nal",
                        //"nal4.nal",
                        "nal6.nal",
                        "misc.nal",
                        "induction.nal",
                        "nal2.nal",
                        "nal3.nal"
            );

//    public static final Deriver the =
//              (TrieDeriver) Deriver.get(
//                );

//            //new DefaultDeriver();
//
//    final TrieDeriver[] levels;
//
//
//    public DefaultDeriver() {
//
//        levels = new TrieDeriver[]{
//                (TrieDeriver) Deriver.get(
//                        "nal1.nal",
//                        //"nal4.nal",
//                        "nal6.nal",
//                        "misc.nal",
//                        "induction.nal"
//
//                        "nal2.nal",
//                        "nal3.nal"
//                )
//        };
//
//
//
//    }
//
//    //new InstrumentedDeriver(
//
//
//    //"relation_introduction.nal"
//
//    //)
//
//
//    @Override
//    public boolean test(Derivation derivation) {
//        int num = (derivation.next()) % (1+ levels.length);
//        for (int i = 0; i < num; i++) {
//            if (!levels[i].test(derivation))
//                return false;
//        }
//        return true;
//    }
//
//
}
