package nars.index.term;

import jcog.Util;
import jcog.list.FasterList;
import nars.Op;
import nars.term.Term;

/**
 * Created by me on 4/2/17.
 */
public class CompoundBuilder extends FasterList<Term> {

    public int hash;

    public final Op op;
    public final int dt;

    public CompoundBuilder(Op op, int dt, Term[] u) {
        super(u.length, u);

        this.op = op;
        this.dt = dt;

        int hash = Util.hashClojure(op.hashCode(), dt);

        //include the hash as if the values were added iteratively such as in add()
        for (Term x : u)
            hash = Util.hashClojure(hash, x.hashCode());

        this.hash = hash;
    }


    public CompoundBuilder(Op op, int dt, int len) {
        super(len);

        this.op = op;
        this.dt = dt;
        this.hash = Util.hashClojure(op.hashCode(), dt);

        //hash will be modified for each added subterm
    }


//    public boolean add(CompoundBuilder x, TermBuilder b) {
//        add(x.get)
//    }

    @Override public boolean add(Term x) {
        super.add(x);
        hash = Util.hashClojure(hash, x.hashCode());
        return true;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, Term element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        CompoundBuilder f = (CompoundBuilder) obj;
        return f.hash == hash && f.op == op && f.dt == dt && super.equals(obj);
    }

    @Override
    public int hashCode() {
        return hash;
    }


}
