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


import com.google.common.collect.TreeTraverser;
import jcog.data.array.IntArrays;
import nars.$;
import nars.Op;
import nars.index.term.TermContext;
import nars.term.atom.Atomic;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
import nars.term.var.AbstractVariable;
import nars.term.var.GenericVariable;
import nars.term.var.Variable;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.list.primitive.ImmutableByteList;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;

import static nars.Op.False;
import static nars.time.Tense.DTERNAL;


public interface Term extends Termlike, Comparable<Termlike> {


    //@NotNull public static final int[] ZeroIntArray = new int[0];
    @NotNull Term[] EmptyArray = new Term[0];
    ImmutableByteList EmptyByteList = ByteLists.immutable.empty();

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

    void recurseTerms(@NotNull Consumer<Term> v);



    default boolean recurseTerms(BiPredicate<Term, Compound> whileTrue) {
        return recurseTerms(whileTrue, null);
    }


    boolean recurseTerms(BiPredicate<Term, Compound> whileTrue, @Nullable Compound parent);


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
     * @param y     another term
     * @param subst the unification context
     * @return whether unification succeeded
     */
    boolean unify(@NotNull Term y, @NotNull Unify subst);


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

    /**
     * matches the first occuring event's time relative to this temporal relation, with parameter for a hypothetical dt
     *
     * @param dt the current offset in the search
     * @return DTERNAL if the subterm was not found
     */
    default int subtermTime(@NotNull Term x) {
        if (this.equals(x)) //unneg().equalsIgnoringVariables(x))
            return 0;
        else
            return DTERNAL;
    }

    /**
     * total span across time represented by a sequence conjunction compound
     */
    default int dtRange() {
        return 0;
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
    @Override
    default void init(@NotNull int[] meta) {

        if (vars() > 0) {
            meta[0] += varDep();
            meta[1] += varIndep();
            meta[2] += varQuery();
        }

        meta[3] += varPattern();
        meta[4] += volume();
        meta[5] |= structure();

    }

    default boolean equalsIgnoringVariables(@NotNull Term other, boolean requireSameTime) {
        return (this instanceof Variable) || (other instanceof Variable) || equals(other);
    }


//    default public boolean hasAll(final Op... op) {
//        //TODO
//    }
//    default public boolean hasAny(final Op... op) {
//        //TODO
//    }

    /**
     * returns an int[] path to the first occurrence of the specified subterm
     *
     * @return null if not a subterm, an empty int[] array if equal to this term, or a non-empty int[] array specifying subterm paths to reach it
     */
    @Nullable
    default byte[] pathTo(@NotNull Term subterm) {
        return subterm.equals(this) ? IntArrays.EMPTY_BYTES : null;
    }

    @NotNull
    default ByteList structureKey() {
        return structureKey(new ByteArrayList(volume() * 2 /* estimate */));
    }

    @NotNull
    default ByteList structureKey(@NotNull ByteArrayList appendTo) {
        appendTo.add((byte) op().ordinal());
        return appendTo;
    }

    @NotNull
    default List<byte[]> pathsTo(Term subterm) {
        return pathsTo(subterm, 0);
    }

    @NotNull
    default List<byte[]> pathsTo(Term subterm, int minLengthOfPathToReturn) {
        List<byte[]> list = $.newArrayList(0);
        pathsTo(
            (x) -> x.equals(subterm) ? x : null,
            (l, t) -> { if (l.size() >= minLengthOfPathToReturn) list.add(l.toArray()); return true; }
        );
        return list;
    }

    default boolean pathsTo(@NotNull Term subterm, @NotNull BiPredicate<ByteList, Term> receiver) {
        return pathsTo((x) -> subterm.equals(x) ? x : null, receiver);
    }

    default <X> boolean pathsTo(@NotNull Function<Term, X> subterm, @NotNull BiPredicate<ByteList, X> receiver) {
        X ss = subterm.apply(this);
        if (ss != null)
            return receiver.test(EmptyByteList, ss);
        return true;
    }


    /**
     * GLOBAL TERM COMPARATOR FUNCTION
     */
    @Override
    default int compareTo(@NotNull Termlike y) {
        if (this == y /*|| this.equals(y)*/) return 0;

//        int diff2 = Integer.compare(hashCode(), y.hashCode());
//        if (diff2 != 0)
//            return diff2;

        if (this.equals(y)) return 0;

        int d = this.op().compareTo( y.op() );
        if (d != 0)
            return d;

        if (this instanceof Compound) {

            Compound cx = (Compound) this;
            Compound cy = (Compound) y;

            TermContainer cxx = cx.subterms();
            TermContainer cyy = cy.subterms();

            if (!cxx.equals(cyy)) {
                int c = TermContainer.compare(cxx, cyy);
                if (c != 0)
                    return c;
            }

            return Integer.compare(cx.dt(), cy.dt());

        } else if ((this instanceof Atomic) && (y instanceof Atomic)) {

            if ((this instanceof AbstractVariable) && (y instanceof AbstractVariable)) {
                //hashcode serves as the ordering too
                return Integer.compare(hashCode() , y.hashCode() );
            }

            //if the op is the same, it is required to be a subclass of Atomic
            //which should have an ordering determined by its toString()

            boolean gx = this instanceof GenericVariable;
            boolean gy = y instanceof GenericVariable;
            if (gx && !gy)
                return -1;
            if (!gx && gy)
                return +1;

            return this.toString().compareTo((/*(Atomic)*/y).toString());
            //return Hack.compare(toString(), y.toString());

        }

        throw new RuntimeException("ordering exception: " + this + ", " + y);
    }


    /**
     * unwraps any negation superterm
     */
    @NotNull
    @Override
    default Term unneg() {
        return this;
    }

    default Term eval(TermContext index) {
        return this;
    }

    default void events(List<ObjectLongPair<Term>> events, long dt) {
        events.add(PrimitiveTuples.pair(this, dt));
    }


    @NotNull
    static Term falseIfNull(@Nullable Term maybeNull) {
        return (maybeNull==null) ? False : maybeNull;
    }

    /** https://google.github.io/guava/releases/snapshot/api/docs/com/google/common/collect/TreeTraverser.html */
    default TreeTraverser<Term> termverse() {
        return TreeTraverser.using(x -> x instanceof Compound ? ((Compound)x).subterms() : Collections.emptyList());
    }
}

