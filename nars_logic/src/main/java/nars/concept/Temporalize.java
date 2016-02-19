package nars.concept;

import nars.nal.meta.PremiseMatch;
import nars.nal.op.Derive;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.nal.Tense.ETERNAL;
import static nars.nal.Tense.ITERNAL;
import static nars.nal.Tense.TIMELESS;

/**
 * Strategies for solving temporal components of a derivation
 */
@FunctionalInterface
public interface Temporalize {

    /**
     *
     * @param derived raw resulting untemporalized derived term that may or may not need temporalized and/or occurrence shifted as part of a derived task
     * @param p current match context
     * @param d derivation rule being evaluated
     * @param occReturn holds the occurrence time as a return value for the callee to use in building the task
     * @return
     */
    Compound compute(@NotNull Compound derived, @NotNull PremiseMatch p, @NotNull Derive d, long[] occReturn);


    /**
     * "automatic" implementation of Temporalize, used by default. slow and wrong about 25..30% of the time sux needs rewritten or replaced
     * apply temporal characteristics to a newly derived term according to the premise's
     */
    Temporalize Auto = (derived, p, d, occReturn) -> {

        Term tp = d.rule.getTaskTermPattern();
        Term bp = d.rule.getBeliefTermPattern();

        ConceptProcess premise = p.premise;

        Task task = premise.task();
        Task belief = premise.belief();

        long occ = premise.occurrenceTarget(); //reset

        Compound tt = task.term();
        Term bb = premise.beliefTerm().term(); // belief() != null ? belief().term() : null;

        int td = tt.t();
        int bd = bb instanceof Compound ? ((Compound)bb).t() : ITERNAL;

        int t = ITERNAL;

        Term cp = d.conclusionPattern;

        if (derived.op().isTemporal() && cp.isCompound()) {

            Compound ccc = (Compound) cp;
            Term ca = ccc.term(0);

            //System.out.println(tt + " "  + bb);

        /* CASES:
            conclusion pattern size 1
                equal to task subterm
                equal to belief subterm
                unique term
            conclusion pattern size 2 (a, b)
                a equal to task subterm
                b equal to task subterm
                a equal to belief subterm
                b equal to belief subterm

         */
            int s = cp.size();
            if (s == 2) {
                Term cb = ccc.term(1);

                //chained relations
                if (td!=ITERNAL && bd!=ITERNAL && (tp.size() == 2) && (bp.size() == 2)) {
                    Compound tpp = (Compound) tp;
                    Compound bpp = (Compound) bp;

                    if (tpp.term(1).equals(bpp.term(0))) {
                        t = td + bd;

                        //chained inner
                        if (!cb.equals(bpp.term(1))) {
                            t = -t; //invert direction
                        }
                    } else if (tpp.term(0).equals(bpp.term(1))) {
                        //chain outer
                        t = td + bd; //?? CHECK
                    } else if (tpp.term(0).equals(bpp.term(0))) {
                        //common left
                        t = td - bd;
                    } else if (tpp.term(1).equals(bpp.term(1))) {
                        //common right
                        t = bd - td;
                    } else {
                        //throw new RuntimeException("unhandled case");
                        t = (bd + td) / 2; //???
                    }

                }

                long to = task.occurrence();
                long bo = belief!=null ? belief.occurrence() : ETERNAL;

                int occDiff = (to!=ETERNAL && bo!=ETERNAL) ? (int) (bo - to) : 0;

                if (td == ITERNAL && bd == ITERNAL)
                {

                    long aTask = tp.subtermTime(ca, td);
                    long aBelief = bp.subtermTime(ca, bd);
                    long bTask = tp.subtermTime(cb, td);
                    long bBelief = bp.subtermTime(cb, bd);

                    if (belief != null) {

                        boolean reversed = false;
                    /* reverse subterms if commutive and the terms are opposite the corresponding pattern */
                        if (derived.op().isCommutative()) {
                            if (!p.resolve(((Compound) cp).term(0)).equals(derived.term(0))) {
                                occDiff = -occDiff;
                                reversed = true;
                            }
                        }


                        if (aTask != ETERNAL && aBelief == ETERNAL &&
                                bBelief != ETERNAL && bTask == ETERNAL) {
                            //forward: task -> belief
                            //t = (int) (task.occurrence() - belief().occurrence());
                            t = occDiff;
                            if (reversed) occ -= t;
                            else occ += t;

                        } else if (aTask == ETERNAL && aBelief != ETERNAL &&
                                bBelief == ETERNAL && bTask != ETERNAL) {
                            //reverse: belief -> task
                            t = -occDiff;
                            //t = (int) (belief().occurrence() - task.occurrence());
                            //t = (int) (task.occurrence() - belief().occurrence());

                            if (!reversed) {
                                occ -= t;
                            } else {
                                occ += t;
                            }


                        } else {

                            //both ITERNAL

                            if ((to != ETERNAL) && (bo != ETERNAL)) {
                                t = occDiff;
                                if (reversed) occ -= t;
                                else occ += t;
                            }

                        }
                    }

                } else if (td == ITERNAL && bd!=ITERNAL) {
                    //belief has dt
                    t = bd;// + occDiff;
                    //TODO align
                } else if (td != ITERNAL && bd==ITERNAL) {
                    //task has dt
                    t = td + occDiff;
                    //occ += t; //TODO check this alignment

                }   else {
                    //t = occDiff;
                    //throw new RuntimeException("unhandled case");
                    //???
                    //t = (td+bd)/2;
                }
            }


//            if (t!=ITERNAL) {
//                long ot = taskPattern.subtermTime(cp, t);
//                long ob = beliefPattern.subtermTime(cp, t);
//
//                System.out.println(ot + " " + ob);
//
//            }

            //System.out.println(derived + " " + a + ":"+ aTask + "|" + aBelief + ", " + b + ":" + bTask + "|" + bBelief);


        }



        //apply occurrence shift
        if (occ > TIMELESS ) {

            Term T = p.resolve(tt);
            Term B = bb!=null ? p.resolve(bb) : null;
            Term C = derived;

            if (belief!=null) {
                //TODO cleanup simplify this is messy and confusing

                if (task.isEternal() && !belief.isEternal()) {
                    //find relative time of belief in the task, relative time of the conclusion, and subtract
                    //the occ (=belief time's)
                    long timeOfBeliefInTask = T.subtermTime(B,td);
                    long timeOfDerivedInTask = T.subtermTime(C,td);
                    if (timeOfDerivedInTask!=ETERNAL && timeOfBeliefInTask!=ETERNAL)
                        occ += (timeOfDerivedInTask - timeOfBeliefInTask);
                    else if (timeOfDerivedInTask!=ETERNAL)
                        occ += timeOfDerivedInTask;
                } else if (!task.isEternal() && belief.isEternal()) {
                    long timeOfTaskInBelief = B.subtermTime(T,bd);
                    long timeOfDerivedInBelief = B.subtermTime(C,bd);

                    if (timeOfTaskInBelief != ETERNAL && timeOfDerivedInBelief != ETERNAL)
                        occ += (timeOfDerivedInBelief - timeOfTaskInBelief);
                    else if (timeOfDerivedInBelief!=ETERNAL)
                        occ += timeOfDerivedInBelief;
                    else {
                        long timeOfDerivedInTask = T.subtermTime(C,td);
                        if (timeOfDerivedInTask!=ETERNAL) {
                            occ += timeOfDerivedInTask;
                        } else {
                            //??
                        }
                    }
                } else if (!task.isEternal() && !belief.isEternal()) {
                    //throw new RuntimeException("ambiguous task or belief");

                    //long ot = T.subtermTime(C, td);
                    //long ob = B.subtermTime(C, bd);
                    //if (t!=ITERNAL)
                    //    occ -= t;
                }
            } else {

                if (!task.isEternal()) {
                    long timeOfDerivedInTask = T.subtermTime(C, td);
                    if (timeOfDerivedInTask!=ETERNAL)
                        occ += timeOfDerivedInTask;
                } else {

                    long ot = tp.subtermTime(cp, td);
                    long ob = bp.subtermTime(cp, bd);

                    if (ot != ETERNAL) {
                        if (tp.isCompound()) {
                            Compound ctp = (Compound) tp;
                            if (ctp.term(0).equals(cp)) {
                                ot -= td;
                            }
                        }
                        occ += ot; //occ + ot;
                    } else if (ob != ETERNAL) {

                        if (belief.occurrence() != task.occurrence()) { //why?
                            if (bp.isCompound()) {
                                Compound cbp = (Compound) bp;
                                if (!cbp.term(1).equals(cp)) {
                                    ob -= bd;
                                }
                            }
                        }

                        occ += ob;

                    } else {
                        //neither, remain eternal
                        throw new RuntimeException("unhandled case");
                    }
                }
            }


        }
        //}
        //}

        if (t != ITERNAL) {
        /*derived = (Compound) p.premise.nar.memory.index.newTerm(derived.op(), derived.relation(),
                t, derived.subterms());*/

            derived = derived.t(t);

//            int nt = derived.t();
//            if (occ > TIMELESS) {
//                if (Math.signum(t) != Math.signum(nt)) {
//                    //re-align the occurrence
//                    occ -= t;
//                } else {
//                    occ -= nt;
//                }
//            }
        }


        occReturn[0] = occ;


        return derived;
        //return nar.memory.index.transformRoot(derived, temporalize);

    };
}
