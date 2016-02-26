package nars.op.sys.prolog.terms;

//!depends

import nars.op.sys.prolog.Prolog;
import nars.op.sys.prolog.io.IO;
import nars.op.sys.prolog.io.Parser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Datatype for a Prolog clause (H:-B) having a head H and a body b
 */
public class Clause extends Fun {
    /**
     * Builds a clause given its head and its body
     */
    public Clause(PTerm head, PTerm body) {
        super(":-", head, body);
    }

    public Clause(PTerm[] headBody) {
        this(headBody[0], headBody[1]);
    }

    /**
     * Constructs a clause by parsing its string representation. Note the
     * building of a dictionary of variables, allowing listing of the clause
     * with its original variable names.
     */
    public Clause(Prolog p, String s) {
        super(":-");
        Clause C = Parser.stringToClause(p, s);
        // IO.mes("CLAUSE:"+C.pprint()+"\nDICT:"+C.dict);
        this.args = C.args;
        this.dict = C.dict;
        this.ground = C.ground;
    }

    /**
     * Reads a goal as a clause containing a dummy header with all variables in
     * it
     */

    @NotNull
    public Clause toGoal() {
        Clause G = new Clause(varsOf(), head());
        G.dict = dict;
        G.checkIfGround();
        if (IO.trace())
            IO.trace("conversion from clause to goal ignores body of: "
                    + pprint());
        return G;
    }

    public static Clause goalFromString(Prolog p, @Nullable String line) {
        if (IO.trace())
            IO.trace("read string: <" + line + '>');

        if (null == line)
            line = PTerm.EOF.name;
        else if (0 == line.length())
            return null;

        Clause C = Parser.stringToClause(p, line);
        if (null == C) {
            if (IO.trace())
                IO.error("warning (null Clause):" + line);
            return null;
        }

        // IO.trace("got goal:\n"+C.toGoal()); //OK
        return C.toGoal();
    }

    /**
     * Detects that a clause is ground (i.e. has no variables)
     */
    final void checkIfGround() {
        ground = varsOf().arity() == 0;
    }

    /**
     * Variable dictionary
     */
    @Nullable
    public Map dict;

    /**
     * Remembers if a clause is ground.
     */
    public boolean ground;

    /**
     * File name and line where sources start and end (if applicable)
     */

    public String fname;

    public int begins_at;

    public int ends_at;

    public void setFile(@NotNull String fname, int begins_at, int ends_at) {
        this.fname = fname.intern();
        this.begins_at = begins_at;
        this.ends_at = ends_at;
    }

    /**
     * Checks if a Clause has been proven ground after beeing read in or
     * created.
     */
    final boolean provenGround() {
        return ground;
    }

    /**
     * Prints out a clause as Head:-Body
     */
    @NotNull
    private static String Clause2String(@NotNull Clause c) {
        PTerm h = c.head();
        PTerm t = c.body();
        if (t instanceof Conj)
            return h + ":-" + ((Conj) t).conjToString();
        return h + ":-" + t;
    }

    // uncomment if you want this to be the default toString
    // procedure - it might create read-back problems, though
    // public String toString() {
    // return Clause2String(this);
    // }

    /**
     * Pretty prints a clause after replacing ugly variable names
     */
    @NotNull
    public String pprint() {
        return pprint(false);
    }

    /**
     * Pretty prints a clause after replacing ugly variable names
     */
    @NotNull
    public String pprint(boolean replaceAnonymous) {
        String s = Clause2String(this.cnumbervars(replaceAnonymous));
        // if(fname!=null) s="%% "+fname+":"+begins_at+"-"+ends_at+"\n"+s;
        return s;
    }

    /**
     * Clause to Term converter: the joy of strong typing:-)
     */
    @NotNull
    public Clause toClause() { // overrides toClause in Term
        return this;
    }

