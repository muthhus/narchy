package nars.term.compound;

import com.gs.collections.api.block.predicate.primitive.IntObjectPredicate;
import nars.Op;
import nars.nal.Tense;
import nars.term.*;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.subst.FindSubst;
import nars.util.data.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

import static nars.nal.Tense.DTERNAL;


public class GenericCompound<T extends Term> implements Compound<T> {


    /**
     * subterm vector
     */
    @NotNull
    public final TermVector<T> subterms;

    /**
     * subterm relation, resolves to unique concept
     */
    public final int relation;

    /**
     * temporal relation (dt), resolves to same concept
     */
    public final int dt;

    /**
     * content hash
     */
    public final int hash;

    @NotNull
    public final Op op;


    public transient boolean normalized;


    public GenericCompound(@NotNull Op op, @NotNull TermContainer subterms) {
        this(op, -1, subterms);
    }

    public GenericCompound(@NotNull Op op, int relation, @NotNull TermContainer subterms) {
        this(op, relation, Tense.DTERNAL, (TermVector<T>) subterms);
    }

    public GenericCompound(@NotNull Op op, int relation, int dt, @NotNull TermVector subterms) {
        if (!op.temporal && dt != DTERNAL)
            throw new RuntimeException("invalid temporal relation for " + op);


        this.subterms = subterms;

        this.normalized = (subterms.vars == 0) && (subterms.varPatterns == 0) /* not included in the count */;
        this.op = op;

        this.relation = relation;

        if (dt!=DTERNAL && !Op.isTemporal(op, dt, subterms.size()))
            throw new InvalidTerm(this.op, relation, dt, terms());

        //t = op.isTemporal() ? t : ITERNAL;
        this.dt = dt;


        this.hash = Util.hashCombine(subterms.hash, opRel(), dt);

    }

    @Override
    public final int opRel() {
        return Terms.opRel(op, relation);
    }

    @Override
    public final boolean isTerm(int i, @NotNull Op o) {
        //return subterms.isTerm(i, o);
        return subterms.term[i].op() == o;
    }

//    protected GenericCompound(GenericCompound copy, int newT) {
//        this.terms = copy.terms;
//        this.normalized = copy.normalized;
//        this.op = copy.op;
//        this.relation = copy.relation;
//        this.hash = copy.
//    }

    @NotNull
    @Override
    public final Op op() {
        return op;
    }

    @Override
    public final boolean isCommutative() {
        return op.commutative && size() > 1;
    }


    //    @Override
//    public final boolean isCommutative() {
//        if (op.isCommutative() && size() > 1) {
//            int t = dt();
//            return (t == DTERNAL || ((t == 0 && op == Op.CONJUNCTION)));
//        }
//        return false;
//    }

//    /** true if this compound does not involve temporal relation between its terms */
//    public boolean atemporal() {
//        return t()==ITERNAL;
//    }

    @Override
    public final void append(@NotNull Appendable p) throws IOException {
        TermPrinter.append(this, p);
    }

    @Override
    public @NotNull Term term() {
        return this;
    }

    @NotNull
    @Override
    public String toString() {
        return TermPrinter.stringify(this).toString();
    }

    @Override
    public int compareTo(@NotNull Termlike o) {
        if (this == o) return 0;
        //if (o == null) return -1;

        Termed t = (Termed) o;
        //int diff = op().compareTo(t.op());

        //sort by op and relation first
        int diff = Integer.compare(opRel(), t.opRel()); //op.ordinal(), t.op().ordinal());
        if (diff != 0)
            return diff;


        Compound c = (Compound) (t.term());

//        int diff2 = Integer.compare(this.hash, c.hashCode());
//        if (diff2 != 0)
//            return diff2;


        int diff3 = Integer.compare(this.dt, c.dt());
        if (diff3 != 0)
            return diff3;

        return subterms.compareTo(c.subterms());

    }


    @Override
    public final void copyInto(@NotNull Collection<Term> set) {
        subterms.copyInto(set);
    }

    @NotNull
    @Override
    public final TermContainer<T> subterms() {
        return subterms;
    }

