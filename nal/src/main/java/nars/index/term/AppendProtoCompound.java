package nars.index.term;

import jcog.Util;
import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * a lightweight, prototype compound, not fully constructed.
 *
 * it is constructed incrementally by appending additional subterms.
 *
 * it accumulates a rolling hash so it may be used as a key in a hash Map
 *
 * TODO:
 *      VolumeLimitedIncrementalProtoCompound extends AppendProtoCompound
 *      etc...
 */
public class AppendProtoCompound implements ProtoCompound {

    public final Op op;
    public final int dt;

    @NotNull private Term[] subs = Term.EmptyArray;
    int size;

    private int hash;



    public AppendProtoCompound(Op op, int dt, @NotNull Term[] u) {
        this.subs = u;
        this.size = u.length;
        this.op = op;
        this.dt = dt;
        if (u.length > 0) {
            int hash = this.hash;
            for (Term x : u)
                hash = Util.hashClojure(hash, x.hashCode());
            this.hash = hash;
        }
    }

    /** hash will be modified for each added subterm
     *  @param initial_capacity estimated size, but will grow if exceeded
     * */
    public AppendProtoCompound(Op op, int dt, int initial_capacity) {
        this.subs = new Term[initial_capacity];
        this.op = op;
        this.dt = dt;

        this.hash = Util.hashClojure(op.hashCode(), dt);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Op op() {
        return op;
    }

    @Override
    public int dt() {
        return dt;
    }


    /** safe for use during final build step */
    @Override public Term[] subterms() {
        if (subs.length == size) {
            return subs; //dont reallocate it's just fine to share
        } else {
            return subs = Arrays.copyOfRange(subs, 0, size);
        }
    }

    @Override public boolean AND(@NotNull Predicate<Term> t) {
        for (Term x : subs) {
            if (x == null) break;
            if (t.test(x)) return false;
        }
        return true;
    }

    @Override public boolean OR(@NotNull Predicate<Term> t) {
        for (Term x : subs) {
            if (x == null) break;
            if (t.test(x)) return true;
        }
        return false;
    }

    public boolean add(@NotNull Term x) {
        int c = subs.length;
        int len = this.size;
        if (c == len) {
            int newCapacity = len + Math.max(1,(len /2));
            Term[] newItems = new Term[newCapacity];
            System.arraycopy(this.subs, 0, newItems, 0, Math.min(len, newCapacity));
            subs = newItems;
        }

        subs[size++] = x;
        hash = Util.hashClojure(hash, x.hashCode());

        return true;
    }

    public void addAll(@NotNull Term[] u) {
        for (Term x : u) {
            add(x);
        }
    }

    @Override
    public boolean equals(Object obj) {
        AppendProtoCompound f = (AppendProtoCompound) obj;
        return f.hash == hash && f.op == op && f.dt == dt && Arrays.equals(subs, f.subs);
    }

    @Override
    public int hashCode() {
        return hash;
    }


    @Override public Term sub(int i) {
        return subs[i];
    }

    @Override
    public void forEach(Consumer<? super Term> action, int start, int stop) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public String toString() {
        return "AppendProtoCompound{" +
                "op=" + op +
                ", dt=" + dt +
                ", subs=" + Arrays.toString(Arrays.copyOfRange(subs, 0, size)) + //HACK use more efficient string method
                '}';
    }

    @NotNull
    @Override
    public Iterator<Term> iterator() {
        throw new UnsupportedOperationException("TODO");
    }
}
