/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.concept;

import nars.Global;
import nars.NAR;
import nars.Premise;
import nars.bag.BLink;
import nars.nal.meta.PremiseMatch;
import nars.nal.op.Derive;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Stamp;
import org.jetbrains.annotations.Nullable;

import static nars.nal.Tense.*;

/**
 * Firing a concept (reasoning event). Derives new Tasks via reasoning rules
 * <p>
 * Concept
 * Task
 * TermLinks
 */
public final class ConceptProcess implements Premise {


    public final NAR nar;
    public final BLink<? extends Task> taskLink;
    public final BLink<? extends Concept> conceptLink;
    public final BLink<? extends Termed> termLink;

    @Nullable
    private final Task belief;
    private final boolean cyclic;
    @Deprecated
    public long occ;


    public ConceptProcess(NAR nar, BLink<? extends Concept> conceptLink,
                          BLink<? extends Task> taskLink,
                          BLink<? extends Termed> termLink, @Nullable Task belief) {
        this.nar = nar;

        this.taskLink = taskLink;
        this.conceptLink = conceptLink;
        this.termLink = termLink;

        this.belief = belief;
        this.cyclic = Stamp.overlapping(task(), belief);
    }


    @Override
    public final Task task() {
        return taskLink.get();
    }

//    /**
//     * @return the current termLink aka BeliefLink
//     */
//    @Override
//    public final BagBudget<Termed> getTermLink() {
//        return termLink;
//    }

    public Concept concept() {
        return conceptLink.get();
    }


//    protected void beforeFinish(final long now) {
//
//        Memory m = nar.memory();
//        m.logic.TASKLINK_FIRE.hit();
//        m.emotion.busy(getTask(), this);
//
//    }

//    @Override
//    final protected Collection<Task> afterDerive(Collection<Task> c) {
//
//        final long now = nar.time();
//
//        beforeFinish(now);
//
//        return c;
//    }

    public final Termed beliefTerm() {
        Task x = belief();
        return x == null ? termLink.get() :
                x.term();
    }

    @Nullable
    @Override
    public final Task belief() {
        return belief;
    }

    @Override
    public final boolean isCyclic() {
        return cyclic;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(
                getClass().getSimpleName())
                .append('[').append(conceptLink).append(',')
                .append(taskLink).append(',')
                .append(termLink).append(',')
                .append(belief())
                .append(']')
                .toString();
    }

    @Override
    public final NAR nar() {
        return nar;
    }

    public int getMaxMatches() {
        final float min = Global.MIN_TERMUTATIONS_PER_MATCH, max = Global.MAX_TERMUTATIONS_PER_MATCH;
        return (int) Math.ceil(task().pri() * (max - min) + min);
    }

