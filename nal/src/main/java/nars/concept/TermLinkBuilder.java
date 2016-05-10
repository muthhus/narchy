package nars.concept;

import com.gs.collections.impl.bag.mutable.HashBag;
import com.gs.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import nars.NAR;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

import static nars.Global.newArrayList;


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

    @NotNull public static HashBag<Termed> components(@NotNull Compound host, @NotNull NAR nar) {
        HashBag<Termed> components = new HashBag(host.volume());

        ///** add self link for structural transform: */
        //components.add(t);

        Op tOp = host.op();
        boolean tEquivalence = tOp == Op.EQUIV;
        boolean tImplication = tOp == Op.IMPLICATION;


        int ni = host.size();
        for (int i = 0; i < ni; i++) {

            Concept ti = growComponent(host.term(i), 0, nar, components);
            if (ti == null)
                continue;

            //if ((tEquivalence || (tImplication && (i == 0))) && ((ti instanceof Conjunction) || (ti instanceof Negation))) {

            if ((tEquivalence || (tImplication && (i == 0))) &&
                (ti.term().isAnyOf(NegationOrConjunction))) {

                visitComponents((Compound) ti, components, nar);

            } else if (ti instanceof Compound) {

                Compound cti = (Compound) ti;
                for (int j = 0, nj = cti.size(); j < nj; j++) {

                    Concept tj = growComponent(cti.term(j), 1, nar, components);
                    if (tj instanceof Compound) {

                        Compound ctj = (Compound) tj;
                        for (int k = 0, nk = ctj.size(); k < nk; k++) {

                            growComponent(ctj.term(k), 2, nar, components);

                        }
                    }


                }
            }


        }
        return components;
    }

    /** builds termlink templates with linear weighting according to their occurrence proportion of total subterms */
    public static @NotNull List<TermTemplate> buildLinear(@NotNull Compound c, @NotNull NAR nar) {
        return linearWeightedTermLinks(components(c, nar));
    }

    @NotNull
    public static List<TermTemplate> linearWeightedTermLinks(@NotNull HashBag<Termed> s) {
        float total = s.size();
        List<TermTemplate> x = newArrayList(s.sizeDistinct());
        s.forEachWithOccurrences((t,n) ->
            x.add(new TermTemplate(t,n/total))
        );
        return x;
    }

    /** builds termlink templates with logarithmic weighting according to their occurrence proportion of total subterms */
    public static @NotNull List<TermTemplate> buildLog(@NotNull Compound c, @NotNull NAR nar) {

        HashBag<Termed> s = components(c, nar);


        List<TermTemplate> x;

        int unique = s.sizeDistinct();
        if ((unique > 1) && (s.size()!=unique)) {
            ObjectFloatHashMap<Termed> w = new ObjectFloatHashMap<>(unique);

            s.forEachWithOccurrences((t, n) ->
                    w.put(t, (float) Math.log1p(n))
            );

            //normalize the vector
            float sum = (float) w.sum();
            x = newArrayList(unique);
            w.forEachKeyValue((t, n) -> x.add(new TermTemplate(t, n / sum)));
        } else {
            x = linearWeightedTermLinks(s);
        }

        return x;
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
    private static void visitComponents(@NotNull Compound t, @NotNull Collection<Termed> components, @NotNull NAR nar) {

        ///** add self link for structural transform: */
        //components.add(t);

        Op tOp = t.op();
        boolean tEquivalence = tOp == Op.EQUIV;
        boolean tImplication = tOp == Op.IMPLICATION;


        int ni = t.size();
        for (int i = 0; i < ni; i++) {

            Concept ti = growComponent(t.term(i), 0, nar, components);
            if (ti == null)
                continue;

            //if ((tEquivalence || (tImplication && (i == 0))) && ((ti instanceof Conjunction) || (ti instanceof Negation))) {

            if ((tEquivalence || (tImplication && (i == 0))) &&
                (ti.term().isAnyOf(NegationOrConjunction))) {

                visitComponents((Compound) ti, components, nar);

            } else if (ti instanceof Compound) {

                Compound cti = (Compound) ti;
                for (int j = 0, nj = cti.size(); j < nj; j++) {

                    Concept tj = growComponent(cti.term(j), 1, nar, components);
                    if (tj instanceof Compound) {

                        Compound ctj = (Compound) tj;
                        for (int k = 0, nk = ctj.size(); k < nk; k++) {

                            growComponent(ctj.term(k), 2, nar, components);

                        }
                    }


                }
            }


        }
    }


    /**
     * determines whether to grow a 1st-level termlink to a subterm
     */
    protected static Concept growComponent(@NotNull Term t, int level, @NotNull NAR nar, @NotNull Collection<Termed> target) {
        if (t instanceof Variable)
            return null;

        Concept ct = nar.concept(t, true);
        if (ct == null) {
            return null;
        } else {
            target.add(ct);//t = ct.term());
            return ct;
        }
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
