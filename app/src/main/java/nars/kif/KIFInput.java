/*
 * Copyright (C) 2014 me
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.kif;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.MapSubst;
import nars.term.var.Variable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Twin;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static nars.Op.VAR_INDEP;
import static nars.rdfowl.NQuadsRDF.equi;
import static nars.term.Terms.compoundOrNull;

/**
 * http://sigmakee.cvs.sourceforge.net/viewvc/sigmakee/sigma/suo-kif.pdf
 * http://sigma-01.cim3.net:8080/sigma/Browse.jsp?kb=SUMO&lang=EnglishLanguage&flang=SUO-KIF&term=subclass
 *
 * @author me
 */
public class KIFInput implements Runnable {

    private final KIF kif;


    private final Iterator<Formula> formulaIterator;
    private final NAR nar;

    private final PrintStream output;

    private final boolean includeSubclass = true;
    private final boolean includeInstance = true;
    private final boolean includeRelatedInternalConcept = true;
    private final boolean includeDisjoint = true;
    private final boolean includeDoc = false;

    public KIFInput(NAR nar, String kifPath) throws Exception {

        this.nar = nar;

        kif = new KIF(kifPath);
        formulaIterator = kif.getFormulas().iterator();

//        this.output = new PrintStream(new FileOutputStream(
//                //"/tmp/kif.nal"
//            "/home/me/s/logic/src/main/java/spimedb/logic/sumo_merged.kif.nal"
//        ));
        this.output = System.out;
    }

    public void start() {
        new Thread(this).start();
    }


    @Override
    public void run() {
        Set<Compound> beliefs = new TreeSet();
        while (formulaIterator.hasNext()) {

            Formula x = formulaIterator.next();
            if (x == null) {
                break;
            }

            Compound y = compoundOrNull(formulaToTerm(x));

            if (y != null)
                beliefs.add(y);

            //  => Implies
            //  <=> Equivalance
                /*Unknown operators: {=>=466, rangeSubclass=5, inverse=1, relatedInternalConcept=7, documentation=128, range=29, exhaustiveAttribute=1, trichotomizingOn=4, subrelation=22, not=2, partition=12, contraryAttribute=1, subAttribute=2, disjoint=5, domain=102, disjointDecomposition=2, domainSubclass=9, <=>=70}*/
        }


        //nar.input( beliefs.stream().map(x -> task(x)) );

//        long[] stamp = { new Random().nextLong() };
        for (Compound x : beliefs) {
            //output.println(x + ".");

//            try {
//                nar.input("$0.01$ " + x + ".");
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
        }

        //nar.believe(y);
    }

    public Term formulaToTerm(String sx) {
        sx = sx.replace("?", "#"); //query var to indepvar HACK

        Formula f = Formula.the(sx);
        if (f != null)
            return formulaToTerm(f);
        else
            try {
                return $.$(sx);
            } catch (Narsese.NarseseException e) {
                return $.quote(sx);
            }
    }

