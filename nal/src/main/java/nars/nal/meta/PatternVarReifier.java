//package nars.nal.meta;
//
//import nars.$;
//import nars.Op;
//import nars.term.Compound;
//import nars.term.Term;
//import nars.term.Termed;
//import nars.term.Terms;
//import nars.term.transform.CompoundTransform;
//import nars.term.variable.Variable;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
///**
// * Created by me on 4/28/16.
// */
//public class PatternVarReifier implements CompoundTransform<Compound, Term> {
//
//    final int id;
//
//    public PatternVarReifier(int ruleID) {
//        this.id = ruleID;
//    }
//
//    @Override
//    public boolean test(@NotNull Term superterm) {
//        return (superterm.varPattern() > 0);
//        //return (o instanceof Compound) && ((Compound)o).varPattern() > 0;
//    }
//
//    @Override
//    public @Nullable Termed apply(Compound parent, @NotNull Term subterm) {
//        return unpatternify(subterm);
//    }
//
//    @Nullable
//    public Term unpatternify(@NotNull Term subterm) {
//        String ruleID = Integer.toString(id, 36);
//        if (subterm.op() == Op.VAR_PATTERN) {
//            return $.quote("%" + ((Variable) subterm).id() + "_" + ruleID);
//        } else if (subterm instanceof Compound) {
//            return ruleComponent((Compound) subterm, this);
//        }
//        return subterm;
//    }
//
//
//
//    @Nullable
//    private static Term ruleComponent(@NotNull Compound term, @NotNull PatternVarReifier r) {
//        return Terms.terms.transform(term, r);
//    }
//
//}
