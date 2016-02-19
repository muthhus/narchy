package nars.op.software.prolog.fluents;

import nars.op.software.prolog.Prolog;
import nars.op.software.prolog.io.IO;
import nars.op.software.prolog.io.Parser;
import nars.op.software.prolog.terms.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Implements a Term and Clause objects based blackboard (database).
 */
public class DataBase extends BlackBoard {

    public DataBase() {
        super();
    }

    private static final Const yes = PTerm.YES;

    private static final Const no = PTerm.NO;

    /**
     * Removes a matching Term from the blackboards and signals failure if no
     * such term is found.
     */
    @Nullable
    public PTerm cin(String k, @NotNull PTerm pattern) {
        PTerm found = take(k, pattern);
        // if(found!=null) {
        // found=found.matching_copy(pattern);
        // }
        if (found == null)
            found = no;
        else
            found = new Fun("the", found.copy());
        return found;
    }

    /**
     * Adds a Term to the blackboard
     */
    @NotNull
    public PTerm out(String k, @NotNull PTerm pattern, boolean copying) {
        add(k, copying ? pattern.copy() : pattern);
        return yes;
    }

    /**
     * Adds a copy of a Term to the blackboard
     */

    // synchronized
    @NotNull
    public PTerm out(String key, @NotNull PTerm pattern) {
        return out(key, pattern, true); // copies pattern
    }

    private void all0(int max, @NotNull ArrayList To, String k, @NotNull PTerm FXs) {
        if (0 == max)
            max = -1;
        Queue Q = (Queue) get(k);
        if (Q == null)
            return;
        // todo: use always the same "server's" trail
        for (Iterator e = Q.toEnumeration(); e.hasNext(); ) {
            PTerm t = (PTerm) e.next();
            if (null == t)
                break;
            t = t.matching_copy(FXs);
            if (t != null && 0 != max--)
                To.add(t);
        }
    }

    private PTerm all1(int max, @NotNull PTerm FXs) {
        ArrayList<PTerm> To = new ArrayList();
        for (Object o : keySet())
            all0(max, To, (String) o, FXs);
        Fun R = new Fun("$", To.toArray(new PTerm[To.size()]));
        // IO.mes("RR"+R);
        // To.copyInto(R.args);
        return ((Cons) R.listify()).args[1];
    }

    private PTerm all2(int max, @Nullable String k, @NotNull PTerm FXs) {
        if (k == null) {
            // IO.mes("expensive operation: all/2 with unknown key");
            return all1(max, FXs);
        }
        ArrayList To = new ArrayList();
        all0(max, To, k, FXs);
        if (To.size() == 0)
            return PTerm.NIL;
        Fun R = new Fun("$", (PTerm[]) To.toArray());
        // To.copyInto(R.args);
        PTerm T = ((Cons) R.listify()).args[1];
        return T;
    }

    /**
     * Returns a (possibly empty) list of matching Term objects
     */
    public PTerm all(String k, PTerm FX) {
        FX = all2(0, k, FX);
        return FX;
    }

    /**
     * Gives an Iterator view to the Queue of Term or Clause objects stored at
     * key k
     *
     * @see Queue
     * @see PTerm
     * @see Clause
     */
    public Iterator toEnumerationFor(String k) {
        Iterator E = super.toEnumerationFor(k);
        return E;
    }

    /**
     * Returns a formatted String representation of this PrologBlackboard object
     */
    @NotNull
    public String pprint() {
        StringBuilder s = new StringBuilder(name());
        for (Object o : keySet()) {
            s.append(pred_to_string((String) o));
            s.append('\n');
        }
        return s.toString();
    }

    @Nullable
    public String pred_to_string(String key) {
        Queue Q = (Queue) get(key);
        if (null == Q)
            return null;
        Iterator e = Q.toEnumeration();
        StringBuilder s = new StringBuilder("% " + key + "\n\n");
        while (e.hasNext()) {
            s.append(((PTerm) e.next()).pprint());
            s.append(".\n");
        }
        s.append('\n');
        return s.toString();
    }

