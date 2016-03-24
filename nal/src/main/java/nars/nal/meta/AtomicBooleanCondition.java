package nars.nal.meta;

import nars.term.Term;
import nars.term.atom.AtomicStringConstant;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * each precondition is testesd for equality by its toString() method reprsenting an immutable key.
 * so subclasses must implement a valid toString() identifier containing its components.
 * this will only be used at startup when compiling
 *
 * WARNING: no preconditions should store any state so that their instances may be used by
 * different contexts (ex: NAR's)
 */
public abstract class AtomicBooleanCondition<C> extends AtomicStringConstant implements BooleanCondition<C> {

    public AtomicBooleanCondition() {
        super();
    }


    @NotNull
    public abstract String toString();

    /** add this or subconditions to expand in a rule */
    @Override
    public void addTo(@NotNull List<Term> l) {
        //default: just add this
        l.add(this);
    }

//    @NotNull
//    public String toJavaConditionString() {
//        return ("(/* TODO: " +
//                this +
//                " */ false)\t");
//    }


}
