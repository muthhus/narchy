package nars.term;

import nars.Op;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** has, or is associated with a specific term */
public interface Termed /* TODO finish implementing: extends Termlike */ {

    @NotNull Term term();

//    default int size() { return term().size(); }

    default Term sub(int i) { return subterms().sub(i); }

    default TermContainer subterms() { return term().subterms(); }


    default int dt() { return term().dt(); }

    @NotNull
    default Op op() { return term().op(); }

    default int varPattern() {
        return term().varPattern();
    }

    default int varQuery() {
        return term().varQuery();
    }

    default int varIndep() {
        return term().varIndep();
    }

    default int varDep() {
        return term().varDep();
    }

    default boolean levelValid(int nal) {
        return term().levelValid(nal);
    }

    default boolean isNormalized() {
        return term().isNormalized();
    }

    @Nullable static Term termOrNull(@Nullable Termed x) {
        return x == null ? null : x.term();
    }

    default int volume() { return term().volume(); }
    default int complexity() { return term().complexity(); }

    default int structure() { return term().structure(); }

    @NotNull default Term unneg() {
        return term().unneg();
    }

    Termed[] EmptyArray = new Termed[0];

    /** average of complexity and volume */
    default float voluplexity() {
        return (complexity()+volume())/2f;
    }

}