    /**
     * apply temporal characteristics to a newly derived term according to the premise's
     * @param cp Rule's (effective) Conclusion Term Pattern
     */
    public final Compound temporalize(Compound derived, Term cp, PremiseMatch p, Derive d) {


        Term tp = d.rule.getTaskTermPattern();
        Term bp = d.rule.getBeliefTermPattern();


        occ = occurrenceTarget(); //reset

        Compound tt = task().term();
        Term bb = beliefTerm().term(); // belief() != null ? belief().term() : null;

        int td = tt.t();
        int bd = bb instanceof Compound ? ((Compound)bb).t() : ITERNAL;

        int t = ITERNAL;

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

                if (td == ITERNAL && bd == ITERNAL) {

                    long aTask = tp.subtermTime(ca, td);
                    long aBelief = bp.subtermTime(ca, bd);
                    long bTask = tp.subtermTime(cb, td);
                    long bBelief = bp.subtermTime(cb, bd);

                    if (aTask != ETERNAL && aBelief == ETERNAL &&
                            bBelief != ETERNAL && bTask == ETERNAL) {
                        //forward: task -> belief
                        //t = (int) (task().occurrence() - belief().occurrence());
                        t = (int) (belief().occurrence() - task().occurrence());
                        occ += t;

                    }
                    else if (aTask == ETERNAL && aBelief != ETERNAL &&
                            bBelief == ETERNAL && bTask != ETERNAL) {
                        //reverse: belief -> task
                        t = (int) (task().occurrence() - belief().occurrence());
                        //t = (int) (belief().occurrence() - task().occurrence());
                        //t = (int) (task().occurrence() - belief().occurrence());
                        occ -= t;
                    } else {
                        //throw new RuntimeException("unhandled case");


                        //both ITERNAL
                        if(belief()!=null) {

                            long to = task().occurrence();
                            long bo = belief().occurrence();
                            if ((to != ETERNAL) && (bo != ETERNAL)) {
                                t = (int) (to - bo);
                                occ -= t;
                            }
                        }
                    }


                } else if (td == ITERNAL && bd!=ITERNAL) {
                    //belief has dt
                    t = bd;
                    //TODO align
                } else if (td != ITERNAL && bd==ITERNAL) {
                    //task has dt
                    t = td;
                    //TODO align
                }   else {
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

            if (belief()!=null) {
                //TODO cleanup simplify this is messy and confusing

                if (task().isEternal() && !belief().isEternal()) {
                    //find relative time of belief in the task, relative time of the conclusion, and subtract
                    //the occ (=belief time's)
                    long timeOfBeliefInTask = T.subtermTime(B,td);
                    long timeOfDerivedInTask = T.subtermTime(C,td);
                    if (timeOfDerivedInTask!=ETERNAL && timeOfBeliefInTask!=ETERNAL)
                        occ += (timeOfDerivedInTask - timeOfBeliefInTask);
                    else if (timeOfDerivedInTask!=ETERNAL)
                        occ += timeOfDerivedInTask;
                } else if (!task().isEternal() && belief().isEternal()) {
                    long timeOfTaskInBelief = B.subtermTime(T,bd);
                    long timeOfDerivedInBelief = B.subtermTime(C,bd);

                    if (timeOfTaskInBelief != ETERNAL && timeOfDerivedInBelief != ETERNAL)
                        occ += (timeOfDerivedInBelief - timeOfTaskInBelief);
                    else if (timeOfDerivedInBelief!=ETERNAL)
                        occ += timeOfDerivedInBelief;
                    else {
                        long timeOfDerivedInTask = T.subtermTime(C,td);
                        occ += timeOfDerivedInTask;
                    }
                } else if (!task().isEternal() && !belief().isEternal()) {
                    //throw new RuntimeException("ambiguous task or belief");

                    //long ot = T.subtermTime(C, td);
                    //long ob = B.subtermTime(C, bd);
                    //if (t!=ITERNAL)
                    //    occ -= t;
                }
            } else {

                if (!task().isEternal()) {
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

                        if (belief().occurrence() != task().occurrence()) { //why?
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

            /* reverse subterms if commutive and the terms are opposite the corresponding pattern */
            if (derived.op().isCommutative()) {
                if (!p.resolve(((Compound)cp).term(0)).equals(derived.term(0))) {
                    t = -t;
                }
            }

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



        return derived;
        //return nar.memory.index.transformRoot(derived, temporalize);
    }


//    /** max(tasktime, belieftime) */
//    public long getMaxOccurrenceTime() {
//        long occ= getTask().getOccurrenceTime();
//        Task b = getBelief();
//        if (b!=null) {
//            occ = Math.max(occ, b.getOccurrenceTime());
//        }
//        return occ;
//    }


    //    /** supplies at most 1 premise containing the pair of next tasklink and termlink into a premise */
//    public static Stream<Task> nextPremise(NAR nar, final Concept concept, float taskLinkForgetDurations, Function<ConceptProcess,Stream<Task>> proc) {
//
//        TaskLink taskLink = concept.getTaskLinks().forgetNext(taskLinkForgetDurations, nar.memory());
//        if (taskLink == null) return Stream.empty();
//
//        TermLink termLink = concept.getTermLinks().forgetNext(nar.memory().termLinkForgetDurations, nar.memory());
//        if (termLink == null) return Stream.empty();
//
//
//        return proc.apply(premise(nar, concept, taskLink, termLink));
//
//    }

//    public static ConceptProcess premise(NAR nar, Concept concept, TaskLink taskLink, TermLink termLink) {
////        if (Terms.equalSubTermsInRespectToImageAndProduct(taskLink.getTerm(), termLink.getTerm()))
////            return null;
//
////        if (taskLink.isDeleted())
////            throw new RuntimeException("tasklink null"); //bag should not have returned this
//
//    }


//    public abstract Stream<Task> derive(final Deriver p);

//    public static void forEachPremise(NAR nar, @Nullable final Concept concept, @Nullable TaskLink taskLink, int termLinks, float taskLinkForgetDurations, Consumer<ConceptProcess> proc) {
//        if (concept == null) return;
//
//        concept.updateLinks();
//
//        if (taskLink == null) {
//            taskLink = concept.getTaskLinks().forgetNext(taskLinkForgetDurations, concept.getMemory());
//            if (taskLink == null)
//                return;
//        }
//
//
//
//
//        proc.accept( new ConceptTaskLinkProcess(nar, concept, taskLink) );
//
//        if ((termLinks > 0) && (taskLink.type!=TermLink.TRANSFORM))
//            ConceptProcess.forEachPremise(nar, concept, taskLink,
//                    termLinks,
//                    proc
//            );
//    }

//    /** generates a set of termlink processes by sampling
//     * from a concept's TermLink bag
//     * @return how many processes generated
//     * */
//    public static int forEachPremise(NAR nar, Concept concept, TaskLink t, final int termlinksToReason, Consumer<ConceptProcess> proc) {
//
//        int numTermLinks = concept.getTermLinks().size();
//        if (numTermLinks == 0)
//            return 0;
//
//        TermLink[] termlinks = new TermLink[termlinksToReason];
//
//        //int remainingProcesses = Math.min(termlinksToReason, numTermLinks);
//
//        //while (remainingProcesses > 0) {
//
//            Arrays.fill(termlinks, null);
//
//            concept.getPremiseGenerator().nextTermLinks(concept, t, termlinks);
//
//            int created = 0;
//            for (TermLink tl : termlinks) {
//                if (tl == null) break;
//
//                proc.accept(
//                    new ConceptTaskTermLinkProcess(nar, concept, t, tl)
//                );
//                created++;
//            }
//
//
//          //  remainingProcesses--;
//
//
//        //}
//
//        /*if (remainingProcesses == 0) {
//            System.err.println(now + ": " + currentConcept + ": " + remainingProcesses + "/" + termLinksToFire + " firings over " + numTermLinks + " termlinks" + " " + currentTaskLink.getRecords() + " for TermLinks "
//                    //+ currentConcept.getTermLinks().values()
//            );
//            //currentConcept.taskLinks.printAll(System.out);
//        }*/
//
//        return created;
//
//    }

//    /** override-able filter for derivations which can be applied
//     * once the term and the truth value are known */
//    public boolean validJudgment(Term derivedTerm, Truth truth) {
//        return true;
//    }
//
//    /** override-able filter for derivations which can be applied
//     * once the term and the truth value are known */
//    public boolean validGoal(Term derivedTerm, Truth truth) {
//        return true;
//    }

}
