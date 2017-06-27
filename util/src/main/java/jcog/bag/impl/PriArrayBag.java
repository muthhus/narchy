package jcog.bag.impl;

import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PriArrayBag<X> extends ArrayBag<X, PriReference<X>> {

    public PriArrayBag(int cap, PriMerge mergeFunction, @NotNull Map<X, PriReference<X>> map) {
        super(cap, mergeFunction, map);
    }

    @Override
    @NotNull
    public final X key(@NotNull PriReference<X> l) {
        return l.get();
    }


}
