/*
 * Variable.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.term.variable;


import nars.$;
import nars.Op;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Terms;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Normalized variable
 * "highly immutable" and re-used
 */
public abstract class Variable extends Atomic {

    public final int id;
    protected final int hash;

    protected Variable(@NotNull Op type, int id) {

        this.id = id;
        this.hash = (type.ordinal() << 16) | id; //lower 16 bits reserved for the type, which includes all permutations of 2x 8-bit id'd common variables
    }


    //@Override abstract public boolean equals(Object other);

    @Override
    public final boolean equals(Object obj) {
        return obj==this || (obj instanceof Variable) && ((Variable)obj).hash == hash;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final int compareTo(Object o) {
        //hashcode can serve as the ordering too
        if (o == this) return 0;
        return o instanceof Variable ? Integer.compare(hash, o.hashCode()) : 1;
    }

    @Override
    public String toString() {
        return op().ch + Integer.toString(id);
    }

    public static final int MAX_VARIABLE_CACHED_PER_TYPE = 32;

    /**
     * numerically-indexed variable instance cache; prevents duplicates and speeds comparisons
     */
    public static final Variable[][] varCache = new Variable[4][MAX_VARIABLE_CACHED_PER_TYPE];

    @NotNull
    public static Op typeIndex(char c) {
        switch (c) {
            case '%':
                return Op.VAR_PATTERN;
            case '#':
                return Op.VAR_DEP;
            case '$':
                return Op.VAR_INDEP;
            case '?':
                return Op.VAR_QUERY;
        }
        throw new RuntimeException(c + " not a variable");
    }

//    @Override
//    abstract public int volume();

    @Override
    public int volume() {
        //TODO decide if this is the case for zero-or-more ellipsis
        return 1;
    }
    /**
     * The syntactic complexity of a variable is 0, because it does not refer to
     * any concept.
     *
     * @return The complexity of the term, an integer
     */
    @Override
    public int complexity() {
        return 0;
    }

    //    //TODO replace this with a generic counting method of how many subterms there are present
//    public static int numPatternVariables(Term t) {
//        t.value(new TermToInt()) //..
////        final int[] has = {0};
////        t.recurseTerms((t1, superterm) -> {
////            if (t1.op() == Op.VAR_PATTERN)
////                has[0]++;
////        });
////        return has[0];
//    }






}
