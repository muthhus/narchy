package nars.bag.impl;

import nars.bag.Table;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;


public interface SortedTable<V, L> extends Table<V,L> {


    @Nullable L top();

    @Nullable L bottom();

}