    /**
     * consults or reconsults a Prolog file by adding or overriding existing
     * predicates to be extended to load from URLs transparently
     */
    static public boolean fromFile(@NotNull Prolog p, @NotNull String f, boolean overwrite) {
        if (IO.trace())
            IO.trace("last consulted file was: " + lastFile);
        boolean ok = fileToProg(p, f, overwrite);
        if (ok) {
            if (IO.trace())
                IO.trace("last consulted file set to: " + f);
            lastFile = f;
        } else {
            if (IO.trace())
                IO.error("error in consulting file: " + f);
        }
        return ok;
    }

    /**
     * reconsults a file by overwritting similar predicates in memory
     */
    static public boolean fromFile(@NotNull Prolog p, @NotNull String f) {
        return fromFile(p, f, true);
    }

    private static String lastFile = Prolog.default_lib;

    /**
     * reconsults the last reconsulted file
     */
    static public boolean fromFile(@NotNull Prolog p) {
        IO.println("begin('" + lastFile + "')");
        boolean ok = fromFile(p, lastFile);
        if (ok)
            IO.println("end('" + lastFile + "')");
        return ok;
    }

    static private boolean fileToProg(@NotNull Prolog prolog, @NotNull String fname,
                                      boolean overwrite) {
        Reader sname = IO.toFileReader(fname);
        if (null == sname)
            return false;
        return streamToProg(prolog, fname, sname, overwrite);
    }

    /**
     * Reads a set of clauses from a stream and adds them to the blackboard.
     * Overwrites old predicates if asked to. Returns true if all went well.
     */
    static public boolean streamToProg(@NotNull Prolog prolog, @NotNull Reader sname,
                                       boolean overwrite) {
        return streamToProg(prolog, sname.toString(), sname, overwrite);
    }

    static private boolean streamToProg(@NotNull Prolog prolog, @NotNull String fname,
                                        @NotNull Reader sname, boolean overwrite) {
        BlackBoard ktable = overwrite ? (BlackBoard) prolog.db.clone() : null;
        // Clause Err=new Clause(new Const("error"),new Var());
        try {
            Parser p = new Parser(prolog, sname);
            apply_parser(p, fname, ktable);
        } catch (Exception e) { // already catched by readClause
            IO.error("unexpected error in streamToProg", e);
            return false;
        }
        return true;
    }

    static private void apply_parser(@NotNull Parser p, @NotNull String fname, BlackBoard ktable) {
        for (; ; ) {
            if (p.atEOF())
                return;
            int begins_at = p.lineno();
            Clause C = p.readClause();
            if (null == C)
                return;
            if (Parser.isError(C))
                Parser.showError(C);
            else {
                // IO.mes("ADDING= "+C.pprint());
                processClause(p.prolog, C, ktable);
                C.setFile(fname, begins_at, p.lineno());
            }
        }
    }

    /**
     * adds a Clause to the joint Linda and Predicate table
     */
    public static void addClause(@NotNull Prolog p, @NotNull Clause C, @Nullable HashDict ktable) {
        String k = C.getKey();
        // overwrites previous definitions
        if (null != ktable && null != ktable.get(k)) {
            ktable.remove(k);
            p.db.remove(k);
        }
        p.db.out(k, C, false);
    }

    /**
     * adds a Clause to the joint Linda and Predicate table
     *
     * @see Clause
     */
    public static void processClause(@NotNull Prolog prolog, @NotNull Clause C, HashDict ktable) {
        if (C.head().matches(new Const("init"))) {
            // IO.mes("init: "+C.getBody());
            Prog.firstSolution(prolog, C.head(), C.body());
        } else {
            // IO.mes("ADDING= "+C.pprint());
            addClause(prolog, C, ktable);
        }
    }

    @NotNull
    public PTerm add(@NotNull PTerm X) {
        return out(X.getKey(), X);
    }
}