    public Term formulaToTerm(Formula x) {
        String root = x.car(); //root operate

        List<String> sargs = IntStream.range(1, x.listLength()).mapToObj(x::getArgument).collect(Collectors.toList());
        List<Term> args = sargs != null ? sargs.stream().map(this::formulaToTerm).collect(Collectors.toList()) : Collections.emptyList();

        if (args.isEmpty())
            return formulaToTerm(x.car());

        /**
         *
         *
         * https://github.com/opencog/opencog/blob/04db8e557a2d67da9025fe455095d2cda0261ea7/opencog/python/sumo/sumo.py
         * def special_link_type(predicate):
         mapping = {
         '=>':types.ImplicationLink,
         '<=>':types.EquivalenceLink,
         'and':types.AndLink,
         'or':types.OrLink,
         'not':types.NotLink,
         'instance':types.MemberLink,
         # This might break some of the formal precision of SUMO, but who cares
         'attribute':types.InheritanceLink,
         'member':types.MemberLink,
         'subclass':types.InheritanceLink,
         'exists':types.ExistsLink,
         'forall':types.ForAllLink,
         'causes':types.PredictiveImplicationLink
         *
         */

        Term y = null;
        switch (root) {
            case "subrelation":
            case "subclass":
                if (includeSubclass) {
                    if (args.size() != 2) {
                        System.err.println("subclass expects 2 arguments");
                    } else {
                        try {
                            y = $.$("(" + args.get(0) + " --> " + args.get(1) + ")");
                        } catch (Narsese.NarseseException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case "instance":
                if (includeInstance) {
                    if (args.size() != 2) {
                        System.err.println("instance expects 2 arguments");
                    } else {
                        try {
                            y = compoundOrNull(Narsese.term("(" + args.get(0) + " --> [" + args.get(1) + "])", false));
                        } catch (Narsese.NarseseException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case "relatedInternalConcept":
                    /*(documentation relatedInternalConcept EnglishLanguage "Means that the two arguments are related concepts within the SUMO, i.e. there is a significant similarity of meaning between them. To indicate a meaning relation between a SUMO concept and a concept from another source, use the Predicate relatedExternalConcept.")            */
                if (includeRelatedInternalConcept) {
                    if (args.size() != 2) {
                        System.err.println("relatedInternalConcept expects 2 arguments");
                    } else {
                        try {
                            y = $.$("(" + args.get(0) + " <-> " + args.get(1) + ").");
                        } catch (Narsese.NarseseException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;

            case "equal":
                y = $.func("equal", args.get(0), args.get(1));
                break;
            case "disjointRelation":
            case "disjoint":
                if (includeDisjoint) {
                    try {
                        y = $.$("(||," + args.get(0) + "," + args.get(1) + ").");
                    } catch (Narsese.NarseseException e) {
                        e.printStackTrace();
                    }
                }
                break;
//                case "disjointRelation":
//                    if (includeDisjoint) {
//                        nar.input("(||," + args.get(0) + "," + args.get(1) + ").");
//                    }
//                    break;
//                case "subrelation":
//                    //for now, use similarity+inheritance but more clear expression is possible
//                    if (includeSubrelation) {
//                        //emit("(" + a.get(0) + " <-> " + a.get(1) + ").");
//                        nar.input("(" + args.get(0) + " --> " + args.get(1) + ").");
//                    }
//                    break;
//            case "forall":
//                y = args.get(1)
            case "exists":
                y = compoundOrNull(args.get(1)); //skip over the first parameter, since depvar is inherently existential
                break;
            case "=>":
                y = impl(args.get(0), args.get(1), true);
                break;
            case "<=>":
                y = impl(args.get(0), args.get(1), false);
                break;

            case "domain":
                //TODO use the same format as Range, converting quantity > 1 to repeats in an argument list
                if (args.size() >= 3) {
                    Term subj = (args.get(0));
                    Term quantity = (args.get(1));
                    Term type = (args.get(2));
                    try {
                        y = $.func("domain", subj, $.p(type, quantity));
                    } catch (Exception ignored) {

                    }

                }
                break;
            case "range":
                if (args.size() == 2) {
                    Term subj = args.get(0);
                    Term range = args.get(1);
                    Variable rr = nextVar(VAR_INDEP);
                    y = $.impl($.inh($.p( rr ), subj), $.inh(rr, range));
                } else {
                    System.err.println("unexpected range format");
                }
                break;
            case "contraryAttribute":
                if (args.size() >= 2) {
                    Term a = args.get(0);
                    Term b = args.get(1);
                    Variable v0 = nextVar(VAR_INDEP);
                    y = equi($.prop(v0, a), $.prop(v0, b).neg());
                }
                break;
            case "documentation":
                if (includeDoc) {
                    if (args.size() >= 2) {
                        Term subj = args.get(0);
                        Term lang = args.get(1);
                        Term desc = $.quote(args.get(2));
                        try {
                            y = $.inh($.p(subj, desc), lang);
                        } catch (Exception e) {
                            //e.printStackTrace();
                            y = null;
                        }
                    }
                }
                break;
            default:
                //System.out.println("unknown: " + x);
                break;
        }

        if (y == null) {

            if (x.car().equals("documentation") && !includeDoc)
                return null;

            Term z = formulaToTerm(x.car());
            if (args.isEmpty())
                return z;
            try {

                if (z != null) {
                    switch (z.toString()) {
                        case "and":
                            y = $.conj(args.toArray(new Term[args.size()]));
                            break;
                        case "or":
                            y = $.disj(args.toArray(new Term[args.size()]));
                            break;
                        case "not":
                            y = args.get(0).neg();
                            break;
                        default:
                            y = $.inh($.p(args), z); //HACK
                            break;
                    }

                }
            } catch (Exception e) {
                return null;
            }

        }

        return y;
    }

    private Variable nextVar(Op v) {
        return $.v(v, nextVar());
    }

    private final AtomicInteger serial = new AtomicInteger(0);

    private String nextVar() {
        return Integer.toString(Math.abs(serial.incrementAndGet()), 36);
    }

    public final Set<Twin<Term>> impl = new HashSet();

    public Compound impl(Term conditionTerm, Term actionTerm, boolean implOrEquiv) {
        MutableSet<Term> conditionVars = new UnifiedSet();
        ((Compound)conditionTerm).recurseTermsToSet(Op.VariableBits, conditionVars, true);
        MutableSet<Term> actionVars = new UnifiedSet();
        ((Compound)actionTerm).recurseTermsToSet(Op.VariableBits, actionVars, true);

        MutableSet<Term> common = conditionVars.intersect(actionVars);
        Map<Term,Term> remap = new HashMap();
        common.forEach(t -> {
            remap.put( t, $.v(
                    //Op.VAR_INDEP,
                    //Op.VAR_QUERY,
                    Op.VAR_PATTERN,
                    "" +
                            "_" + t.toString().substring(1)));
        });

        conditionTerm = conditionTerm.replace(remap);
        if (conditionTerm == null)
            return null;

        actionTerm = actionTerm.replace(remap);
        if (actionTerm == null)
            return null;

        try {
            impl.add(Tuples.twin(conditionTerm, actionTerm));
            if (!implOrEquiv) {
                impl.add(Tuples.twin(actionTerm, conditionTerm)); //reverse
            }

            return
                    implOrEquiv ?
                            $.impl(conditionTerm, actionTerm) :
                            (Compound) equi(conditionTerm, actionTerm)
                    ;
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
        return null;
    }


//    public static void main(String[] args) throws Exception {
//        Param.DEBUG = true;
//
//
//
//
//
//
//        Default e = new Default();
//
//        NAR d = new Terminal();
//        KIFInput k = new KIFInput(d, "/home/me/sumo/Merge.kif");
//        k.run();
//
//        //(($_#AGENT,#OBJECT)-->needs)==>($_#AGENT,#OBJECT)-->wants)).
//        //String rules = "((%AGENT,%OBJECT)-->needs), %X |- ((%AGENT,%OBJECT)-->wants), (Belief:Identity)\n";
//
//
//        TrieDeriver miniDeriver =
//                //new TrieDeriver(PremiseRuleSet.rules(false, "nal6.nal"));
//                TrieDeriver.get(new PremiseRuleSet(
//                        k.impl.parallelStream().map(tt -> {
//                            try {
//                                return PremiseRuleSet.parse(tt.getOne() + ", () |- " + tt.getTwo() + ", (Belief:Identity)\n");
//                            } catch (Exception e1) {
//                                //e1.printStackTrace();
//                                return null;
//                            }
//                        }).filter(Objects::nonNull).toArray(PremiseRule[]::new)
//                ) );
//
//
//        miniDeriver.print(System.out);
//
//        //d.clear();
//        e.log();
//
//        PreferSimpleAndPolarized budgeting = new PreferSimpleAndPolarized();
//
//        e.onTask(t -> {
//           if (t.isInput()) {
//               //d.forEachTask(b -> {
//                   miniDeriver.test(new Derivation(
//                           e,
//                           budgeting,
//                           Param.UnificationStackMax
//                   ) {
//                       @Override
//                       public void derive(Task x) {
//                           e.input(x);
//                       }
//                   }.restartC(new Premise( t, Terms.ZeroProduct, null, 1f), Param.UnificationTTLMax));
//               //});
//           }
//        });
//        e.input("[Physical]:X.");
//        e.input("[Atom]:Y.");
//        e.input("[Electron]:E.");
//        e.input("[Proton]:P.");
//        e.input("contains(X,Y).");
//        e.input("([Integer]:1 && [Integer]:3).");
//        e.input("starts(A,B).");
//        e.input("[GovernmentFn]:A.");
//        e.input("[WealthFn]:B.");
//        e.run(500);
////        d.conceptsActive().forEach(System.out::println);
//        //d.concept("[Phrase]").print();
//
//    }
}
