package nars.link;

import nars.NAR;
import nars.Param;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public enum TermLinkBuilder {
    ;


    @NotNull public static Set<Term> components(@NotNull Compound host, @NotNull NAR nar) {

        Set<Term> components =
                //new TreeSet<>();
                new HashSet<>(host.complexity());

        int hostLevels = levels(host);
        for (int i = 0, s = host.size(); i < s; i++) {
            components(host.term(i), hostLevels, components, nar);
        }
        return components;
    }

//    final static int levelBoost = Op.or(Op.EQUIV, Op.CONJ, Op.IMPL);

    private static int levels(@NotNull Compound host) {
        switch (host.op()) {
            case PROD:
            case SETe:
            case SETi:
            case DIFFe:
            case DIFFi:
            case SECTi:
            case SECTe:
            case IMGe:
            case IMGi:
                return 1;

            case INH:
                return 2;
            case SIM:
                return 2;

            case IMPL:
            case EQUI:
                return (host.vars() > 0) ? 3 : 2;
            case CONJ: {

                int s = host.size();
                int vars = host.vars();
                if (s <= Param.MAX_CONJ_SIZE_FOR_LAYER2_TEMPLATES) {
                    return (vars > 0) ? 3 : 2;
                } else {
                    return 2;
                    //return (vars > 0) ? 2 : 1; //prevent long conjunctions from creating excessive templates
                }
            }

            default:
                throw new UnsupportedOperationException("unhandled operator type: " + host.op());
        }
    }


    /**
     * determines whether to grow a 1st-level termlink to a subterm
     */
    protected static void components(@NotNull Term t, int level, @NotNull Collection<Term> target, @NotNull NAR nar) {

        t = t.unneg();

        if (t instanceof Atomic) {

            target.add(t); //any case when these should not be added?

        } else {

            Term u = t;
            t = nar.concepts.conceptualizable(t, true);
            if (t!=null) {
                if (!target.add(t))
                    return; //already added
            } else {
                t = u;
            }

            level--;

            if ((level > 0 && t instanceof Compound)) {
                Compound cct = (Compound) t;
                for (int i = 0, ii = cct.size(); i < ii; i++) {
                    components(cct.term(i), level, target, nar);
                }
            }

        }
    }

//    /** builds termlink templates with linear weighting according to their occurrence proportion of total subterms */
//    public static @NotNull List<TermTemplate> buildLinear(@NotNull Compound c, @NotNull NAR nar) {
//        return linearWeightedTermLinks(components(c, nar));
//    }

//    @NotNull
//    public static List<TermTemplate> linearWeightedTermLinks(@NotNull HashBag<Termed> s) {
//        float total = s.size();
//        List<TermTemplate> x = newArrayList(s.sizeDistinct());
//        s.forEachWithOccurrences((t,n) ->
//            x.add(new TermTemplate(t,n/total))
//        );
//        return x;
//    }

//    /** builds termlink templates with logarithmic weighting according to their occurrence proportion of total subterms
//     *  NOT TESTED may not be good
//     * */
//    public static @NotNull List<TermTemplate> buildLog(@NotNull Compound c, @NotNull NAR nar) {
//
//        HashBag<Termed> s = components(c, nar);
//
//
//        List<TermTemplate> x;
//
//        int unique = s.sizeDistinct();
//        if ((unique > 1) && (s.size()!=unique)) {
//            ObjectFloatHashMap<Termed> w = new ObjectFloatHashMap<>(unique);
//
//            s.forEachWithOccurrences((t, n) ->
//                    w.put(t, (float) Math.log1p(n))
//            );
//
//            //normalize the vector
//            float sum = (float) w.sum();
//            x = newArrayList(unique);
//            w.forEachKeyValue((t, n) -> x.add(new TermTemplate(t, n / sum)));
//        } else {
//            x = linearWeightedTermLinks(s);
//        }
//
//        return x;
//    }

    //static final int NegationOrConjunction = Op.or(Op.CONJUNCTION, Op.NEGATE);

//    /**
//     * Collect TermLink templates into a list, go down one level except in
//     * special cases
//     * <p>
//     *
//     * @param t          The CompoundTerm for which to build links
//     * @param components set of components being accumulated, to avoid duplicates
//     */
//    private static void visitComponents(@NotNull Compound t, @NotNull Collection<Termed> components, @NotNull NAR nar) {
//
//        ///** add self link for structural transform: */
//        //components.add(t);
//
//        Op tOp = t.op();
//        boolean tEquivalence = tOp == Op.EQUIV;
//        boolean tImplication = tOp == Op.IMPLICATION;
//
//
//        int ni = t.size();
//        for (int i = 0; i < ni; i++) {
//
//            Concept ti = growComponent(t.term(i), 0, nar, components);
//            if (ti == null)
//                continue;
//
//            //if ((tEquivalence || (tImplication && (i == 0))) && ((ti instanceof Conjunction) || (ti instanceof Negation))) {
//
//            if ((tEquivalence || (tImplication && (i == 0))) &&
//                (ti.term().isAnyOf(NegationOrConjunction))) {
//
//                visitComponents((Compound) ti, components, nar);
//
//            } else if (ti instanceof Compound) {
//
//                Compound cti = (Compound) ti;
//                for (int j = 0, nj = cti.size(); j < nj; j++) {
//
//                    Concept tj = growComponent(cti.term(j), 1, nar, components);
//                    if (tj instanceof Compound) {
//
//                        Compound ctj = (Compound) tj;
//                        for (int k = 0, nk = ctj.size(); k < nk; k++) {
//
//                            growComponent(ctj.term(k), 2, nar, components);
//
//                        }
//                    }
//
//
//                }
//            }
//
//
//        }
//    }
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
