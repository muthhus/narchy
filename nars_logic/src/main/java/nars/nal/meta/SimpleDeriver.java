package nars.nal.meta;

import nars.Global;
import nars.nal.Deriver;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class SimpleDeriver extends Deriver {

    @NotNull
    private final List<List<Term>> unrolled;

    public SimpleDeriver(@NotNull PremiseRuleSet rules) {
        super(rules);

        List<List<Term>> u = Global.newArrayList();
        //return Collections.unmodifiableList(premiseRules);
        for (PremiseRule r : rules.rules) {
            for (PostCondition p : r.postconditions)
                u.add( r.getConditions(p) );
        }
        this.unrolled = u;

        u.forEach(System.out::println);
    }

    @Override
    public void run(@NotNull PremiseEval m) {

        int now = m.now();

        for (List<Term> r : unrolled) {
            for (Term p : r) {

                if (p instanceof BooleanCondition) {

                    try {
                        if (!((BooleanCondition) p).booleanValueOf(m))
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
