package nars.index.term;

import jcog.byt.DynByteSeq;
import nars.IO;
import nars.Op;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.time.Tense.DTERNAL;

/**
 * a lightweight, prototype compound, not fully constructed.
 * <p>
 * it is constructed incrementally by appending additional subterms.
 * <p>
 * it accumulates a rolling hash so it may be used as a key in a hash Map
 * <p>
 * TODO:
 * VolumeLimitedIncrementalProtoCompound extends AppendProtoCompound
 * etc...
 */
public class AppendProtoCompound extends /*HashCached*/DynByteSeq implements ProtoCompound {

    public final Op op;
    public final int dt;

    @NotNull
    private Term[] subs = Term.EmptyArray;

    int size;

    int hash;

    public AppendProtoCompound(Op op, int dt, @NotNull Term[] u) {
        this(op, dt, 0);

        this.subs = u; //zero-copy direct usage
        size = u.length;
        for (Term x : u)
            appendKey(x);
    }

    /**
     * hash will be modified for each added subterm
     *
     * @param initial_capacity estimated size, but will grow if exceeded
     */
    public AppendProtoCompound(Op op, int dt, int initial_capacity) {
        super(initial_capacity * 8);
        if (initial_capacity > 0)
            this.subs = new Term[initial_capacity];
        this.op = op;
        this.dt = dt;

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


    /**
     * safe for use during final build step
     */
    @Override
    public Term[] subterms() {
        Term[] tt;
        int s = this.size;
        @NotNull Term[] ss = this.subs;
        if (ss.length == s) {
            tt = ss;
            //return ss; //dont reallocate it's just fine to share
        } else {
            tt = Arrays.copyOfRange(ss, 0, s); //trim
        }

        this.subs = null; //clear refernce to the array from this point
        return tt;
    }

    /**
     * hashes and prepares for use in hashmap
     */
    public AppendProtoCompound commit() {
        writeByte(op.ordinal());
        for (Term x : subs)
            appendKey(x);
        if (dt!=DTERNAL)
            writeInt(dt);

        compact();
        this.hash = hash(0, len);
        return this;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean AND(@NotNull Predicate<Term> t) {
        for (Term x : subs) {
            //if (x == null)
            //  break;
            if (t.test(x)) return false;
        }
        return true;
    }

    @Override
    public boolean OR(@NotNull Predicate<Term> t) {
        for (Term x : subs) {
            //if (x == null)
            //  break;
            if (t.test(x)) return true;
        }
        return false;
    }

    public boolean add(@NotNull Term x) {
        int c = subs.length;
        int len = this.size;
        if (c == len) {
            ensureCapacity(len, len + Math.max(1, (len / 2)));
        }

        _add(x);

        return true;
    }

    protected void _add(@NotNull Term x) {
        subs[size++] = x;
    }

    private void appendKey(@NotNull Term x) {
        try {
            IO.writeTerm(this, x);
            writeByte(0); //separator
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void ensureCapacity(int newCapacity) {
        int s = this.size;
        if (s < newCapacity)
            ensureCapacity(s, newCapacity);
    }

    private void ensureCapacity(int curCap, int newCapacity) {
        Term[] newItems = new Term[newCapacity];
        System.arraycopy(this.subs, 0, newItems, 0, Math.min(curCap, newCapacity));
        subs = newItems;
    }

    public void addAll(@NotNull Term[] u) {
        int ul = u.length;
        if (ul > 0) {
            ensureCapacity(size + ul);
            for (Term x : u)
                _add(x);
        }
    }

//    @Override
//    public boolean equals(Object obj) {
//        AppendProtoCompound x = (AppendProtoCompound) obj;
//        return x.hash == hash && x.op == op && x.dt == dt && Arrays.equals(bytes, x.bytes);
//                //Util.equalArraysDirect(subs, x.subs);
//    }

    @Override
    public Term sub(int i) {
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
