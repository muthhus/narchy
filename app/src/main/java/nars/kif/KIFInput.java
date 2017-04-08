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
import nars.nar.Default;
import nars.nar.Terminal;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;

import java.util.Iterator;
import java.util.List;

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

    private boolean includeSubclass = true;
    private boolean includeInstance = true;
    private boolean includeRelatedInternalConcept = true;
    private boolean includeDisjoint = true;
    private boolean includeSubrelation = true;

    public KIFInput(NAR nar, String kifPath) throws Exception {

        this.nar = nar;

        kif = new KIF(kifPath);
        formulaIterator = kif.getFormulas().iterator();

    }

    public void setIncludeDisjoint(boolean includeDisjoint) {
        this.includeDisjoint = includeDisjoint;
    }

    public void setIncludeRelatedInternalConcept(boolean includeRelatedInternalConcept) {
        this.includeRelatedInternalConcept = includeRelatedInternalConcept;
    }

    public void setIncludeInstance(boolean includeInstance) {
        this.includeInstance = includeInstance;
    }

    public void setIncludeSubclass(boolean includeSubclass) {
        this.includeSubclass = includeSubclass;
    }

    public void setIncludeSubrelation(boolean includeSubrelation) {
        this.includeSubrelation = includeSubrelation;

    }

    public void start() {
        new Thread(this).start();
    }


    @Override
    public void run() {
        while (formulaIterator.hasNext()) {

            Formula x = formulaIterator.next();
            if (x == null) {
                break;
            }

            Compound y = formulaToTerm(x);

            if (y!=null)
                nar.believe(y);

            //  => Implies
            //  <=> Equivalance
                /*Unknown operators: {=>=466, rangeSubclass=5, inverse=1, relatedInternalConcept=7, documentation=128, range=29, exhaustiveAttribute=1, trichotomizingOn=4, subrelation=22, not=2, partition=12, contraryAttribute=1, subAttribute=2, disjoint=5, domain=102, disjointDecomposition=2, domainSubclass=9, <=>=70}*/
        }

    }

    public Compound formulaToTerm(Formula x) {
        String root = x.car(); //root operate

        List<String> args = x.argumentsToArrayList(1);

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

        Compound y = null;
        switch (root) {
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
                            y = $.$("({" + args.get(0) + "} --> " + args.get(1) + ")");
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
            case "disjoint":
                //"(||," <term> {","<term>} ")"      // disjunction
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
                case "=>":
                    Formula condition = x.cddrAsFormula();
                    if (condition!=null) {
                        Compound conditionTerm = formulaToTerm(condition);
                        if (conditionTerm!=null) {
                            Formula action = x.cddrAsFormula();
                            if (action != null) {
                                Compound actionTerm = formulaToTerm(action);
                                if (actionTerm!=null) {
                                    y = $.impl(conditionTerm, actionTerm);
                                }
                            }
                        }
                    }


                    break;

//            case "domain":
//                Term subj = $.the(args.get(0));
//                Term quantity = $.the(args.get(0));
//                Term type = $.the(args.get(0));
//                break;
            case "contraryAttribute":
                if (args!=null && args.size() >=2) {
                    Term a = $.the(args.get(0));
                    Term b = $.the(args.get(1));
                    y = (Compound) $.equi($.prop($.varIndep(0), a), $.neg($.prop($.varIndep(0), b)));
                }
                break;
            case "documentation":
                if (args!=null && args.size() >=2) {
                    Term subj = $.the(args.get(0));
                    Atomic lang = $.the(args.get(1));
                    Term desc = $.quote(args.get(2));
                    try {
                        y = $.func(lang, subj, desc);
                    } catch (Exception e) {
                        //e.printStackTrace();
                        y = null;
                    }
                }
                break;
            default:
                try {
                    String xy = x.toString();
                    y = $.$(xy); //HACK
                } catch (Narsese.NarseseException e) {
                    e.printStackTrace();
                }
                //System.out.println("unknown: " + x);
                break;
        }
        return y;
    }


    public static void main(String[] args) throws Exception {
        NAR d = new Terminal();
        d.log();
        KIFInput k = new KIFInput(d, "/home/me/sumo/Merge.kif");
        k.run();

    }
}
