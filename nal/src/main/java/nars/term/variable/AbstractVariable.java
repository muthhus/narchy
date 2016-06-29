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


import nars.Global;
import nars.Op;
import nars.term.Termlike;
import nars.term.Terms;
import org.jetbrains.annotations.NotNull;

import static nars.Op.VAR_QUERY;

/**
 * Normalized variable
 * "highly immutable" and re-used
 */
public abstract class AbstractVariable implements Variable {

    public final int id;
    protected transient final int hash;
    @NotNull
    private transient final String str;

    protected AbstractVariable(@NotNull Op type, int id) {

        this.id = id;
        this.hash = Terms.hashVar(type, id); //lower 16 bits reserved for the type, which includes all permutations of 2x 8-bit id'd common variables
        this.str = type.ch + Integer.toString(id);
    }

    @Override public final int id() {
        return id;
    }

    //@Override abstract public boolean equals(Object other);

    @Override
    public final boolean equals(@NotNull Object obj) {
        return obj==this ||
                (obj instanceof AbstractVariable && obj.hashCode() == hash); //hash first, it is more likely to differ
                //((obj instanceof Variable) && ((Variable)obj).hash == hash);
    }

    @Override
    public final int compareTo(@NotNull Termlike o) {
        //hashcode serves as the ordering too
        if (o == this) return 0;
        return o instanceof AbstractVariable ? Integer.compare(hash, o.hashCode()) : 1;
    }


    @Override
    public final int hashCode() {
        return hash;
    }



    @NotNull
    @Override
    public String toString() {
        return str;
    }

    /**
     * numerically-indexed variable instance cache; prevents duplicates and speeds comparisons
     */
    private static final AbstractVariable[][] varCache = new AbstractVariable[4][Global.MAX_VARIABLE_CACHED_PER_TYPE];

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
                return VAR_QUERY;
        }
        throw new RuntimeException(c + " not a variable");
    }
//    @NotNull
//    static Op varCacheFor(Op c) {
//        switch (c) {
//            case Op.VAR_PATTERN:
//                return varCache[0]
//            case '#':
//                return varCacheFor
//            case '$':
//                return Op.VAR_INDEP;
//            case '?':
//                return Op.VAR_QUERY;
//        }
//        throw new RuntimeException(c + " not a variable");
//    }
    static int typeIndex(@NotNull Op o) {
        switch (o) {
            case VAR_PATTERN:  return 0;
            case VAR_DEP:  return 1;
            case VAR_INDEP:  return 2;
            case VAR_QUERY: return 3;
            default:
                throw new UnsupportedOperationException();
        }
    }


//    @Override
//    abstract public int volume();

    static {
        //precompute cached variable instances
        for (Op o : new Op[] { Op.VAR_PATTERN, Op.VAR_QUERY, Op.VAR_DEP, Op.VAR_INDEP } ) {
            int t = typeIndex(o);
            for (int i = 0; i < Global.MAX_VARIABLE_CACHED_PER_TYPE; i++) {
                varCache[t][i] = vNew(o, i);
            }
        }
    }

    public static AbstractVariable cached(@NotNull Op type, int counter) {
        if (counter >= Global.MAX_VARIABLE_CACHED_PER_TYPE) {
            return vNew(type, counter); //for special variables like ellipsis
        }

        return varCache[typeIndex(type)][counter];
//        if (v == null) {
//            v = vNew(type, counter);
//            vct[counter] = v;
//        }
    }

    /** TODO move this to TermBuilder */
    @NotNull
    static AbstractVariable vNew(@NotNull Op type, int counter) {
        switch (type) {
            case VAR_PATTERN: return new VarPattern(counter);
            case VAR_QUERY: return  new VarQuery(counter);
            case VAR_DEP: return  new VarDep(counter);
            case VAR_INDEP: return  new VarIndep(counter);
            default:
                throw new UnsupportedOperationException();
        }
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
