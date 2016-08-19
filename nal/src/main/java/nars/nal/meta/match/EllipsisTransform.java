package nars.nal.meta.match;

import nars.Op;
import nars.nal.rule.PremiseRule;
import nars.term.Term;
import nars.term.transform.VariableNormalization;
import nars.term.var.AbstractVariable;
import nars.term.var.GenericNormalizedVariable;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;

/** ellipsis that transforms one of its elements, which it is required to match within */
public class EllipsisTransform extends EllipsisOneOrMore {

    @NotNull
    public final Term from;
    @NotNull
    public final Term to;

    public EllipsisTransform(@NotNull AbstractVariable name, @NotNull Term from, @NotNull Term to) {
        super(name, multiVariable(name.hashCode(),
                from.equals(Op.Imdex) ? to.hashCode() : from.hashCode(), //hashcode of the non-imdex term
                from.equals(Op.Imdex) //bit indicating which one it refers to
        ));
        this.from = from;
        this.to = to;
    }

    public static int multiVariable(int a, int b, boolean fromOrTo) {
        return ((a+1) << (1+8)) | ((b+1) << 1) | (fromOrTo ? 0 : 1);
    }

    @NotNull
    @Override
    public String toString() {

        String ss = super.toString();
        return ss.substring(0,ss.length()-3) /* minus the ..+, as-if calling super.super.toString() */ +
                ".." + from + '=' + to + "..+";
    }

    @NotNull
    @Override
    @Deprecated public Variable clone(@NotNull AbstractVariable v, VariableNormalization normalizer) {
        //throw new RuntimeException("n/a");
        //return new GenericVariable(Op.VAR_QUERY, "Ellipsis_Transform_Clone_Unknown");
        return this;
    }

    @NotNull
    public static Variable make(@NotNull AbstractVariable v, Term from, Term to, VariableNormalization normalizer) {
        //normalizes any variable parameter terms of an EllipsisTransform
        PremiseRule.PremiseRuleVariableNormalization vnn = (PremiseRule.PremiseRuleVariableNormalization) normalizer;
        return new EllipsisTransform(v,
                from instanceof Variable ? vnn.applyAfter((Variable)from) : from,
                to instanceof Variable ? vnn.applyAfter((Variable)to) : to);
    }

//    @NotNull
//    public Term match(@NotNull Compound y, int a, int b, @NotNull FindSubst subst) {
//        if (from == Op.Imdex && (y.op().isImage())) {
//
//            int rel = y.relation();
//            int n = (b-a)+1;
//            int i = 0;
//            int ab = 0;
//            Term[] t = new Term[n];
//            Term to = this.to;
//            while (i < n)  {
//                t[i++] = ((i == rel) ? subst.resolve(to) : y.term(ab));
//                ab++;
//            }
//            return EllipsisMatch.match(t);
//
//        } else {
//            return EllipsisMatch.match(y, a, b);
//        }
//    }
}
