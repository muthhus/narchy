package org.intelligentjava.machinelearning.decisiontree;

import jcog.Texts;
import jcog.list.FasterList;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.List;

/**
 * table of float[]'s of uniform length, each column labeled by a unique header (H)
 */
public class FloatTable<H> {

    public final List<float[]> rows = new FasterList();
    public final H[] cols;

    public FloatTable(@NotNull H... cols) {
        this.cols = cols;
    }

    public FloatTable<H> add(float... row) {
        assert (row.length == cols.length);
        rows.add(row);
        return this;
    }

    public FloatTable<H> print(PrintStream out) {
        rows.stream().map(x -> Texts.n4(x)).forEach(out::println);
        return this;
    }

}
