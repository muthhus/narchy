package nars.term.obj;

import jcog.bag.Bag;
import jcog.pri.PLink;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.concept.Concept;
import nars.conceptualize.state.ConceptState;
import nars.index.term.TermIndex;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Termlike;
import nars.term.atom.Atomic;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Created by me on 7/28/16.
 */
public class TermjectConcept<X> implements Atomic, Concept, Termject<X> {

    @NotNull
    private final Termject<X> termject;
    private final Bag<Term,PLink<Term>> termLinks;
    private final Bag<Task,PLink<Task>> taskLinks;
    private ConceptState policy;

    public TermjectConcept(@NotNull Termject<X> t, Bag<Term,PLink<Term>> termLinks, Bag<Task,PLink<Task>> taskLinks) {
        this.termject = t;
        this.termLinks = termLinks;
        this.taskLinks = taskLinks;

    }

    @Override
    public void delete(NAR nar) {
        Concept.delete(this, nar);
    }

    @Override
    public @NotNull Op op() {
        return term().op();
    }


    @Override
    public int complexity() {
        return term().complexity();
    }

    @Override
    public int structure() {
        return term().structure();
    }

    @Override
    public void recurseTerms(@NotNull Consumer<Term> v) {
        term().recurseTerms(v);
    }

    @Override
    public boolean recurseTerms(BiPredicate<Term, Compound> whileTrue, @Nullable Compound parent) {
        return term().recurseTerms(whileTrue, parent);
    }

    @Override
    public boolean isCommutative() {
        return term().isCommutative();
    }

    @Override
    public boolean unify(@NotNull Term y, @NotNull Unify subst) {
        return term().unify(y, subst);
    }

    @Override
    public void append(@NotNull Appendable w) throws IOException {
        term().append(w);
    }

    @Override
    public int size() {
        return term().size();
    }

    @Override
    public int hashCode() {
        return term().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Termed && term().equals(((Termed)obj).term());
    }

    @Override
    public boolean contains(Termlike t) {
        return term().contains(t);
    }

    @Override
    public boolean isTemporal() {
        return term().isTemporal();
    }

    @Override
    public @Nullable Term sub(int i, @Nullable Term ifOutOfBounds) {
        return term().sub(i, ifOutOfBounds);
    }

    @Override
    public boolean AND(Predicate<Term> v) {
        return term().AND(v);
    }

    @Override
    public boolean OR(Predicate<Term> v) {
        return term().OR(v);
    }

    @Override
    public int varIndep() {
        return term().varIndep();
    }

    @Override
    public int varDep() {
        return term().varDep();
    }

    @Override
    public int varQuery() {
        return term().varQuery();
    }

    @Override
    public int varPattern() {
        return term().varPattern();
    }

    @Override
    public @NotNull Termject<X> term() {
        return termject;
    }

    @Override
    public int volume() {
        return term().volume();
    }

    @Override
    public X val() {
        return termject.val();
    }

    @Override
    public int compareVal(X v) {
        return termject.compareVal(v);
    }

    @Override
    public Class type() {
        return termject.type();
    }


    @Override
    public @NotNull Bag<Task,PLink<Task>> tasklinks() {
        return taskLinks;
    }

    @Override
    public @NotNull Bag<Term,PLink<Term>> termlinks() {
        return termLinks;
    }


    @Override
    public @Nullable Map<Object, Object> meta() {
        return null;
    }

    @Override
    public @NotNull BeliefTable beliefs() {
        return BeliefTable.EMPTY;
    }

    @Override
    public @NotNull BeliefTable goals() {
        return BeliefTable.EMPTY;
    }

    @Override
    public @NotNull QuestionTable questions() {
        return QuestionTable.EMPTY;
    }

    @Override
    public @Nullable QuestionTable quests() {
        return QuestionTable.EMPTY;
    }


    @Override
    public @Nullable ConceptState state() {
        return policy;
    }

    @Override
    public ConceptState state(@NotNull ConceptState c, NAR nar) {
        ConceptState current = this.policy;
        this.policy = c;
        return current;
    }

    @Override
    public void setMeta(@NotNull Map newMeta) {

    }

    @Override
    public String toString() {
        return term().toString();
    }

}
