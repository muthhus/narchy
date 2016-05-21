package nars.nal.meta;

import nars.term.atom.AtomicStringConstant;
import org.jetbrains.annotations.NotNull;

/**
 * each precondition is testesd for equality by its toString() method reprsenting an immutable key.
 * so subclasses must implement a valid toString() identifier containing its components.
 * this will only be used at startup when compiling
 *
 * WARNING: no preconditions should store any state so that their instances may be used by
 * different contexts (ex: NAR's)
 */
public abstract class AtomicBoolCondition extends AtomicStringConstant implements BoolCondition {

    public AtomicBoolCondition() {
        super();
    }


    @NotNull
    public abstract String toString();



//    @NotNull
//    public String toJavaConditionString() {
//        return ("(/* TODO: " +
//                this +
//                " */ false)\t");
//    }


}
