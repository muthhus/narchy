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


import nars.$;
import nars.Op;
import nars.term.atom.Atomic;
import nars.term.atom.AtomicSingleton;
import nars.term.container.TermContainer;
import nars.term.subst.FindSubst;
import nars.term.var.AbstractVariable;
import nars.term.var.Variable;
import nars.term.visit.SubtermVisitor;
import nars.term.visit.SubtermVisitorX;
import nars.util.data.array.IntArrays;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static nars.time.Tense.DTERNAL;


public interface Term extends Termed, Termlike, Comparable<Termlike> {


    /**
     * absolute/singular Boolean truths
     */
    AtomicSingleton True = new AtomicSingleton("†");
    AtomicSingleton False = new AtomicSingleton("Ø");


    @NotNull
    @Override
    default Term term() {
        return this;
    }

    @NotNull
    @Override
    Op op();

    @Override
    int volume();

    @Override
    int complexity();

    @Override
    int structure();

//    @Override
//    int size();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    void recurseTerms(@NotNull SubtermVisitor v);

    default void recurseTerms(@NotNull SubtermVisitorX v) {
        recurseTerms(v, null);
    }

    void recurseTerms(@NotNull SubtermVisitorX v, @Nullable Compound parent);


    /**
     * Commutivity in NARS means that a Compound term's
     * subterms will be unique and arranged in order (compareTo)
     * <p>
     * <p>
     * commutative CompoundTerms: Sets, Intersections Commutative Statements:
     * Similarity, Equivalence (except the one with a temporal order)
     * Commutative CompoundStatements: Disjunction, Conjunction (except the one
     * with a temporal order)
     *
     * @return The default value is false
     */
    boolean isCommutative();


    /**
     *
     * @param y another term
     * @param subst the unification context
     * @return whether unification succeeded
     */
    boolean unify(@NotNull Term y, @NotNull FindSubst subst);



    /**
     * true if the operator bit is included in the enabld bits of the provided vector
     */
    default boolean isAny(int bitsetOfOperators) {
        int s = op().bit;
        return (s & bitsetOfOperators) == s;
    }

//    /** for multiple Op comparsions, use Op.or to produce an int and call isAny(int vector) */
//    default boolean isA(@NotNull Op otherOp) {
//        return op() == otherOp;
//    }


//    default boolean hasAll(int structuralVector) {
//        final int s = structure();
//        return (s & structuralVector) == s;
//    }
//


    default boolean hasVarIndep() {
        return varIndep() != 0;
    }

//    /** returns the first ellipsis subterm or null if not present */
//    @Nullable
//    @Override default Ellipsis firstEllipsis() {
//        return null;
//    }

    default boolean hasVarDep() {
        return varDep() != 0;
    }

    default boolean hasVarQuery() {
        return varQuery() != 0;
    }


    void append(@NotNull Appendable w) throws IOException;

//    default public void append(Writer w, boolean pretty) throws IOException {
//        //try {
//            name().append(w, pretty);
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
//    }

    //    default public StringBuilder toStringBuilder(boolean pretty) {
//        return name().toStringBuilder(pretty);
//    }

//    @Deprecated
//    String toString();
//    default public String toString(boolean pretty) {
//        return toStringBuilder(pretty).toString();
//    }


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


    @Override
    default boolean isNormalized() {
        return true;
    }

    default int subtermTime(@NotNull Term x) {
        return subtermTime(x, this instanceof Compound ? ((Compound) this).dt() : DTERNAL);
    }
//    default long subtermTimeOrZero(Term x, long offset) {
//        int e = subtermTime(x, this instanceof Compound ? ((Compound)this).dt() : DTERNAL);
//        return e == DTERNAL ? DTERNAL : e + offset;
//    }

