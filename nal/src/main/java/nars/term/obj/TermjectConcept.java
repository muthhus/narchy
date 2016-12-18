package nars.term.obj;

import nars.NAR;
import nars.Op;
import nars.Task;
import nars.bag.Bag;
import nars.budget.control.Activation;
import nars.budget.policy.ConceptState;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Terms;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
import nars.term.visit.SubtermVisitor;
import nars.term.visit.SubtermVisitorX;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Created by me on 7/28/16.
 */
public class TermjectConcept<X> implements Concept, Termject<X> {

    @NotNull
    private final Termject<X> termject;
    private final Bag<Term> termLinks;
    private final Bag<Task> taskLinks;
    private ConceptState policy;

    public TermjectConcept(@NotNull Termject<X> t, Bag<Term> termLinks, Bag<Task> taskLinks) {
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
    public boolean isNormalized() {
        return true;
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
    public void recurseTerms(@NotNull SubtermVisitor v) {
        term().recurseTerms(v);
    }

    @Override
    public void recurseTerms(@NotNull SubtermVisitorX v, @Nullable Compound parent) {
        term().recurseTerms(v, parent);
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
    public boolean containsTerm(Termlike t) {
        return term().containsTerm(t);
    }

    @Override
    public boolean hasTemporal() {
        return term().hasTemporal();
    }

    @Override
    public @Nullable Term termOr(int i, @Nullable Term ifOutOfBounds) {
        return term().termOr(i, ifOutOfBounds);
    }

    @Override
    public boolean and(Predicate<Term> v) {
        return term().and(v);
    }

    @Override
    public boolean or(Predicate<Term> v) {
        return term().or(v);
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
    public @NotNull Bag<Task> tasklinks() {
        return taskLinks;
    }

    @Override
    public @NotNull Bag<Term> termlinks() {
        return termLinks;
    }

    @NotNull
    @Override
    public final TermContainer templates() {
        return Terms.NoSubterms;
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
    public void state(@NotNull ConceptState c, NAR nar) {
        this.policy = c;
    }

    @NotNull
    @Override
    public Activation process(@NotNull Task input, NAR nar) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMeta(@NotNull Map newMeta) {

    }
}
