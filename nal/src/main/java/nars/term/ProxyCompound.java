package nars.term;

import nars.IO;
import nars.Op;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ProxyCompound implements Compound/*, NonInternable*/ {

    /** only modify this if you are sure it wont change hash/equality */
    protected Compound ref;

    public ProxyCompound(Compound ref) {
        this.ref = ref;
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return Compound.equals(this, obj);
    }

    @Override
    public final String toString() {
        return IO.Printer.stringify(this).toString();
    }

    @Override
    public void append(@NotNull Appendable p) throws IOException {
        IO.Printer.append(this, p);
    }

    @Override
    public boolean isNormalized() {
        return ref.isNormalized();
    }

    @Override
    public void setNormalized() {
        ref.setNormalized();
    }

    @Override
    public @NotNull Op op() {
        return ref.op();
    }

    @Override
    public int dt() {
        return ref.dt();
    }

    @Override
    public @NotNull TermContainer subterms() {
        return ref.subterms();
    }

    @Override
    public boolean impossibleSubTermOrEquality(@NotNull Term target) {
        return ref.impossibleSubTermOrEquality(target);
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
    public boolean unifyPossible(@Nullable Op t) {
        return ref.unifyPossible(t);
    }

    @Override
    public boolean isDynamic() {
        return ref.isDynamic();
    }

    @Override
    public boolean subIs(int i, Op o) {
        return ref.subIs(i, o);
    }

    @Override
    public boolean subIs(int i, Term maybeEquals) {
        return ref.subIs(i, maybeEquals);
    }

    @Override
    public boolean subIs(Op thisOp, int i, Term sub) {
        return ref.subIs(thisOp, i, sub);
    }

    @Override
    public int vars(@Nullable Op type) {
        return ref.vars(type);
    }



    @Override
    public boolean equivalentStructures() {
        return ref.equivalentStructures();
    }

    @Override
    @NotNull
    public Compound compound(int i) {
        return ref.compound(i);
    }


    @Override
    @Nullable
    public <C extends Compound> C cterm(int i) {
        return ref.cterm(i);
    }

    @Override
    public @Nullable boolean subEquals(int i, @NotNull Term x) {
        return ref.subEquals(i, x);
    }

    @Override
    public int intValue(int x, IntObjectToIntFunction<Term> reduce) {
        return ref.intValue(x, reduce);
    }

    @Override
    public @NotNull TreeSet<Term> toSortedSet() {
        return ref.toSortedSet();
    }

    @Override
    public @NotNull Set<Term> toSet() {
        return ref.toSet();
    }

    @Override
    public @NotNull Set<Term> toSet(Predicate<Term> ifTrue) {
        return ref.toSet(ifTrue);
    }


    @Override
    public boolean equalTerms(@NotNull Term[] c) {
        return ref.equalTerms(c);
    }

    @Override
    public Term[] theArray() {
        return ref.theArray();
    }

    @Override
    public Term[] toArray(Term[] x, int from, int to) {
        return ref.toArray(x, from, to);
    }

    @Override
    public @NotNull Term[] terms(@NotNull IntObjectPredicate<Term> filter) {
        return ref.terms(filter);
    }

    @Override
    public void forEachAtomic(@NotNull Consumer<? super Atomic> action) {
        ref.forEachAtomic(action);
    }

    @Override
    public void forEachCompound(@NotNull Consumer<? super Compound> action) {
        ref.forEachCompound(action);
    }


    @Override
    public @NotNull Term[] terms(int start, int end) {
        return ref.terms(start, end);
    }

    @Override
    public int indexOf(@NotNull Term t) {
        return ref.indexOf(t);
    }

    @Override
    public int count(@NotNull Predicate<Term> match) {
        return ref.count(match);
    }

    @Override
    public @NotNull Set<Term> unique(@NotNull Function<Term, Term> each) {
        return ref.unique(each);
    }



    @Override
    public boolean isSorted() {
        return ref.isSorted();
    }



    @Override
    @NotNull
    public TermContainer asFiltered(Predicate<Term> p) {
        return ref.asFiltered(p);
    }

    @Override
    public Stream<Term> subStream() {
        return ref.subStream();
    }


    @Override
    public @NotNull Term[] toArraySubRange(int from, int to) {
        return ref.toArraySubRange(from, to);
    }

    @Override
    public boolean recurseSubTerms(BiPredicate<Term, Compound> whileTrue, Compound parent) {
        return ref.recurseSubTerms(whileTrue, parent);
    }

//    @Override
//    public @Nullable Compound normalize() {
//        return null;
//    }
//
//    @Override
//    public @Nullable Term transform(@NotNull CompoundTransform t) {
//        return null;
//    }
//
//    @Override
//    public @Nullable Term transform(int newDT, @NotNull CompoundTransform t) {
//        return null;
//    }
//
//    @Override
//    public @Nullable Term transform(Op op, int dt, @NotNull CompoundTransform t) {
//        return null;
//    }
//
//    @Override
//    public @NotNull Term eternal() {
//        return null;
//    }
//
//    @Override
//    public @NotNull Term root() {
//        return null;
//    }
//
//    @Override
//    public @NotNull Term conceptual() {
//        return null;
//    }
}
