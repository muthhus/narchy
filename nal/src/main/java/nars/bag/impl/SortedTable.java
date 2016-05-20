package nars.bag.impl;

import nars.bag.Table;
import org.jetbrains.annotations.Nullable;


public interface SortedTable<V, L> extends Table<V,L> {


    @Nullable L top();

    @Nullable L bottom();

}
