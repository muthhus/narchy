/*
 * Here comes the text of your license
 * Each line should be prefixed with  *
 */
package nars.concept;

import nars.Global;
import nars.NAR;
import nars.Premise;
import nars.Symbols;
import nars.bag.BLink;
import nars.budget.Budget;
import nars.nal.meta.PremiseMatch;
import nars.nal.op.Derive;
import nars.task.DerivedTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Stamp;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.Tense.*;
import static nars.truth.TruthFunctions.eternalize;

/**
 * Firing a concept (reasoning event). Derives new Tasks via reasoning rules
 * <p>
 * Concept
 * Task
 * TermLinks
 */
abstract public class ConceptProcess implements Premise {


    public final NAR nar;
    public final BLink<? extends Task> taskLink;
    public final BLink<? extends Concept> conceptLink;
    public final BLink<? extends Termed> termLink;

    @Nullable
    private final Task belief;
    private final boolean cyclic;

    @Deprecated
    //HACK temporary workspace variable, pertains only to current derivation of which many may occurr for this premise (re-used). TODO find a cleaner way
    transient public long occ;


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

    @NotNull
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
    @NotNull
    public final Compound temporalize(@NotNull Compound derived, @NotNull Term cp, @NotNull PremiseMatch p, @NotNull Derive d) {


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

                long to = task().occurrence();
                long bo = belief!=null ? belief().occurrence() : ETERNAL;

                int occDiff = (to!=ETERNAL && bo!=ETERNAL) ? (int) (bo - to) : 0;

                if (td == ITERNAL && bd == ITERNAL)
                {

                    long aTask = tp.subtermTime(ca, td);
                    long aBelief = bp.subtermTime(ca, bd);
                    long bTask = tp.subtermTime(cb, td);
                    long bBelief = bp.subtermTime(cb, bd);

                    if (belief() != null) {


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
                            //t = (int) (task().occurrence() - belief().occurrence());
                            t = occDiff;
                            if (reversed) occ -= t;
                            else occ += t;

                        } else if (aTask == ETERNAL && aBelief != ETERNAL &&
                                bBelief == ETERNAL && bTask != ETERNAL) {
                            //reverse: belief -> task
                            t = -occDiff;
                            //t = (int) (belief().occurrence() - task().occurrence());
                            //t = (int) (task().occurrence() - belief().occurrence());

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
                        if (timeOfDerivedInTask!=ETERNAL) {
                            occ += timeOfDerivedInTask;
                        } else {
                            //??
                        }
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



    /** part 2 */
    public void derive(@NotNull Termed<Compound> c, @Nullable Truth truth, Budget budget, long now, long occ, @NotNull PremiseMatch p, @NotNull Derive d) {

        char punct = p.punct.get();

        Task belief = belief();


        boolean derivedTemporal = occ != ETERNAL;

        Task derived = newDerivedTask(c, punct)
                .truth(truth)
                .budget(budget) // copied in, not shared
                .time(now, occ)
                .parent(task(), belief /* null if single */)
                .anticipate(derivedTemporal && d.anticipate)
                .log( Global.DEBUG ? d.rule : "Derived");

        if (!complete(derived))
            return;

        //--------- TASK WAS DERIVED if it reaches here

        if (derivedTemporal && (truth != null) && d.eternalize) {

            complete(newDerivedTask(c, punct)
                    .truth(
                            truth.freq(),
                            eternalize(truth.conf())
                    )

                    .time(now, ETERNAL)

                    .budget(budget) // copied in, not shared
                    .budgetCompoundForward(this)

                    .parent(derived)  //this is lighter weight and potentially easier on GC than: parent(task, belief)

                    .log("Immediaternalized") //Immediate Eternalization

            );

        }

    }

    @NotNull
    public DerivedTask newDerivedTask(@NotNull Termed<Compound> c, char punct) {
        return new DerivedTask(c, punct, this);
    }

    private final boolean complete(Task derived) {

        //pre-normalize to avoid discovering invalidity after having consumed space while in the input queue
        derived = derived.normalize(memory());
        if (derived != null) {

            //if (Global.DEBUG) {
            if (task().equals(derived))
                return false;
                //throw new RuntimeException("derivation same as task");
            if (belief() != null && belief().equals(derived))
                return false;
                //throw new RuntimeException("derivation same as belief");
            //}

            accept(derived);
            return true;
        }
        return false;
    }


    /** when a derivation is accepted, this is called  */
    abstract protected void accept(Task derivation);

    /** after a derivation has completed, commit is called allowing it to process anything collected */
    abstract protected void commit();

    public final void run(@NotNull PremiseMatch matcher) {
        matcher.start(this);
        commit();
    }

    public boolean hasTemporality() {
        if (task().term().t()!=ITERNAL) return true;
        @Nullable Task b = belief();
        if (b == null) return false;
        return b.term().t()!=ITERNAL;
    }
}
