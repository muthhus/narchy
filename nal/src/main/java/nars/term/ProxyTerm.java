package nars.term;

import nars.Op;
import nars.index.term.TermIndex;
import nars.term.subst.Unify;
import nars.term.visit.SubtermVisitor;
import nars.term.visit.SubtermVisitorX;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
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
    public boolean containsTerm(Termlike t) {
        return ref.containsTerm(t);
    }

    @Override
    public boolean hasTemporal() {
        return ref.hasTemporal();
    }

    @Override
    public @Nullable Term termOr(int i, @Nullable Term ifOutOfBounds) {
        return ref.termOr(i, ifOutOfBounds);
    }

    @Override
    public boolean equals(Object obj) {
        return ref.equals(obj);
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
        return ref.compareTo(y);
    }

    @Override
    public boolean AND(Predicate<Term> v) {
        return ref.AND(v);
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
    public void recurseTerms(@NotNull SubtermVisitor v) {
        ref.recurseTerms(v);
    }

    @Override
    public void recurseTerms(@NotNull SubtermVisitorX v, @Nullable Compound parent) {
        ref.recurseTerms(v);
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
    public Term eval(TermIndex index) {
        return ref.eval(index);
    }
}
