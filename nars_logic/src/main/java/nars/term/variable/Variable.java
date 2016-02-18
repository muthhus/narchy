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
import nars.term.atom.AbstractStringAtom;
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
    public final Op type;
    public final String str;
    private final int hash;

    protected Variable(Op type, int id) {
        this.type = type;
        this.id = id;
        this.str = type.ch + Integer.toString(id);
        this.hash = (type.ordinal() << 8) | id;
    }

    @Override
    public final boolean equals(Object obj) {
        return obj == this;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final int compareTo(Object o) {
        //hashcode can serve as the ordering too
        return o instanceof Variable ? -1 : Integer.compare(hash, o.hashCode());
    }

    @Override
    public final String toString() {
        return str;
    }

    public static final int MAX_VARIABLE_CACHED_PER_TYPE = 32;

    /**
     * numerically-indexed variable instance cache; prevents duplicates and speeds comparisons
     */
    public static final Variable[][] varCache = new Variable[4][MAX_VARIABLE_CACHED_PER_TYPE];

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
    public final int volume() {
        //TODO decide if this is the case for zero-or-more ellipsis
        return 1;
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


    /**
     * The syntactic complexity of a variable is 0, because it does not refer to
     * any concept.
     *
     * @return The complexity of the term, an integer
     */
    @Override
    public final int complexity() {
        return 0;
    }



}