    /**
     * matches the first occuring event's time relative to this temporal relation, with parameter for a hypothetical dt
     */
    default int subtermTime(@NotNull Term x, int dt) {

        x = $.unneg(x); //ignore polarity

        if ($.unneg(this).equalsIgnoringVariables(x))
            return 0;

        if (!this.op().temporal)
            return DTERNAL;

        Compound c = ((Compound) this);

        //TODO use structure to early exit

        if (dt == 0 || dt == DTERNAL) {
            //TODO search better, containsTerm wont work in all cases
            for (Term y : ((Compound)this).terms()) {
                int sdt = y.subtermTime(x);
                if (sdt!=DTERNAL)
                    return sdt;
            }
        } else if (this.size() == 2) {

            int firstIndex, lastIndex;

            if (isCommutative()) {
                //use the normalized order of the terms so that the first is always @ 0

                if (dt < 0) {
                    dt = -dt;

                    firstIndex = 1;
                    lastIndex = 0;
                } else {

                    firstIndex = 0;
                    lastIndex = 1;
                }
            } else {
                firstIndex = 0;
                lastIndex = 1;
            }

            Term first = $.unneg(c.term(firstIndex));
            if (first.equalsIgnoringVariables(x))
                return 0;

            Term last = $.unneg(c.term(lastIndex));
            if (last.equalsIgnoringVariables(x))
                return dt;

            int withinSubj = first.subtermTime(x);
            if (withinSubj != DTERNAL)
                return withinSubj;

            int withinPred = last.subtermTime(x);
            if (withinPred != DTERNAL)
                return dt + withinPred;

        } else {
            //throw new RuntimeException("invalid temporal type: " + this);
            return 0;
        }

        return DTERNAL;
    }

    /**
     * meta is int[] that collects term metadata:
     * 0: patternVar
     * 1: depVars
     * 2: indepVars
     * 3: queryVars
     * 4: volume
     * 5: struct
     * <p>
     * subclasses can override this for more efficient aggregation if certain features are sure to be absent
     */
    default int init(@NotNull int[] meta) {

        if (vars() > 0) {
            meta[0] += varDep();
            meta[1] += varIndep();
            meta[2] += varQuery();
        }

        meta[3] += varPattern();
        meta[4] += volume();
        meta[5] |= structure();

        return hashCode();
    }

    default boolean equalsIgnoringVariables(@NotNull Term other) {
        return (this instanceof Variable) || (other instanceof Variable) || equals(other);
    }


//    default public boolean hasAll(final Op... op) {
//        //TODO
//    }
//    default public boolean hasAny(final Op... op) {
//        //TODO
//    }

    /** returns an int[] path to the first occurrence of the specified subterm
     * @return null if not a subterm, an empty int[] array if equal to this term, or a non-empty int[] array specifying subterm paths to reach it
     */
    @Nullable default byte[] pathTo(@NotNull Term subterm) {
        if (subterm.equals(this))
            return IntArrays.EMPTY_BYTES;
        return null;
    }

    @NotNull
    default ByteList structureKey() {
        return structureKey(new ByteArrayList(volume()*2 /* estimate */));
    }

    @NotNull
    default ByteList structureKey(@NotNull ByteArrayList appendTo) {
        appendTo.add((byte)op().ordinal());
        return appendTo;
    }

    default List<byte[]> pathsTo(Term subterm) {
        List<byte[]> list = $.newArrayList();
        pathsTo(
            (x) -> x.equals(subterm) ? x : null,
            (l,t) -> list.add(l.toArray())
        );
        return list;
    }

    default boolean pathsTo(Term subterm, @NotNull BiPredicate<ByteList,Term> receiver) {
        return pathsTo((x)->subterm.equals(x) ? x : null, receiver);
    }

    default <X> boolean pathsTo(@NotNull Function<Term,X> subterm, @NotNull BiPredicate<ByteList,X> receiver) {
        X ss = subterm.apply(this);
        if (ss!=null)
            return receiver.test(ByteLists.immutable.empty(), ss);
        return true;
    }


    /** GLOBAL TERM COMPARATOR FUNCTION */
    @Override
    default int compareTo(@NotNull Termlike y) {
        if (this == y /*|| this.equals(y)*/) return 0;

        int d = this.op().compareTo(((Term)y).op()); //HACK
        if (d!=0)
            return d;

        if (this instanceof Compound) {

            Compound cx = (Compound)this;
            Compound cy = (Compound)y;

            int diff3 = TermContainer.compare(cx.subterms(),cy.subterms());
            if (diff3 != 0)
                return diff3;

            return Integer.compare(cx.dt(), cy.dt());

        } else if (this instanceof AbstractVariable) {
            //hashcode serves as the ordering too
            return Integer.compare(this.hashCode(), y.hashCode());
        } else if (this instanceof Atomic) {
            //if the op is the same, it is required to be a subclass of Atomic
            //which should have an ordering determined by its toString()
            return this.toString().compareTo((/*(Atomic)*/y).toString());
        }

        throw new RuntimeException("ordering exception: " + this + ", " + y);
    }


    static boolean equalAtemporally(@NotNull Termed a, @NotNull Termed<Compound> b) {
        return ((a.structure() == b.structure()) &&
                (a.op() == b.op()) &&
                (a.volume() == b.volume()) &&
                b.equals(Terms.atemporalize((Compound) a)));
    }
}

