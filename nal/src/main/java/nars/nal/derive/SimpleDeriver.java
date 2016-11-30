package nars.nal.derive;

import nars.$;
import nars.nal.Deriver;
import nars.nal.meta.BoolCondition;
import nars.nal.meta.PostCondition;
import nars.nal.meta.Derivation;
import nars.nal.rule.PremiseRule;
import nars.nal.rule.PremiseRuleSet;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class SimpleDeriver implements Deriver {

    @NotNull
    private final List<List<Term>> unrolled;
    private final PremiseRuleSet rules;

    public SimpleDeriver(@NotNull PremiseRuleSet rules) {
        this.rules = rules;

        List<List<Term>> u = $.newArrayList();
        //return Collections.unmodifiableList(premiseRules);
        for (PremiseRule r : rules.rules) {
            for (PostCondition p : r.postconditions)
                u.add( r.conditions(p) );
        }
        this.unrolled = u;

        u.forEach(System.out::println);
    }

    @Override
    public void accept(@NotNull Derivation m) {

        int now = m.now();

        for (List<Term> r : unrolled) {
            for (Term p : r) {

                if (p instanceof BoolCondition) {

                    try {
                        if (!((BoolCondition) p).run(m, m.now()))
                            break;

                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }

            }

            m.revert(now);
        }

    }
}
