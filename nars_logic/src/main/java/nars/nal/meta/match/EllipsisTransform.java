package nars.nal.meta.match;

import nars.Op;
import nars.nal.meta.PremiseMatch;
import nars.nal.meta.PremiseRule;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.VariableNormalization;
import nars.term.variable.GenericVariable;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;

/** ellipsis that transforms one of its elements, which it is required to match within */
public class EllipsisTransform extends EllipsisOneOrMore {

    public final Term from;
    public final Term to;

    public EllipsisTransform(@NotNull Variable name, Term from, Term to) {
        super(name);


        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {

        String ss = super.toString();
        return ss.substring(0,ss.length()-3) /* minus the ..+, as-if calling super.super.toString() */ +
                ".." + from + '=' + to + "..+";
    }

    @NotNull
    @Override
    @Deprecated public Variable clone(@NotNull Variable v, VariableNormalization normalizer) {
        throw new RuntimeException("n/a");
    }

    public static Variable make(@NotNull Variable v, Term from, Term to, VariableNormalization normalizer) {
        //normalizes any variable parameter terms of an EllipsisTransform
        PremiseRule.PremiseRuleVariableNormalization vnn = (PremiseRule.PremiseRuleVariableNormalization) normalizer;
        return new EllipsisTransform(v,
                from instanceof GenericVariable ? vnn.applyAfter((GenericVariable)from) : from,
                to instanceof GenericVariable ? vnn.applyAfter((GenericVariable)to) : to);
    }

    @NotNull
    public EllipsisMatch collect(@NotNull Compound y, int a, int b, @NotNull PremiseMatch subst) {
        if (from == Op.Imdex && (y.op().isImage())) {

            int rel = y.relation();
            int n = (b-a)+1;
            int i = 0;
            int ab = 0;
            Term[] t = new Term[n];
            Term to = this.to;
            while (i < n)  {
                t[i++] = ((i == rel) ? subst.resolve(to) : y.term(ab));
                ab++;
            }
            return new EllipsisMatch(t);

        } else {
            return new EllipsisMatch(
                    y, a, b
            );
        }
    }
}
