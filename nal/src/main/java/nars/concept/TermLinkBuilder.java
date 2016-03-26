package nars.concept;

import com.gs.collections.impl.bag.mutable.HashBag;
import nars.Global;
import nars.NAR;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;


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

    public static @NotNull List<TermTemplate> build(@NotNull Compound host, @NotNull NAR nar) {
        //TODO use a MultiSet or Bag to count # of occurrences of components, in case there are repeats,
        //these should be weighted stronger. requires a new termlink template class like Pair<Term,Float> to include a weight

        //List<Termed> components = Global.newArrayList(0);
        HashBag<Termed> components = new HashBag(host.volume());

        visitComponents(host, components, nar);

        float total = components.size();
        List<TermTemplate> x = Global.newArrayList(components.sizeDistinct());
        components.forEachWithOccurrences((t,n) -> x.add(new TermTemplate(t,n/total)));
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

            Term ti = growComponent(t.term(i), 0, nar, components);
            if (ti == null)
                continue;

            //if ((tEquivalence || (tImplication && (i == 0))) && ((ti instanceof Conjunction) || (ti instanceof Negation))) {

            if ((tEquivalence || (tImplication && (i == 0))) &&
                (ti.isAnyOf(NegationOrConjunction))) {

                visitComponents((Compound) ti, components, nar);

            } else if (ti instanceof Compound) {
                Compound cti = (Compound) ti;


                int nj = cti.size();
                for (int j = 0; j < nj; j++) {

                    Term tj = growComponent(cti.term(j), 1, nar, components);
                    if (tj instanceof Compound) {
                        Compound ctj = (Compound) tj;

                        int nk = ctj.size();
                        for (int k = 0; k < nk; k++) {

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
    protected static Term growComponent(@NotNull Term t, int level, @NotNull NAR nar, @NotNull Collection<Termed> target) {
        Concept ct = nar.concept(t, true);
        if ((ct == null) || (ct instanceof Variable)) {
            return null;
        }
        else {
            target.add(t = ct.term());
            return t;
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
