/*
 * Term.java
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
package nars.term;


import nars.Op;
import nars.nal.meta.match.Ellipsis;
import nars.term.variable.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static nars.nal.Tense.ETERNAL;
import static nars.nal.Tense.ITERNAL;


public interface Term extends Termed, Comparable, Termlike {



    @NotNull
    @Override default Term term() {
        return this;
    }

    @Nullable
    @Override
    Op op();

    /** volume = total number of terms = complexity + # total variables */
    @Override
    int volume();

    /** total number of leaf terms, excluding variables which have a complexity of zero */
    @Override
    int complexity();


    @Override
    int structure();


    /** number of subterms. if atomic, size=0 */
    @Override
    int size();



    default void recurseTerms(SubtermVisitor v) {
        recurseTerms(v, null);
    }

    void recurseTerms(SubtermVisitor v, Compound parent);


    /**
     * Commutivity in NARS means that a Compound term's
     * subterms will be unique and arranged in order (compareTo)
     *
     * <p>
     * commutative CompoundTerms: Sets, Intersections Commutative Statements:
     * Similarity, Equivalence (except the one with a temporal order)
     * Commutative CompoundStatements: Disjunction, Conjunction (except the one
     * with a temporal order)
     *
     * @return The default value is false
     */
    boolean isCommutative();

    /** provides the "anonymized" form of the compound which is used to reference the concept it would be associated with */
    default Term anonymous() {
        return this;
    }

    /**
     * Whether this compound term contains any variable term
     */
    default boolean hasVar() {
        return vars() > 0;
    }


    //boolean hasVar(final Op type);


    /** tests if contains a term in the structural hash
     *  WARNING currently this does not detect presence of pattern variables
     * */
    default boolean hasAny(@NotNull Op op) {
//        if (op == Op.VAR_PATTERN)
//            return Variable.hasPatternVariable(this);
        return hasAny(op.bit());
    }




//    default boolean hasAll(int structuralVector) {
//        final int s = structure();
//        return (s & structuralVector) == s;
//    }
//

    /** true if the operator's bit is included in the enabld bits of the provided vector */
    @Override default boolean isAny(int structuralVector) {
        int s = op().bit();
        return (s & structuralVector) == s;
    }
    /** for multiple Op comparsions, use Op.or to produce an int and call isAny(int vector) */
    default boolean isAny(@NotNull Op op) {
        return isAny(op.bit());
    }

    /** # of contained independent variables */
    int varIndep();
    /** # of contained dependent variables */
    int varDep();
    /** # of contained query variables */
    int varQuery();


    /** total # of variables, excluding pattern variables */
    int vars();

    default boolean hasVarIndep() {
        return varIndep()!=0;
    }

    /** returns the first ellipsis subterm or null if not present */
    @Nullable
    @Override default Ellipsis firstEllipsis() {
        return null;
    }

    default boolean hasVarDep() {
        return varDep()!=0;
    }

    default boolean hasVarQuery() {
        return varQuery()!=0;
    }



    void append(Appendable w, boolean pretty) throws IOException;

//    default public void append(Writer w, boolean pretty) throws IOException {
//        //try {
//            name().append(w, pretty);
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
//    }

    @NotNull
    StringBuilder toStringBuilder(boolean pretty);

//    default public StringBuilder toStringBuilder(boolean pretty) {
//        return name().toStringBuilder(pretty);
//    }

    @Nullable String toString(boolean pretty);
//    default public String toString(boolean pretty) {
//        return toStringBuilder(pretty).toString();
//    }

    default String toStringCompact() {
        return toString();
    }


    @Override
    default boolean levelValid(int nal) {

        if (nal >= 8) return true;

        int mask = Op.NALLevelEqualAndAbove[nal];
        return (structure() | mask) == mask;
    }

    @NotNull
    default String structureString() {
        return String.format("%16s",
                Integer.toBinaryString(structure()))
                    .replace(" ", "0");
    }

    /** upper 16 bits: ordinal, lower 16 bits: relation (default=-1) */
    @Override default int opRel() {
        return op().ordinal()<<16 | (-1 & 0xffff);
    }

    boolean isCompound();

    @Override
    default boolean isNormalized() {
        return true;
    }

    default long subtermTime(Term x) {
        return subtermTime(x, this instanceof Compound ? ((Compound)this).t() : ITERNAL);
    }

    /** matches the first occuring event's time relative to this temporal relation, with parameter for a hypothetical dt */
    default long subtermTime(Term x, int dt) {

        if (this.equals(x))
            return 0;

        if (!this.op().isTemporal() || dt == ITERNAL)
            return ETERNAL;

        Compound c = ((Compound) this);


        if (dt == 0) {
            if (this.containsTerm(x))
                return 0; //also handles &| multi-arg case
        } else if (this.size() == 2) {

            //use the normalized order of the terms so that the first is always @ 0
            int firstIndex, lastIndex;
            if (dt < 0) {
                dt = -dt;
                firstIndex = 1;
                lastIndex = 0;
            } else {
                firstIndex = 0;
                lastIndex = 1;
            }

            Term first = c.term(firstIndex);
            if (first.equals(x)) return 0;

            Term last = c.term(lastIndex);
            if (last.equals(x)) return dt;

            long withinSubj = first.subtermTime(x);
            if (withinSubj!=ETERNAL)
                return withinSubj;
            long withinPred = last.subtermTime(x);
            if (withinPred!=ETERNAL)
                return dt + withinPred;

        } else {
            //throw new RuntimeException("invalid temporal type: " + this);
            return 0;
        }

        return ETERNAL;
    }

//    default public boolean hasAll(final Op... op) {
//        //TODO
//    }
//    default public boolean hasAny(final Op... op) {
//        //TODO
//    }

}