    @Override
    public final boolean equals(@Nullable Object that) {
        return this == that ||
                ( that instanceof Compound && hash == that.hashCode() && equalsFurther((Termed) that));
    }

    @Override
    public final boolean equalTerms(@NotNull TermContainer c) {
        return subterms.equalTerms(c);
    }

    private final boolean equalsFurther(@NotNull Termed thatTerm) {

        Term u = thatTerm.term();
        if (opRel() == u.opRel() /*&& (((t instanceof Compound))*/) {
            Compound c = (Compound) u;
            /*if (relation != c.relation())
                return false;*/
            if (/*op.isTemporal() &&*/ dt!=c.dt())
                return false;
            if (!subterms.equals(c.subterms()))
                return false;
        }
        return true;
    }


    @Override
    public int hashCode() {
        return hash;
    }


    @Override
    public final int varDep() {
        return subterms.varDeps;
    }

    @Override
    public final int varIndep() {
        return subterms.varIndeps;
    }

    @Override
    public final int varQuery() {
        return subterms.varQuerys;
    }

    @Override
    public final int varPattern() {
        return subterms.varPatterns;
    }


    @Override
    public final int vars() {
        return subterms.vars;
    }

//    public final Term[] cloneTermsReplacing(int index, Term replaced) {
//        return terms.cloneTermsReplacing(index, replaced);
//    }


    @Override
    public final boolean contains(Object o) {
        return subterms.contains(o);
    }


    @Override
    public final void forEach(@NotNull Consumer<? super T> action, int start, int stop) {
        subterms.forEach(action, start, stop);
    }

    @NotNull
    @Override
    public TermContainer replacing(int subterm, Term replacement) {
        throw new RuntimeException("n/a for compound");
    }

    @Override
    public final void forEach(@NotNull Consumer<? super T> c) {
        subterms.forEach(c);
    }

    @NotNull
    @Override
    @Deprecated
    public T[] terms() {
        return subterms.terms();
    }

    @Override
    public final Term[] terms(@NotNull IntObjectPredicate<T> filter) {
        return subterms.terms(filter);
    }

    @Override
    public final Iterator<T> iterator() {
        return subterms.iterator();
    }

    @Override
    public int structure() {
        return subterms.structure() | op.bit;
    }

    @Override
    public final T term(int i) {
        return subterms.term(i);
    }

    @Override
    public final boolean containsTerm(Termlike target) {
        return subterms.containsTerm(target);
    }

    @Override
    public final int size() {
        return subterms.size();
    }

    @Override
    public final int complexity() {
        return subterms.complexity();
    }

    @Override
    public final int volume() {
        return subterms.volume();
    }

    @Override
    public final boolean impossibleSubTermVolume(int otherTermVolume) {
        return subterms.impossibleSubTermVolume(otherTermVolume);
    }

    @Override
    public final boolean isNormalized() {
        return normalized;
    }

    @Override
    public boolean match(@NotNull Compound y, @NotNull FindSubst subst) {

        int ys = y.size();
        TermVector<T> xsubs = subterms;
        if (xsubs.size() == ys)  {
            @NotNull Op op = this.op;
            if (op.isImage() && (relation != y.relation()))
                return false;


            @NotNull TermContainer ysubs = y.subterms();
            return (isCommutative()) ?
            //return (ys > 1 && op.commutative) ?
                    subst.matchPermute(xsubs, ysubs) :
                    subst.matchLinear(xsubs, ysubs);
        }
        return false;

    }

    /**
     * WARNING: this does not perform commutive handling correctly. use the index newTerm method for now
     */
    @NotNull
    @Override
    public final Compound dt(int cycles) {

        if (cycles == dt) return this;

        GenericCompound g = new GenericCompound(this.op, relation, cycles, subterms);
        if (normalized) g.setNormalized();
        return g;
    }

    @Override
    public final int dt() {
        return dt;
    }


    @Override
    public final int relation() {
        return relation;
    }

    /**
     * do not call this manually, it will be set by VariableNormalization only
     */
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
