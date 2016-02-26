package nars.concept;

import nars.Global;
import nars.NAR;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.variable.Variable;
import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;


public enum TermLinkBuilder {
    ;

//    public final transient Termed concept;
//
//    protected final Term[] template;
//
//    transient boolean incoming;
//
//    protected int hash;
//    protected float forgetCycles;
//    protected long now;

    public static @NotNull List<Termed> build(Termed<Compound> host, @NotNull NAR nar) {
        Set<Termed> components = Global.newHashSet(0);
        prepareComponentLinks(host.term(), components, nar);

        if (components.isEmpty())
            return Collections.emptyList();
        else
            return new FasterList(components);
    }

    static final int NegationOrConjunction = Op.or(Op.CONJUNCTION, Op.NEGATE);

    /**
     * Collect TermLink templates into a list, go down one level except in
     * special cases
     * <p>
     *
     * @param t          The CompoundTerm for which to build links
     * @param components set of components being accumulated, to avoid duplicates
     */
    static void prepareComponentLinks(@NotNull Compound t, @NotNull Set<Termed> components, @NotNull NAR nar) {

        ///** add self link for structural transform: */
        //components.add(t);

        boolean tEquivalence = t.op() == Op.EQUIV;
        boolean tImplication = t.op() == Op.IMPLICATION;


        int ni = t.size();
        for (int i = 0; i < ni; i++) {

            Term ti = growComponent(t.term(i), 0, nar);
            if (ti == null)
                continue;


            if (!(ti instanceof Variable)) {
                components.add(ti);
            }

            if ((tEquivalence || (tImplication && (i == 0))) &&
                    (ti.isAny(NegationOrConjunction))) {

                prepareComponentLinks((Compound) ti, components, nar);

            } else if (ti instanceof Compound) {
                Compound cti = (Compound) ti;


                int nj = cti.size();
                for (int j = 0; j < nj; j++) {
                    Term tj = growComponent(cti.term(j), 1, nar);
                    if (tj == null)
                        continue;


                    if (!(tj instanceof Variable)) {
                        components.add(tj);
                    }


                    if (tj instanceof Compound) {
                        Compound cctj = (Compound) tj;
                        int nk = cctj.size();
                        for (int k = 0; k < nk; k++) {

                            Term tk = growComponent(cctj.term(k), 2, nar);
                            if (tk == null)
                                continue;

                            if (!(tk instanceof Variable)) {
                                components.add(tk);
                            }
                        }
                    }


                }
            }


        }
    }


    /**
     * determines whether to grow a 1st-level termlink to a subterm
     */
    protected static Term growComponent(Term t, int level, @NotNull NAR nar) {

        Concept tti = nar.concept(t);
        if (tti == null)
            return null;

        return tti.term();
    }

//    static final boolean growLevel1(Term t) {
//        return growComponent(t) /*&&
//                ( growProductOrImage(t) || (t instanceof SetTensional)) */;
//    }

//    /** original termlink growth policy */
//    static boolean growProductOrImage(Term t) {
//        return (t instanceof Product) || (t instanceof Image);
//    }


//    static final boolean growLevel2(Term t) {
//        return growComponent(t); //growComponent(t); leads to failures, why?
//        //return growComponent(t) && growProductOrImage(t);
//        //if ((t instanceof Product) || (t instanceof Image) || (t instanceof SetTensional)) {
//        //return (t instanceof Product) || (t instanceof Image) || (t instanceof SetTensional) || (t instanceof Junction);
//    }


}
