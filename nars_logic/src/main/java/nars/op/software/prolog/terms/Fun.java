package nars.op.software.prolog.terms;

import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

import static nars.op.software.prolog.terms.Const.qname;

/**
 * Implements compound terms
 *
 * @see nars.op.software.prolog.terms.PTerm
 */
public class Fun extends Nonvar implements Compound<PTerm> {

    public PTerm args[]; //TODO make final

    public final int arity() {
        return args.length;
    }

    public Fun(String s) {
        super(s);
        args = null;
    }
  
  /*
  public Fun(int arity) {
     //setDefaultName();
     args=new Term[arity];
  }
  */

    public Fun(String name, PTerm... args) {
        super(name);
        this.args = args;
    }

    /** null will cause this function to take its class simplename as the identifier */
    public Fun(@Nullable String s, int arity) {
        super(s);
        args = new PTerm[arity];
    }

    public void init(int arity) {
        PTerm[] args = this.args = new PTerm[arity];
        for (int i = 0; i < arity; i++) {
            args[i] = new Var();
        }
    }

    public final nars.op.software.prolog.terms.PTerm arg(int i) {
        return args[i].ref();
    }

    public final int getIntArg(int i) {
        return (int) ((Int) arg(i)).getValue();
    }

    public final void setArg(int i, PTerm T) {
        args[i] = T;
    }

    public final int putArg(int i, PTerm T, Prog p) {
        // return getArg(i).unify(T,p.getTrail())?1:0;
        return args[i].unify(T, p.getTrail()) ? 1 : 0;
    }

    public Fun(String s, PTerm x0) {
        this(s, 1);
        args[0] = x0;
    }

    public Fun(String s, PTerm x0, PTerm x1) {
        this(s, 2);
        args[0] = x0;
        args[1] = x1;
    }

    public Fun(String s, PTerm x0, PTerm x1, PTerm x2) {
        this(s, 3);
        args[0] = x0;
        args[1] = x1;
        args[2] = x2;
    }

    public Fun(String s, PTerm x0, PTerm x1, PTerm x2, PTerm x3) {
        this(s, 4);
        args[0] = x0;
        args[1] = x1;
        args[2] = x2;
        args[3] = x3;
    }

    protected final String funToString() {
        return qname(name) +
                ((args == null) ?
                "()" : (args.length <= 0 ? "" : '(' + show_args() + ')'));
    }

    public String toString() {
        return funToString();
    }

    protected static String watchNull(nars.op.software.prolog.terms.PTerm x) {
        return ((null == x) ? "null" : x.toString());
    }

    private String show_args() {
        nars.op.software.prolog.terms.PTerm[] a = this.args;
        StringBuilder s = new StringBuilder(watchNull(a[0]));
        for (int i = 1; i < a.length; i++) {
            s.append(',').append(watchNull(a[i]));
        }
        return s.toString();
    }

    boolean bind_to(nars.op.software.prolog.terms.PTerm that, Trail trail) {
        return getClass() == that.getClass() && name.equals(that.name) && args.length == ((Fun) that).args.length;
    }

    boolean unify_to(nars.op.software.prolog.terms.PTerm that, Trail trail) {
        return bind_to(that, trail) ?
                unifyBind((Fun) that, args, trail) :
                that.bind_to(this, trail);
    }

    static boolean unifyBind(Fun that, nars.op.software.prolog.terms.PTerm[] a, Trail trail) {
        Fun other = that;
        int len = a.length;
        nars.op.software.prolog.terms.PTerm[] oa = other.args;
        for (int i = 0; i < len; i++) {
            if (!a[i].unify(oa[i], trail))
                return false;
        }
        return true;
    }

    public nars.op.software.prolog.terms.PTerm token() {
        return args[0];
    }

    // stuff allowing polymorphic cloning of Fun subclasses
    // without using reflection - should be probaly faster than
    // reflection classes - to check

    final public Fun funClone() {
        Fun f = null;

        try {
            // use of clone is needed for "polymorphic" copy
            f = (Fun) clone();
        } catch (CloneNotSupportedException e) {
            // IO.errmes("clone: "+e);
        }

        return f;
    }

    final public Fun funClone(nars.op.software.prolog.terms.PTerm[] newArgs) {
        Fun f = funClone();
        f.args = newArgs;
        return f;
    }

    protected Fun unInitializedClone() {
        Fun f = funClone();
        f.args = new nars.op.software.prolog.terms.PTerm[args.length];
        return f;
    }

    protected Fun initializedClone() {
        Fun f = funClone();
        f.init(args.length);
        return f;
    }

    final nars.op.software.prolog.terms.PTerm reaction(nars.op.software.prolog.terms.PTerm that) {
        // IO.mes("TRACE>> "+name());

        nars.op.software.prolog.terms.PTerm[] args = this.args;
        int n = args.length;
        nars.op.software.prolog.terms.PTerm[] fargs = new nars.op.software.prolog.terms.PTerm[n];
        for (int i = 0; i < n; i++) {
            fargs[i] = args[i].reaction(that);
        }
        return funClone(fargs);
    }

    public Cons listify() {
        Cons l = new Cons(new Const(name), Const.NIL);
        Cons curr = l;
        for (int i = 0; i < args.length; i++) {
            Cons tail = new Cons(args[i], Const.NIL);
            curr.args[1] = tail;
            curr = tail;
        }
        return l;
    }

    boolean isClause() {
        return arity() == 2 && name.equals(":-");
    }

    @NotNull
    @Override
    public TermContainer<PTerm> subterms() {
        return null;
    }

    @Override
    public boolean isNormalized() {
        return false;
    }

    @NotNull
    @Override
    public Compound t(int cycles) {
        return null;
    }

    @Override
    public int t() {
        return 0;
    }

    @Override
    public Iterator<PTerm> iterator() {
        return null;
    }

    @Override
    public PTerm term(int i) {
        return null;
    }

    @Override
    public void addAllTo(Collection<Term> set) {

    }

    @Override
    public PTerm[] terms() {
        return new PTerm[0];
    }

    @Override
    public void forEach(Consumer<? super PTerm> action, int start, int stop) {

    }

    @Override
    public TermContainer replacing(int subterm, Term replacement) {
        return null;
    }
}
