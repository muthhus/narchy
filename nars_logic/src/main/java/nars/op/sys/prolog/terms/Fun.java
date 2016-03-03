package nars.op.sys.prolog.terms;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.transform.subst.FindSubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Implements compound terms
 *
 * @see PTerm
 */
public class Fun extends Const implements Compound<PTerm> {

    public PTerm args[]; //TODO make final

    @Override
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



    /** null will cause this function to take its class simplename as the identifier */
    public Fun(@Nullable String s, int arity) {
        super(s);
        args = new PTerm[arity];
    }

    @NotNull
    public static PTerm[] init(int arity) {
        PTerm[] args = new PTerm[arity];
        for (int i = 0; i < arity; i++) {
            args[i] = new Var();
        }
        return args;
    }

    public final PTerm arg(int i) {
        return args[i].ref();
    }

    public final int getIntArg(int i) {
        return (int) ((Int) arg(i)).getValue();
    }

/*    public final void setArg(int i, PTerm T) {
        args[i] = T;
    }*/

    public final int putArg(int i, @NotNull PTerm T, @NotNull Prog p) {
        // return getArg(i).unify(T,p.getTrail())?1:0;
        return args[i].unify(T, p.getTrail()) ? 1 : 0;
    }

    public Fun(String s, @NotNull PTerm... x0) {
        this(s, x0.length);
        this.args = x0;
    }

//    public Fun(String name, PTerm... args) {
//        super(name);
//        this.args = args;
//    }
//    public Fun(String s, PTerm x0, PTerm x1) {
//        this(s, 2);
//        args[0] = x0;
//        args[1] = x1;
//    }
//
//    public Fun(String s, PTerm x0, PTerm x1, PTerm x2) {
//        this(s, 3);
//        args[0] = x0;
//        args[1] = x1;
//        args[2] = x2;
//    }
//
//    public Fun(String s, PTerm x0, PTerm x1, PTerm x2, PTerm x3) {
//        this(s, 4);
//        args[0] = x0;
//        args[1] = x1;
//        args[2] = x2;
//        args[3] = x3;
//    }

    @NotNull
    protected final String funToString() {
        return qname(name) +
                ((args == null) ?
                "()" : (args.length <= 0 ? "" : '(' + show_args() + ')'));
    }

    @NotNull
    public String toString() {
        return funToString();
    }

    @NotNull
    protected static String watchNull(@Nullable PTerm x) {
        return ((null == x) ? "null" : x.toString());
    }

    private String show_args() {
        PTerm[] a = this.args;
        StringBuilder s = new StringBuilder(watchNull(a[0]));
        for (int i = 1; i < a.length; i++) {
            s.append(',').append(watchNull(a[i]));
        }
        return s.toString();
    }

    @Override
    boolean bind_to(@NotNull PTerm that, Trail trail) {
        return /*getClass() == that.getClass() &&*/ name.equals(that.name) && args.length == ((Fun) that).args.length;
    }

    @Override
    boolean unify_to(@NotNull PTerm that, Trail trail) {
        return bind_to(that, trail) ?
                unifyBind((Fun) that, args, trail) :
                that.bind_to(this, trail);
    }

    static boolean unifyBind(Fun that, @NotNull PTerm[] a, Trail trail) {
        Fun other = that;
        int len = a.length;
        PTerm[] oa = other.args;
        for (int i = 0; i < len; i++) {
            if (!a[i].unify(oa[i], trail))
                return false;
        }
        return true;
    }

    @Override
    public PTerm token() {
        return args[0];
    }

    // stuff allowing polymorphic cloning of Fun subclasses
    // without using reflection - should be probaly faster than
    // reflection classes - to check

    @Nullable
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

    @Nullable
    final public Fun funClone(PTerm[] newArgs) {
        Fun f = funClone();
        f.args = newArgs;
        return f;
    }

    @Nullable
    protected Fun unInitializedClone() {
        Fun f = funClone();
        f.args = new PTerm[args.length];
        return f;
    }

//    protected Fun initializedClone() {
//        Fun f = funClone();
//        f.init(args.length);
//        return f;
//    }

    @Override
    @Nullable
    final PTerm reaction(@NotNull PTerm that) {
        // IO.mes("TRACE>> "+name());

        PTerm[] args = this.args;
        int n = args.length;
        PTerm[] fargs = new PTerm[n];
        for (int i = 0; i < n; i++) {
            fargs[i] = args[i].reaction(that);
        }
        return funClone(fargs);
    }

    @NotNull
    public Cons listify() {
        Cons l = new Cons(new Const(name), PTerm.NIL);
        Cons curr = l;
        PTerm[] args = this.args;
        for (int i = 0; i < args.length; i++) {
            Cons tail = new Cons(args[i], PTerm.NIL);
            curr.args[1] = tail;
            curr = tail;
        }
        return l;
    }

    @Override
    boolean isClause() {
        return arity() == 2 && name.equals(":-");
    }


    @Override
    public @NotNull
    Op op() {
        return null;
    }

    @Override
    public int volume() {
        return 0;
    }

    @Override
    public int complexity() {
        return 0;
    }

    @Override
    public int structure() {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean containsTerm(Term t) {
        return false;
    }

    @Override
    public boolean isCommutative() {
        return false;
    }

    @Override
    public int varIndep() {
        return 0;
    }

    @Override
    public int varDep() {
        return 0;
    }

    @Override
    public int varQuery() {
        return 0;
    }

    @Override
    public int varPattern() {
        return 0;
    }

    @Override
    public int vars() {
        return 0;
    }

    @NotNull
    @Override
    public TermContainer<PTerm> subterms() {
        return null;
    }

    @Override
    public boolean match(Compound y, FindSubst subst) {
        return false;
    }

    @Override
    public boolean isNormalized() {
        return false;
    }

    @NotNull
    @Override
    public Compound dt(int cycles) {
        return null;
    }

    @Override
    public int dt() {
        return 0;
    }

    @Nullable
    @Override
    public Iterator<PTerm> iterator() {
        return null;
    }

    @Nullable
    @Override
    public PTerm term(int i) {
        return null;
    }

    @Override
    public boolean equalTerms(TermContainer c) {
        return false;
    }

    @Override
    public void addAllTo(Collection<Term> set) {

    }

    @NotNull
    @Override
    public PTerm[] terms() {
        return new PTerm[0];
    }

    @Override
    public void forEach(Consumer<? super PTerm> action, int start, int stop) {

    }

    @Nullable
    @Override
    public TermContainer replacing(int subterm, Term replacement) {
        return null;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }

    @Override
    public int relation() {
        return -1;
    }
}
