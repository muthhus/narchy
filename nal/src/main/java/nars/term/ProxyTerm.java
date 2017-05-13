package nars.term;

import nars.Op;
import nars.index.term.TermContext;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by me on 3/26/17.
 */
public class ProxyTerm<T extends Term> implements Term {

    public final T ref;

    public ProxyTerm(T t) {
        this.ref = t;
    }

    @Override
    public int size() {
        return ref.size();
    }

    @Override
    public boolean contains(Termlike t) {
        return ref.contains(t);
    }

    @Override
    public boolean isTemporal() {
        return ref.isTemporal();
    }

    @Override
    public @Nullable Term sub(int i, @Nullable Term ifOutOfBounds) {
        return ref.sub(i, ifOutOfBounds);
    }

    @Override
    public boolean equals(Object obj) {
        return ref == obj || ref.equals(obj);
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }

    @Override
    public String toString() {
        return ref.toString();
    }

    @Override
    public int compareTo(@NotNull Termlike y) {
        return (ref == y) ? 0 : ref.compareTo(y);
    }

    @Override
    public boolean AND(Predicate<Term> v) {
        return ref.AND(v);
    }

    @Override
    public boolean ANDrecurse(@NotNull Predicate<Term> v) {
        return ref.ANDrecurse(v);
    }

    @Override
    public boolean OR(Predicate<Term> v) {
        return ref.OR(v);
    }

    @Override
    public int varIndep() {
        return ref.varIndep();
    }

    @Override
    public int varDep() {
        return ref.varDep();
    }

    @Override
    public int varQuery() {
        return ref.varQuery();
    }

    @Override
    public int varPattern() {
        return ref.varPattern();
    }

    @Override
    public @NotNull Op op() {
        return ref.op();
    }

    @Override
    public int volume() {
        return ref.volume();
    }

    @Override
    public int complexity() {
        return ref.complexity();
    }

    @Override
    public int structure() {
        return ref.structure();
    }

    @Override
    public void recurseTerms(@NotNull Consumer<Term> v) {
        ref.recurseTerms(v);
    }

    @Override
    public boolean recurseTerms(BiPredicate<Term, Compound> whileTrue, @Nullable Compound parent) {
        return ref.recurseTerms(whileTrue);
    }

    @Override
    public boolean isCommutative() {
        return ref.isCommutative();
    }

    @Override
    public boolean unify(@NotNull Term y, @NotNull Unify subst) {
        return ref.unify(y, subst);
    }

    @Override
    public void append(@NotNull Appendable w) throws IOException {
        ref.append(w);
    }

    @Override
    public Term eval(TermContext index) {
        return ref.eval(index);
    }

    @Override
    public boolean isDynamic() {
        return ref.isDynamic();
    }

}
