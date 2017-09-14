package nars.concept;

import jcog.list.FasterList;
import nars.Op;
import nars.term.Term;
import nars.term.Termed;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static nars.Op.*;

public enum TermLinks {
    ;


    public static Collection<Termed> templates(Term term) {

        if (term.size() > 0) {

            Collection<Termed> templates;

            Set<Termed> tc =
                    //new UnifiedSet<>(id.volume() /* estimate */);
                    new HashSet<>(term.volume());

            TermLinks.addTemplates(term, tc, 1 + layers(term));

            int tcs = tc.size();

            if (tcs > 0)
                return new FasterList<>(tc.toArray(new Termed[tcs])); //store as list for compactness and fast iteration
            else
                return emptyList();
        } else {

            return List.of(term);
        }
    }

    /** recurses */
    static void addTemplates(Term root, Set<Termed> tc, int layersRemain) {

        Term b = root.unneg();

        Op o = b.op();
        switch (o) {
            //case VAR_DEP:
            //case VAR_INDEP:
            //case VAR_QUERY:
            //  break; //OK
            default:
                if (!o.conceptualizable)
                    return;
        }

        if (!tc.add(b))
            return; //already added

        if (b.size() == 0)
            return;

        if (--layersRemain <= 0) // || !b.op().conceptualizable || b.isAny(VAR_QUERY.bit | VAR_PATTERN.bit))
            return;

        int lb = 1 + layers(b);
        layersRemain = Math.min(lb, layersRemain);

        for (Term bb : b.subterms()) {

            addTemplates(bb, tc, layersRemain);

//            @Nullable Concept c = nar.conceptualize(b);
//
//            Iterable<? extends Termed> e = null;
//            if (c != null) {
////                    if (layersRemain > 0) {
//                e = c.subterms();
////                        if (e.size() == 0) {
////                            //System.out.println(c);
////                            //HACK TODO determine if good
////                            //c.termlinks().sample(ctpl.size(), (Consumer<PriReference<Term>>)(x->tc.add(x.get())));
////                        }
////                    }
//            } else /*if (!b.equals(id))*/ {
//
////                    if (layersRemain > 0) {
//                e = b.subterms();
////                        if (e.size() == 0) {
////                            //System.out.println(" ? " + e);
////                            //e.termlinks().sample(10, (Consumer<PriReference<Term>>)(x->tc.add(x.get())));
////                        }
////                    }
//
//
//            }

        }
    }

    static int layers(Term host) {
        switch (host.op()) {

            case SETe:
            case SETi:

//            case IMGe:
//            case IMGi:
//                return 1;

            case DIFFe:
            case DIFFi:
            case SECTi:
            case SECTe:
                return 1;

            case PROD:
                return 2;

            case CONJ:
                return 2;

            case SIM:
                return 2;

            case INH:
                return 3;

            case IMPL:
                return 3;


//                int s = host.size();
//                if (s <= Param.MAX_CONJ_SIZE_FOR_LAYER2_TEMPLATES) {
//                    int vars = host.vars();
//                    return (vars > 0) ? 3 : 2;
//                } else {
//                    return 2;
//                    //return (vars > 0) ? 2 : 1; //prevent long conjunctions from creating excessive templates
//                }


            default:
                throw new UnsupportedOperationException("unhandled operator type: " + host.op());


        }
    }
}
