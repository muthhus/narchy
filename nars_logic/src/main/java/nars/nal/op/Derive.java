package nars.nal.op;

import com.google.common.base.Joiner;
import nars.$;
import nars.Global;
import nars.Memory;
import nars.Op;
import nars.budget.Budget;
import nars.concept.ConceptProcess;
import nars.nal.meta.*;
import nars.nal.meta.match.EllipsisMatch;
import nars.nal.nal8.Operator;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.variable.Variable;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.Tense.ETERNAL;

/**
 * Handles matched derivation results
 * < (&&, postMatch1, postMatch2) ==> derive(term) >
 */
public class Derive extends AbstractLiteral implements ProcTerm {

    @NotNull
    public final String id;

    public final boolean anticipate;
    public final boolean eternalize;

    @NotNull
    public final PremiseRule rule;

    /** result pattern */
    @NotNull
    public final Term conclusionPattern;

    @NotNull private final BooleanCondition<PremiseMatch> postMatch; //TODO use AND condition

    /** whether this a single or double premise derivation; necessary in case premise
     * does have a belief but it was not involved in determining Truth */
    public final boolean beliefSingle, desireSingle;


    public Derive(PremiseRule rule, Term term, @NotNull BooleanCondition[] postMatch,
                  boolean beliefSingle, boolean desireSingle, boolean anticipate, boolean eternalize) {
        this.rule = rule;
        this.postMatch = (postMatch.length > 0) ? new AndCondition(postMatch) : BooleanCondition.TRUE;
        this.conclusionPattern = term;
        this.beliefSingle = beliefSingle;
        this.desireSingle = desireSingle;
        this.anticipate = anticipate;
        this.eternalize = eternalize;

        String i = "Derive:(" + term;
        if (eternalize && anticipate) {
            i += ", {eternalize,anticipate}";
        } else if (eternalize) {
            i += ", {eternalize}";
        } else if (anticipate) {
            i += ", {anticipate}";
        }


        if (postMatch.length > 0) {
            i += ", {" + Joiner.on(',').join(postMatch) + '}';
        }

        i += ")";
        this.id = i;
    }


    @NotNull
    @Override
    public String toString() {
        return id;
    }


    /** main entry point for derivation result handler.
     * @return true to allow the matcher to continue matching,
     * false to stop it */
    @Override public final void accept(@NotNull PremiseMatch m) {

        Term derivedTerm = m.resolve(conclusionPattern);

        if (derivedTerm == null)
            return;

        if ((derivedTerm instanceof EllipsisMatch)) {
            throw new RuntimeException("invalid ellipsis match: " + derivedTerm);
//            EllipsisMatch em = ((EllipsisMatch)derivedTerm);
//            if (em.size()!=1) {
//                throw new RuntimeException("invalid ellipsis match: " + em);
//            }
//            derivedTerm = em.term(0); //unwrap the item
        }

        if (ensureValidVolume(derivedTerm) && postMatch.booleanValueOf(m))
            derive(m, derivedTerm);


    }

    private static boolean ensureValidVolume(Term derivedTerm) {

        //HARD VOLUME LIMIT
        boolean tooLarge = derivedTerm.volume() > Global.COMPOUND_VOLUME_MAX;
        if (tooLarge) {

            if (Global.DEBUG) {
                //$.logger.error("Term volume overflow");
                /*c.forEach(x -> {
                    Terms.printRecursive(x, (String line) ->$.logger.error(line) );
                });*/

                String message = "Term volume overflow: " + derivedTerm;
                $.logger.error(message);
                System.exit(1);
                //throw new RuntimeException(message);
            }

            return false;

        }

        return true;

    }


    /** part 1 */
    private void derive(@NotNull PremiseMatch p, @Nullable Term t) {

        if (t.hasVarPattern()) {
            return;
        }

        ConceptProcess premise = p.premise;
        Memory mem = premise.memory();

        //get the normalized term to determine the budget (via it's complexity)
        //this way we can determine if the budget is insufficient
        //before conceptualizating in mem.taskConcept
        Termed tNorm = mem.index.normalized(t);

        //HACK why?
        if ((tNorm == null) || !tNorm.term().isCompound())
            return;

        Truth truth = p.truth.get();

        Budget budget = p.getBudget(truth, tNorm);
        if (budget == null)
            return;

        boolean p7 = mem.nal() >= 7;

        long now = mem.time();
        long occ;

        Compound ct = (Compound) tNorm.term();

        if (p7) {
            Term cp = this.conclusionPattern;

            if (Op.isOperation(cp) && p.transforms.containsKey( Operator.operator((Compound) cp) ) ) {
                //unwrap operation from conclusion pattern; the pattern we want is its first argument
                cp = Operator.opArgsArray((Compound) cp)[0];
            }

            ct = premise.temporalize(ct,
                    cp, p, this
            );

            occ = premise.occ;

        } else {
            occ = ETERNAL;
        }

        premise.derive(ct, truth, budget, now, occ, p, this);

    }






}
