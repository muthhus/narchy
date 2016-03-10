package nars.truth;

import nars.Symbols;
import nars.util.Texts;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/** abstract typed Truth  */
public interface MetaTruth<T> extends Cloneable, Serializable {

    T value();

    //void setValue(T v); //move to MutableMetaTruth interface

    //TODO add appendString(sb, decimals)
    @NotNull
    default StringBuilder appendString(@NotNull StringBuilder sb) {
        String vs = value().toString();
        int decimals = 2;
        sb.ensureCapacity(3 + (2 + decimals) + vs.length() );
        return sb
                .append(Symbols.TRUTH_VALUE_MARK)
                .append(vs)
                .append(Symbols.VALUE_SEPARATOR)
                .append(Texts.n(conf(), decimals))
                .append(Symbols.TRUTH_VALUE_MARK);
    }

    /**
     * Get the confidence value
     *
     * @return The confidence value
     */
    float conf();



//    /** TODO move this to a MutableTruth interface to separate a read-only impl */
//    void setConfidence(float c);



    @NotNull
    default CharSequence toCharSequence() {
        StringBuilder sb =  new StringBuilder();
        return appendString(sb);
    }

}
