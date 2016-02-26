package nars.term.container;

import com.google.common.collect.Iterators;
import nars.nal.meta.match.Ellipsis;
import nars.term.Term;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Created by me on 2/26/16.
 */
final class EmptyTermContainer implements TermContainer {

    protected EmptyTermContainer() {

    }

    @Override
    public int volume() {
        return 0;
    }

    @Override
    public int complexity() {
        return 0;
    }

    @Override
    public int structure() {
        return 0;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean containsTerm(Term t) {
        return false;
    }

    @Override
    public @Nullable
    Ellipsis firstEllipsis() {
        return null;
    }

    @Override
    public Iterator iterator() {
        return Iterators.emptyIterator();
    }

    @Override
    public int compareTo(Object o) {
        return Integer.compare(hashCode(), o.hashCode());
    }

    @Override
    public int varDep() {
        return 0;
    }

    @Override
    public int varIndep() {
        return 0;
    }

    @Override
    public int varQuery() {
        return 0;
    }

    @Override
    public int varPattern() {
        return 0;
    }

    @Override
    public int vars() {
        return 0;
    }

    @Nullable
    @Override
    public Term term(int i) {
        return null;
    }

    @Override
    public boolean equalTerms(TermContainer c) {
        return false;
    }

    @NotNull
    @Override
    public Term[] terms() {
        return Terms.EmptyArray;
    }

    @Override
    public void forEach(Consumer action, int start, int stop) {

    }

    @Nullable
    @Override
    public TermContainer replacing(int subterm, Term replacement) {
        return null;
    }

    @Override
    public void addAllTo(Collection set) {

    }
}
