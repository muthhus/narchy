package nars.term.compound;

import com.gs.collections.api.block.predicate.primitive.IntObjectPredicate;
import nars.Op;
import nars.nal.nal7.Tense;
import nars.term.Term;
import nars.term.TermVector;
import nars.term.Termed;
import nars.term.compile.TermPrinter;
import nars.util.data.Util;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

import static nars.nal.nal7.Tense.ITERNAL;


public class GenericCompound<T extends Term> implements Compound<T> {

    public final Op op;

    /** subterm vector */
    public final TermVector<T> terms;

    /** subterm relation, resolves to unique concept */
    public final int relation;

    /** temporal relation (dt), resolves to same concept */
    public final int t;


    private final transient int hash;
    private transient boolean normalized = false;


    public GenericCompound(Op op, TermVector subterms) {
        this(op, -1, subterms);
    }

    public GenericCompound(Op op, int relation, TermVector subterms) {
        this(op, relation, Tense.ITERNAL, subterms);
    }

    public GenericCompound(Op op, int relation, int t, TermVector subterms) {
        this.terms = subterms;
        this.normalized = (subterms.vars() == 0);
        this.op = op;

        t = op().isTemporal() ? t : ITERNAL;
        this.t = t;

        this.relation = relation;

        this.hash = Util.hashCombine(terms.hashCode(), opRel(), t);


    }

//    protected GenericCompound(GenericCompound copy, int newT) {
//        this.terms = copy.terms;
//        this.normalized = copy.normalized;
//        this.op = copy.op;
//        this.relation = copy.relation;
//        this.hash = copy.
//    }

    @Override
    public final Op op() {
        return op;
    }


    @Override
    public final boolean isCommutative() {
        return op.isCommutative();
    }

    @Override
    public final void append(Appendable p, boolean pretty) throws IOException {
        TermPrinter.append(this, p, pretty);
    }

    @Override public Compound anonymous() {
        return this.t(ITERNAL);
    }

    @Override
    public int compareTo(Object o) {
        int r=0;
        if (this != o) {
            Termed t = (Termed) o;
            //int diff = op().compareTo(t.op());
            int diff = Integer.compare(op().ordinal(), t.op().ordinal());
            if (diff != 0) r = diff;
            else {

                Compound c = (Compound) (t.term());
                int diff2 = Integer.compare(relation(), c.relation());
                if (diff2 != 0) return diff2;

                int diff3 = Integer.compare(this.t, c.t());
                if (diff3 != 0) return diff3;

                r=subterms().compareTo(c.subterms());
            }
        }

        return r;
    }


    @Override
    public final void addAllTo(Collection<Term> set) {
        terms.addAllTo(set);
    }

    @Override
    public final TermVector<T> subterms() {
        return terms;
    }

    @Override
    public boolean equals(Object that) {
        return this == that || hashCode() == that.hashCode() && equalsFurther((Termed) that);
    }

    private boolean equalsFurther(Termed thatTerm) {

        boolean r=false;
        Term u = thatTerm.term();
        if ((op == u.op()) /*&& (((t instanceof Compound))*/) {
            Compound c = (Compound) u;
            r=terms.equals(c.subterms())
                    && (relation == c.relation())
                    && (t == c.t());
        }
        return r;
    }




    @Override
    public int hashCode() {
        return hash;
    }


    @Override
    public final int varDep() {
        return terms.varDep();
    }

    @Override
    public final int varIndep() {
        return terms.varIndep();
    }

    @Override
    public final int varQuery() {
        return terms.varQuery();
    }

    @Override
    public final int vars() {
        return terms.vars();
    }

//    public final Term[] cloneTermsReplacing(int index, Term replaced) {
//        return terms.cloneTermsReplacing(index, replaced);
//    }

    public final boolean isEmpty() {
        return terms.isEmpty();
    }

    public final boolean contains(Object o) {
        return terms.contains(o);
    }


    @Override
    public final void forEach(Consumer<? super T> action, int start, int stop) {
        terms.forEach(action, start, stop);
    }

    @Override
    public final void forEach(Consumer<? super T> c) {
        terms.forEach(c);
    }

    @Override public T[] terms() {
        return terms.term;
    }

    @Override
    public final Term[] terms(IntObjectPredicate<T> filter) {
        return terms.terms(filter);
    }

    @Override
    public final Iterator<T> iterator() {
        return terms.iterator();
    }

    @Override
    public int structure() {
        return terms.structure() | op.bit();
    }

    @Override
    public final T term(int i) {
        return terms.term(i);
    }

    @Override
    public final boolean containsTerm(Term target) {
        return terms.containsTerm(target);
    }

    @Override
    public final int size() {
        return terms.size();
    }

    @Override
    public final int complexity() {
        return terms.complexity();
    }

    @Override
    public final int volume() {
        return terms.volume();
    }

    @Override
    public final boolean impossibleSubTermVolume(int otherTermVolume) {
        return terms.impossibleSubTermVolume(otherTermVolume);
    }

    @Override
    public final boolean isNormalized() {
        return normalized;
    }

    public Compound t(int cycles) {
        if (cycles == t) return this;
        GenericCompound g = new GenericCompound(op(), relation, cycles, subterms());
        if (isNormalized()) g.setNormalized();
        return g;
    }

    @Override
    public int t() {
        return t;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    @Override
    public int relation() {
        return relation;
    }

    /** do not call this manually, it will be set by VariableNormalization only */
    public final void setNormalized() {
        this.normalized = true;
    }


//    @Override
//    public int bytesLength() {
//        int len = /* opener byte */1 + (op.isImage() ? 1 : 0);
//
//        int n = size();
//        for (int i = 0; i < n; i++) {
//            len += term(i).bytesLength() + 1 /* separator or closer if end*/;
//        }
//
//        return len;
//    }

//    @Override
//    public final byte[] bytes() {
//
//        ByteBuf b = ByteBuf.create(bytesLength());
//
//        b.add((byte) op().ordinal()); //header
//
//        if (op().isImage()) {
//            b.add((byte) relation); //header
//        }
//
//        appendSubtermBytes(b);
//
//        if (op().maxSize != 1) {
//            b.add(COMPOUND_TERM_CLOSERbyte); //closer
//        }
//
//        return b.toBytes();
//    }
//
//
//    @Override
//    public void appendSubtermBytes(ByteBuf b) {
//        terms.appendSubtermBytes(b);
//    }
//
//    @Override
//    public final boolean and(Predicate<? super Term> v) {
//        return v.test(this) && terms.and(v);
//    }
//    @Override
//    public final boolean or(Predicate<? super Term> v) {
//        return v.test(this) || terms.or(v);
//    }




}
