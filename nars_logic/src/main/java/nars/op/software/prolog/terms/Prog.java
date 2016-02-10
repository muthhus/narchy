package nars.op.software.prolog.terms;

import nars.op.software.prolog.Prolog;
import org.jetbrains.annotations.NotNull;

/**
 * Basic toplevel Prolog Engine. Loads and executes Prolog
 * programs and can be extended to spawn threads executing on new Prolog Engine
 * objects as well as networking threads and
 * synced local and remote Linda transactions
 */
public class Prog extends Source implements Runnable {
    private final Prolog prolog;
    // CONSTRUCTORS

    /**
     * Creates a Prog starting execution with argument "goal"
     */
    public Prog(Prolog prolog, Clause goal, Prog parent) {
        super(parent);
        this.prolog = prolog;
        this.parent = parent;
        goal = goal.ccopy();
        this.trail = new Trail();
        this.orStack = new Trail();
        if (null != goal)
            orStack.add(nextUnfolder(goal));

    }

    // INSTANCE FIELDS

    private Trail trail;

    /**
     * Contains Unfolders that may produce answers.
     */
    private Trail orStack;

    private final Prog parent;

    public final Trail getTrail() {
        return trail;
    }

    public final Prog getParent() {
        return parent;
    }

    // CLASS FIELDS

    public static int tracing = 1;

    // INSTANCE METHODS

    /**
     * Here is where actual LD-resolution computation happens.
     * It consists of a chain of "unfolding" steps, possibly
     * involving backtracking, which is managed by the OrStack.
     */
    public PTerm getElement() {
        Trail stack = this.orStack;

        if (null == stack)
            return null;

        Clause answer = null;
        Unfolder I;
        while (null != (I = (Unfolder) stack.removeLastElseNull())) {
            answer = I.getAnswer();
            if (null != answer)
                break;
            Clause nextgoal = I.getElement();
            if (null != nextgoal) {
                if (I.notLastClause())
                    stack.add(I);
                else
                    I.stop();
                stack.add(nextUnfolder(nextgoal));
            }
        }
        PTerm head;
        if (null == answer) {
            head = null;
            stop();
        } else
            head = answer.head();
        return head;
    }

    @NotNull
    private Unfolder nextUnfolder(Clause nextgoal) {
        return new Unfolder(prolog, nextgoal, this);
    }

    public void stop() {
        Trail t = this.trail;
        if (null != t) {
            t.unwind(0);
            this.trail = null;
        }
        orStack = null;
    }

    /**
     * Computes a copy of the first solution X of Goal G.
     */

    public static PTerm firstSolution(Prolog prolog, PTerm X, PTerm G) {
        Prog p = new Prog(prolog, new Clause(X, G), null);
        PTerm a = ask_engine(p);
        if (a != null) {
            a = new Fun("the", a);
            p.stop();
        } else
            a = Const.NO;
        return a;
    }

    /**
     * asks a logic engine to return a solution
     */

    static public PTerm ask_engine(Prog p) {
        return p.getElement();
    }

    /**
     * usable for launching on a separate thread
     */
    public void run() {
        for (; ; ) {
            PTerm Answer = getElement();
            if (null == Answer)
                break;
        }
    }
}