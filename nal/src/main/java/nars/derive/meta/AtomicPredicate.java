package nars.derive.meta;

import nars.Op;
import nars.term.atom.AtomicString;
import org.jetbrains.annotations.NotNull;

/**
 * to form the derivater trie, each precondition must provide a key (by: toString())
 * to identify it and determine equality
 *
 * subclasses must implement a valid toString() identifier containing its components.
 * this will only be used at startup when compiling
 *
 * WARNING: no preconditions should store any state so that their instances may be used by
 * different contexts (ex: NAR's)
 *
 * TODO rename this as TermPredicator (builder pattern) that generates AtomicString extending
 * TermPredicate instances with constant name provided in constructor.
 * this will be better than expecting each implementation's toString() method to remain
 * constant
 */
public abstract class AtomicPredicate<X> extends AtomicString implements BoolPredicate<X> {

    @NotNull
    public abstract String toString();


    @Override
    public @NotNull Op op() {
        return Op.ATOM;
    }


    @Override
    public int varIndep() {
        return 0;
    }

    @Override
    public int varDep() {
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

}
