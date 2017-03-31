//package nars.derive;
//
//import nars.$;
//import nars.derive.meta.BoolPredicate;
//import nars.derive.meta.PostCondition;
//import nars.derive.rule.PremiseRule;
//import nars.derive.rule.PremiseRuleSet;
//import nars.premise.Derivation;
//import nars.term.Term;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.List;
//
//
//public class SimpleDeriver implements Deriver {
//
//    @NotNull
//    private final List<List<Term>> unrolled;
//    private final PremiseRuleSet rules;
//
//    public SimpleDeriver(@NotNull PremiseRuleSet rules) {
//        this.rules = rules;
//
//        List<List<Term>> u = $.newArrayList();
//        //return Collections.unmodifiableList(premiseRules);
//        for (PremiseRule r : rules.rules) {
//            for (PostCondition p : r.postconditions)
//                u.add( r.conditions(p) );
//        }
//        this.unrolled = u;
//
//        u.forEach(System.out::println);
//    }
//
//    @Override
//    public void accept(@NotNull Derivation m) {
//
//        int now = m.now();
//
//        for (List<Term> r : unrolled) {
//            for (Term p : r) {
//
//                if (p instanceof BoolPredicate) {
//
//                    try {
//                        if (!((BoolPredicate) p).test(m))
//                            break;
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                        break;
//                    }
//                }
//
//            }
//
//            m.revert(now);
//        }
//
//    }
//}
