package nars.term;

import nars.IO;
import nars.Op;
import nars.index.term.NonInternable;
import nars.index.term.TermContext;
import nars.term.subst.Unify;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;


public class ProxyTerm<T extends Term> implements Term, NonInternable {

    public /*HACK make unpublic */ T ref;

    public ProxyTerm(T t) {
        this.ref = t;
    }

    @Override
    public String toString() {
        return ref.toString();
    }

    @NotNull
    @Override
    public Term term() {
        return ref.term();
    }

    @NotNull
    @Override
    public Op op() {
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
    public boolean equals(Object o) {
        return ref.equals(o);
    }

    @Override
    public int hashCode() {
        return ref.hashCode();
    }

    @Override
    public boolean recurseTerms(BiPredicate<Term, Compound> whileTrue) {
        return ref.recurseTerms(whileTrue);
    }

    @Override
    public boolean recurseTerms(BiPredicate<Term, Compound> whileTrue, @Nullable Compound parent) {
        return ref.recurseTerms(whileTrue, parent);
    }

    @Override
    public boolean recurseTerms(Predicate<Compound> parentsMust, Predicate<Term> whileTrue, Compound parent) {
        return ref.recurseTerms(parentsMust, whileTrue, parent);
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
    public boolean isAny(int bitsetOfOperators) {
        return ref.isAny(bitsetOfOperators);
    }

    @Override
    public boolean hasVarIndep() {
        return ref.hasVarIndep();
    }

    @Override
    public boolean hasVarDep() {
        return ref.hasVarDep();
    }

    @Override
    public boolean hasVarQuery() {
        return ref.hasVarQuery();
    }

    @Override
    public void append(@NotNull Appendable w) throws IOException {
        ref.append(w);
    }

    @Override
    public boolean levelValid(int nal) {
        return ref.levelValid(nal);
    }

    @Override
    @NotNull
    public String structureString() {
        return ref.structureString();
    }

    @Override
    public boolean isNormalized() {
        return ref.isNormalized();
    }

    @Override
    public int subtermTime(@NotNull Term x) {
        return ref.subtermTime(x);
    }

    @Override
    public int dtRange() {
        return ref.dtRange();
    }

    @Override
    public void init(@NotNull int[] meta) {
        ref.init(meta);
    }

    @Override
    public boolean equalsIgnoringVariables(@NotNull Term other, boolean requireSameTime) {
        return ref.equalsIgnoringVariables(other, requireSameTime);
    }

    @Override
    public @Nullable byte[] pathTo(@NotNull Term subterm) {
        return ref.pathTo(subterm);
    }

    @Override
    public @NotNull ByteList structureKey() {
        return ref.structureKey();
    }

    @Override
    public @NotNull ByteList structureKey(@NotNull ByteArrayList appendTo) {
        return ref.structureKey(appendTo);
    }

    @Override
    @NotNull
    public List<byte[]> pathsTo(Term subterm) {
        return ref.pathsTo(subterm);
    }

    @Override
    @NotNull
    public List<byte[]> pathsTo(Term subterm, int minLengthOfPathToReturn) {
        return ref.pathsTo(subterm, minLengthOfPathToReturn);
    }

    @Override
    public boolean pathsTo(@NotNull Term subterm, @NotNull BiPredicate<ByteList, Term> receiver) {
        return ref.pathsTo(subterm, receiver);
    }

    @Override
    public <X> boolean pathsTo(@NotNull Function<Term, X> subterm, @NotNull BiPredicate<ByteList, X> receiver) {
        return ref.pathsTo(subterm, receiver);
    }

    @Override
    public int opX() {
        return ref.opX();
    }

    @Override
    public int compareTo(@NotNull Termlike y) {
        return ref.compareTo(y);
    }

    @NotNull
    @Override
    public Term unneg() {
        return ref.unneg();
    }

    @Override
    public Term eval(TermContext index) {
        return ref.eval(index);
    }

    @Override
    public void events(List<ObjectLongPair<Term>> events, long dt) {
        ref.events(events, dt);
    }


    @Override
    public Term dt(int dt) {
        return ref.dt(dt);
    }

    @Override
    public int varsUnique(@NotNull Op type) {
        return ref.varsUnique(type);
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
    public boolean containsRecursively(Term t) {
        return ref.containsRecursively(t);
    }

    @Override
    public boolean containsRecursively(Term t, Predicate<Compound> inSubtermsOf) {
        return ref.containsRecursively(t, inSubtermsOf);
    }

    @Override
    public boolean isTemporal() {
        return ref.isTemporal();
    }

    @Override
    public boolean hasAll(int structuralVector) {
        return ref.hasAll(structuralVector);
    }

    @Override
    public boolean hasAny(int structuralVector) {
        return ref.hasAny(structuralVector);
    }

    @Override
    public boolean hasAny(@NotNull Op op) {
        return ref.hasAny(op);
    }

    @Override
    public boolean impossibleSubTerm(@NotNull Termlike target) {
        return ref.impossibleSubTerm(target);
    }

    @Override
    public boolean impossibleSubTermOrEqualityVolume(int otherTermsVolume) {
        return ref.impossibleSubTermOrEqualityVolume(otherTermsVolume);
    }

    @Override
    @Nullable
    public <T extends Term> T sub(int i, @Nullable T ifOutOfBounds) {
        return ref.sub(i, ifOutOfBounds);
    }

    @Override
    public boolean impossibleSubTermVolume(int otherTermVolume) {
        return ref.impossibleSubTermVolume(otherTermVolume);
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
    public boolean ANDrecurse(@NotNull Predicate<Term> v) {
        return ref.ANDrecurse(v);
    }

    @Override
    public void recurseTerms(@NotNull Consumer<Term> v) {
        ref.recurseTerms(v);
    }

    @Override
    public boolean OR(Predicate<Term> v) {
        return ref.OR(v);
    }

    @Override
    public boolean ORrecurse(@NotNull Predicate<Term> v) {
        return ref.ORrecurse(v);
    }

    @Override
    public int vars() {
        return ref.vars();
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

}