    /**
     * Replaces varibles with nice looking upper case constants for printing
     * purposes
     */
    // synchronized
    @Nullable
    public Clause cnumbervars(boolean replaceAnonymous) {
        Map dd = this.dict;
        if (dd == null)
            return (Clause) numbervars();
        if (provenGround())
            return this;
        Trail trail = new Trail();

        dd.forEach( (X, V) -> {
            if (X instanceof String) {

                long occNb = ((Int) dd.get(V)).longValue();
                String s = (occNb < 2 && replaceAnonymous) ? "_" : (String) X;
                // bug: occNb not accurate when adding artif. '[]' head

                //Var V = (Var) dd.get(X);
                ((Var)V).unify(new PseudoVar(s), trail);
            }
        });
        Clause NewC = (Clause) numbervars();
        trail.unwind(0);
        return NewC;
    }

    /**
     * Converts a clause to a term. Note that Head:-true will convert to the
     * term Head.
     */
    @NotNull
    public final PTerm toTerm() {
        return (body() instanceof true_) ?
                head() :
                this;
    }

    /**
     * Creates a copy of the clause with variables standardized apart, i.e.
     * something like f(s(X),Y,X) becomes f(s(X1),Y1,X1)) with X1,Y1 variables
     * garantted not to occurring in the the current resolvant.
     */
    @Nullable
    final Clause ccopy() {
        if (ground)
            return this;
        Clause C = (Clause) copy();
        C.dict = null;
        C.ground = ground;
        return C;
    }

    /**
     * Extracts the head of a clause (a Term).
     */
    @NotNull
    public final PTerm head() {
        return args[0].ref();
    }

    /**
     * Extracts the body of a clause
     */
    @NotNull
    public final PTerm body() {
        return args[1].ref();
    }

    /**
     * Gets the leftmost (first) goal in the body of a clause, i.e. from
     * H:-B1,B2,...,Bn it will extract B1.
     */
    final PTerm getFirst() {
        PTerm body = body();
        if (body instanceof Conj)
            return ((Conj) body).args[0].ref();
        else if (body instanceof true_)
            return null;
        else
            return body;
    }

    /**
     * Gets all but the leftmost goal in the body of a clause, i.e. from
     * H:-B1,B2,...,Bn it will extract B2,...,Bn. Note that the returned Term is
     * either Conj or True, the last one meaning an empty body.
     *
     * @see True
     * @see Conj
     */
    @NotNull
    final PTerm getRest() {
        PTerm body = body();
        return body instanceof Conj ?
                ((Conj) body).args[1].ref() :
                PTerm.TRUE;
    }

    /**
     * Concatenates 2 Conjunctions
     *
     * @see Clause#unfold
     */
    static final PTerm appendConj(PTerm x, PTerm y) {
        y = y.ref();
        if (x instanceof true_)
            return y;
        if (y instanceof true_)
            return x; // comment out if using getState
        if (x instanceof Conj) {
            PTerm[] ca = ((Conj) x).args;
            PTerm curr = ca[0].ref();
            PTerm cont = appendConj(ca[1], y);
            // curr.getState(this,cont);
            return new Conj(curr, cont);
        } else
            return new Conj(x, y);
    }

    /**
     * Algebraic composition operation of 2 Clauses, doing the basic resolution
     * step Prolog is based on. From A0:-A1,A2...An and B0:-B1...Bm it builds
     * (A0:-B1,..Bm,A2,...An) mgu(A1,B0). Note that it returns null if A1 and B0
     * do not unify.
     *
     * @see PTerm#unify()
     */
    @Nullable
    private final Clause unfold(@NotNull Clause that, @NotNull Trail trail) {
        Clause result = null;
        PTerm first = getFirst();

        if (first != null && that.head().matches(first, trail)) {

            that = that.ccopy();

            that.head().unify(first, trail);

            PTerm cont = appendConj(that.body(), getRest());
            result = new Clause(head(), cont);
        }
        return result;
    }

    // synchronized
    @Nullable
    final Clause unfold_with_goal(@NotNull Clause goal, @NotNull Trail trail) {
        return goal.unfold(this, trail);
    }

	/*
     * // synchronized final Clause unfoldedCopy(Clause that,Trail trail) { int
	 * oldtop=trail.size(); Clause result=unfold(that,trail); if(result==null)
	 * return null; result=result.ccopy(); trail.unwind(oldtop); return result;
	 * }
	 */

    /**
     * Returns a key based on the principal functor of the head of the clause
     * and its arity.
     */
    @Nullable
    final public String getKey() {
        return head().getKey();
    }

    final boolean isClause() {
        return true;
    }
}
