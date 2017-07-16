/*
 * CompoundTerm.java
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

import jcog.Util;
import jcog.data.sexpression.IPair;
import jcog.data.sexpression.Pair;
import nars.$;
import nars.IO;
import nars.Op;
import nars.index.term.TermContext;
import nars.op.mental.Abbreviation;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompoundDT;
import nars.term.container.TermContainer;
import nars.term.subst.Unify;
import nars.term.var.Variable;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.list.mutable.primitive.ByteArrayList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * a compound term
 * TODO make this an interface extending Subterms
 */
public interface Compound extends Term, IPair, TermContainer {

    static boolean equals(Compound a, @Nullable Object b) {
        if (a == b)
            return true;
        if (a.hashCode() != b.hashCode())
            return false;


        Compound bc;
        if (b instanceof Compound) {
            bc = (Compound) b;
        } /*else if (that instanceof CompoundConcept) { //Termed but not Task
            cthat = ((CompoundConcept) that).term();
            if (this == cthat)
                return true;
        } */ else {
            return false;
        }

        return
                (a.subterms().equals(bc.subterms()))
                        &&
                        (a.op() == bc.op())
                        &&
                        (a.dt() == bc.dt())
                ;

        //subterm sharing:
//        if (as != cs) {
//            if (!as.equivalent(cs)) {
//                return false;
//            } else {
//                //share the subterms vector
//                if (cthat instanceof GenericCompound) {
//                    this.subterms = cs; //HACK cast sucks
//                }
//            }
//        }
    }

    @NotNull
    TermContainer subterms();

    @Override
    default int hashCodeSubTerms() {
        return subterms().hashCode();
    }


    @Override
    default int opX() {
        return Term.opX(op(), size());
    }

    /**
     * if the compound tracks normalization state, this will set the flag internally
     */
    default void setNormalized() {
    }

    /**
     * gets the set of unique recursively contained terms of a specific type
     * TODO generalize to a provided lambda predicate selector
     */
    @NotNull
    default MutableSet<Term> recurseTermsToSet(@NotNull Op onlyType) {
        if (!hasAny(onlyType))
            return Sets.mutable.empty();

        MutableSet<Term> t = new UnifiedSet(1);//$.newHashSet(volume() /* estimate */);

        //TODO use an additional predicate to cull subterms which don't contain the target type
        recurseTerms((t1) -> {
            if (t1.op() == onlyType) //TODO make recurseTerms by Op then it can navigate to subterms using structure hash
                t.add(t1);
        });
        return t;
    }


    /**
     * returns whether the set operation caused a change or not
     */
    @NotNull
    default boolean termsToSet(int inStructure, @NotNull Collection<Term> t, boolean addOrRemoved) {
        boolean r = false;

        TermContainer tt = subterms();
        int l = tt.size();
        for (int i = 0; i < l; i++) {
            @NotNull Term s = tt.sub(i);
            if (inStructure == -1 || ((s.structure() & inStructure) > 0)) {
                r |= (addOrRemoved) ? t.add(s) : t.remove(s);
                if (!addOrRemoved && r) //on removal we can exit early
                    return true;
            }
        }
        return r;
    }

    @NotNull
    default boolean termsToSetRecurse(int inStructure, @NotNull Collection<Term> t, boolean addOrRemoved) {
        final boolean[] r = {false};
        recurseTerms((s) -> {

            if (!addOrRemoved && r[0]) { //on removal we can exit early
                return; //HACK todo make a visitor with a predicate termination condition rather than have to continue traversing
            }

            if (inStructure == -1 || ((s.structure() & inStructure) > 0)) {
                r[0] |= (addOrRemoved) ? t.add(s) : t.remove(s);
            }
        });

        return r[0];
    }


    @NotNull
    @Override
    default public Term unneg() {
        //probably rarely called; UnitCompound1 should be used for NEG's
        //if (Param.DEBUG) assert (op() != NEG); //HACK for detection

//        if (op() == NEG) {
//            Term x = sub(0);
//            if (x instanceof Compound && isNormalized()) { //the unnegated content will also be normalized if this is
//                ((Compound) x).setNormalized();
//            }
//            return x;
//        }
        return this;
    }


    //    @NotNull
//    default MutableSet<Term> termsToSet(boolean recurse, int inStructure, MutableSet<Term> t) {
//        if (recurse) {
//            recurseTerms((s) -> {
//                    t.add(s);
//            });
//        } else {
//            for (int i = 0; i < size(); i++) {
//                @NotNull T s = term(i);
//                if ((s.structure() & inStructure) > 0)
//                    t.add(s);
//            }
//        }
//        return t;//.toImmutable();
//    }


    /**
     * temporary: this will be replaced with a smart visitor api
     */
    @Override
    default boolean recurseTerms(BiPredicate<Term, Compound> whileTrue) {

        return recurseTerms(whileTrue, this);
    }

    @Override
    default boolean recurseTerms(BiPredicate<Term, Compound> whileTrue, @Nullable Compound parent) {
        if (whileTrue.test(this, parent)) {
            return subterms().recurseSubTerms(whileTrue, this);
        }
        return false;
    }

    @Override
    default void recurseTerms(@NotNull Consumer<Term> v) {
        v.accept(this);
        //subterms().forEach(s -> s.recurseTerms(v));
        subterms().recurseTerms(v);
    }


    @Override
    default boolean ORrecurse(@NotNull Predicate<Term> p) {
        if (p.test(this))
            return true;
        return subterms().ORrecurse(p);
    }

    @Override
    default boolean ANDrecurse(@NotNull Predicate<Term> p) {
        if (!p.test(this))
            return false;
        return subterms().ANDrecurse(p);
    }

    @Override
    default void init(@NotNull int[] meta) {

        subterms().init(meta);

        meta[5] |= op().bit;

    }

    @Nullable
    default byte[] pathTo(@NotNull Term subterm) {
        if (subterm.equals(this)) return ArrayUtils.EMPTY_BYTE_ARRAY;
        //if (!containsRecursively(subterm)) return null;
        return pathTo(new ByteArrayList(0), this, subterm);
    }


    @Nullable
    default <X> boolean pathsTo(@NotNull Function<Term, X> subterm, @NotNull BiPredicate<ByteList, X> receiver) {
        X ss = subterm.apply(this);
        if (ss != null) {
            if (!receiver.test(ByteLists.immutable.empty(), ss))
                return false;
        }
        return pathsTo(new ByteArrayList(0), this, subterm, receiver);
    }

    @Nullable
    static byte[] pathTo(@NotNull ByteArrayList p, Compound superTerm, @NotNull Term target) {
        if (superTerm.impossibleSubTerm(target))
            return null;

        int n = superTerm.size();
        for (int i = 0; i < n; i++) {
            Term s = superTerm.sub(i);
            if (s.equals(target)) {
                p.add((byte) i);
                return p.toArray();
            }
            if (s instanceof Compound) {
                Compound cs = (Compound) s;
                byte[] pt = pathTo(p, cs, target);
                if (pt != null) {
                    p.add((byte) i);
                    return pt;
                }

            }
        }

        return null;
    }

    @Nullable
    static <X> boolean pathsTo(@NotNull ByteArrayList p, Compound superTerm, @NotNull Function<Term, X> subterm, @NotNull BiPredicate<ByteList, X> receiver) {


        int ppp = p.size();

        int n = superTerm.size();
        for (int i = 0; i < n; i++) {
            Term s = superTerm.sub(i);
            X ss = subterm.apply(s);

            p.add((byte) i);

            if (ss != null) {
                if (!receiver.test(p, ss))
                    return false;
            }
            if (s instanceof Compound) {
                Compound cs = (Compound) s;
                if (!pathsTo(p, cs, subterm, receiver))
                    return false;
            }
            p.removeAtIndex(ppp);
        }

        return true;
    }

    @NotNull
    default ByteList structureKey(@NotNull ByteArrayList appendTo) {
        appendTo.add((byte) op().ordinal());
        appendTo.add((byte) size());
        forEach(x -> {
            x.structureKey(appendTo);
        });
        return appendTo;
    }


    /**
     * unification matching entry point (default implementation)
     *
     * @param y     compound to match against (the instance executing this method is considered 'x')
     * @param subst the substitution context holding the match state
     * @return whether match was successful or not, possibly having modified subst regardless
     */
    @Override
    default boolean unify(@NotNull Term ty, @NotNull Unify subst) {


        if (ty instanceof Compound) {
            if (equals(ty))
                return true;

            if (!subst.freeVars(this))
                return false; //no free vars, the only way unification can proceed is if equal

            Op op = op();
            if (op != ty.op())
                return false;

            int xs;
            if ((xs = size()) != ty.size())
                return false;

            Compound y = (Compound) ty;
            TermContainer xsubs = subterms();
            TermContainer ysubs = y.subterms();


            if (op.temporal && !matchTemporalDT(dt(), y.dt(), subst.dur))
                return false;

            //do not do a fast termcontainer test unless it's linear; in commutive mode we want to allow permutations even if they are initially equal
            if (isCommutative()) {
                return xsubs.unifyCommute(ysubs, subst);
            } else {
                return xsubs.equals(ysubs) || xsubs.unifyLinear(ysubs, subst);
            }

        } else if (ty instanceof Abbreviation.AliasConcept) {
            Compound abbreviated = ((Abbreviation.AliasConcept) ty).abbr.term();
            return abbreviated.equals(this) || unify(abbreviated, subst);
        }

        return false;

    }

    //TODO generalize
    static boolean matchTemporalDT(int a, int b, int dur) {
        if (a == XTERNAL || b == XTERNAL) return true;
        if (a == DTERNAL || b == DTERNAL) return true;
        return Math.abs(a - b) <= dur;
    }

    @Override
    @NotNull
    default Compound term() {
        return this;
    }

    @Override
    default void append(@NotNull Appendable p) throws IOException {
        IO.Printer.append(this, p);
    }


//    @Nullable
//    default Term subterm(@NotNull int... path) {
//        Term ptr = this;
//        for (int i : path) {
//            if ((ptr = ptr.termOr(i, null)) == null)
//                return null;
//        }
//        return ptr;
//    }

    @Nullable
    default Term sub(@NotNull ByteList path) {
        Term ptr = this;
        int s = path.size();
        for (int i = 0; i < s; i++)
            if ((ptr = ptr.sub(path.get(i), null)) == null)
                return null;
        return ptr;
    }

    /**
     * extracts a subterm provided by the address tuple
     * returns null if specified subterm does not exist
     */
    @Nullable
    default Term sub(@NotNull byte... path) {
        return sub(path.length, path);
    }

    @Nullable
    default Term sub(int n, @NotNull byte... path) {
        Term ptr = this;
        for (int i = 0; i < n; i++) {
            if ((ptr = ptr.sub((int) path[i], null)) == null)
                return null;
        }
        return ptr;
    }

    default Term sub(int i, @Nullable Term ifOutOfBounds) {
        return subterms().sub(i, ifOutOfBounds);
    }


    @Nullable
    @Override
    default Object _car() {
        //if length > 0
        return sub(0);
    }

    /**
     * cdr or 'rest' function for s-expression interface when arity > 1
     */
    @Nullable
    @Override
    default Object _cdr() {
        int len = size();
        switch (len) {
            case 1:
                throw new RuntimeException("Pair fault");
            case 2:
                return sub(1);
            case 3:
                return new Pair(sub(1), sub(2));
            case 4:
                return new Pair(sub(1), new Pair(sub(2), sub(3)));
        }

        //this may need tested better:
        Pair p = null;
        for (int i = len - 2; i >= 0; i--) {
            p = new Pair(sub(i), p == null ? sub(i + 1) : p);
        }
        return p;
    }


    @NotNull
    @Override
    default Object setFirst(Object first) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    default Object setRest(Object rest) {
        throw new UnsupportedOperationException();
    }


    @Override
    default int varDep() {
        return subterms().varDep();
    }

    @Override
    default int varIndep() {
        return subterms().varIndep();
    }

    @Override
    default int varQuery() {
        return subterms().varQuery();
    }

    @Override
    default int varPattern() {
        return subterms().varPattern();
    }

    @Override
    default int vars() {
        return subterms().vars();
    }


    @NotNull
    @Override
    default Term sub(int i) {
        return subterms().sub(i);
    }

    @NotNull
    @Override
    default Term[] toArray() {
        return subterms().toArray();
    }


    @Override
    default void forEach(@NotNull Consumer<? super Term> c) {
        subterms().forEach(c);
    }


    @Override
    default int structure() {
        return subterms().structure() | op().bit;
    }


    @Override
    default int size() {
        return subterms().size();
    }

    @Override
    default int complexity() {
        return subterms().complexity(); //already has +1 for this compound
    }

    @Override
    default int volume() {
        return subterms().volume();  //already has +1 for this compound
    }

    @Override
    default boolean impossibleSubTermVolume(int otherTermVolume) {
        return subterms().impossibleSubTermVolume(otherTermVolume);
    }


    @Override
    default boolean isCommutative() {
        if (op().commutative) { //TODO only test dt() if equiv or conj
            int dt = dt();
            switch (dt) {
                case 0:
                case DTERNAL:
                    return (size() > 1);
                case XTERNAL:
                default:
                    return false;
            }
        }
        return false;
    }


    @Override
    default void forEach(@NotNull Consumer<? super Term> action, int start, int stop) {
        subterms().forEach(action, start, stop);
    }


    @Override
    default Iterator<Term> iterator() {
        return subterms().iterator();
    }

    @Override
    default void copyInto(@NotNull Collection<Term> set) {
        subterms().copyInto(set);
    }


//    @Nullable
//    @Override
//    default Ellipsis firstEllipsis() {
//        //return subterms().firstEllipsis();
//        return null;
//    }


    @Override
    boolean isNormalized();

//    /** whether the anonymized form of this term equals x */
//    @Override default boolean equalsAnonymously(@NotNull Term x) {
//
//        if ((opRel()==x.opRel()) && (structure()==x.structure()) && (volume()==x.volume())) { //some simple pre-tests to hopefully avoid needing to anonymize
//
//            return anonymous().equals(x);
//        }
//
//        return false;
//    }


    /**
     * gets temporal relation value
     */
    int dt();

    @Override
    default Term dt(int nextDT) {

        int dt;
        if (op().temporal && nextDT != (dt = this.dt())) {
            Op op = op();
            Compound b = this instanceof GenericCompoundDT ?
                    ((GenericCompoundDT) this).ref : this;

            if ((nextDT != XTERNAL && !concurrent(nextDT)) && size() > 2)
                return Null; //tried to temporalize what can only be commutive

            if (nextDT!=DTERNAL)
                return new GenericCompoundDT(b, nextDT);
            else {
                return op.the(nextDT, toArray());
            }

        } else {
            return this;
        }


    }

    /**
     * similar to a indexOf() call, this will search for a int[]
     * path to the first subterm occurrence of the supplied term,
     * or null if none was found
     */
    @Nullable
    default byte[] isSubterm(@NotNull Term t) {
        if (!impossibleSubTerm(t)) {
            ByteArrayList l = new ByteArrayList();

            if (pathFirst(this, t, l)) {

                return Util.reverse(l);
            }
        }
        return null;
    }


    /**
     * finds the first occurring index path to a recursive subterm equal
     * to 't'
     */

    static boolean pathFirst(@NotNull Compound container, @NotNull Term t, @NotNull ByteArrayList l) {
        int s = container.size();
        for (int i = 0; i < s; i++) {
            Term xx = container.sub(i);
            if (xx.equals(t) || ((xx.contains(t)) && pathFirst((Compound) xx, t, l))) {
                l.add((byte) i);
                return true;
            } //else, try next subterm and its subtree
        }

        return false;
    }


    @Override
    default boolean equalsIgnoringVariables(@NotNull Term other, boolean requireSameTime) {
        if (other instanceof Variable)
            return true;

//        if (op() == NEG)
//            throw new UnsupportedOperationException("left hand side should already be unneg'd");
//
//        if (other.op()==NEG)
//            other = other.unneg();

        Op op = op();
        if (!(other.op() == op))
            return false;

        int s = size();

        if (other.size() == s) {

            if (requireSameTime)
                if (((Compound) other).dt() != dt())
                    return false;

            Compound o = (Compound) other;
            Term[] a = toArray();
            Term[] b = o.toArray();
            for (int i = 0; i < s; i++) {
                if (!a[i].equalsIgnoringVariables(b[i], requireSameTime))
                    return false;
            }
            return true;
        }
        return false;
    }

    @Override
    default boolean isTemporal() {
        return hasAny(Op.TemporalBits) &&
                (isAny(Op.TemporalBits) && (dt() != DTERNAL))
                ||
                (subterms().isTemporal());
    }

    @Override
    default int subtermTime(@NotNull Term x) {

        if (equals(x))
            return 0;

        if (impossibleSubTerm(x))
            return DTERNAL;

        int dt = dt();
        int idt;
        boolean reverse;

        //TODO do shuffled search to return different equivalent results wherever they may appear

        Op op = op();
        boolean shift;
        if (!op.temporal || dt == DTERNAL || dt == XTERNAL || dt == 0) {
            idt = 0; //parallel or eternal, no dt increment
            reverse = false;
            shift = false;
        } else {
            shift = op == CONJ;
            idt = dt;
            if (idt < 0) {
                idt = -idt;
                reverse = true;
            } else {
                reverse = false;
            }
        }

        @NotNull TermContainer yy = subterms();
        int ys = yy.size();
        int offset = 0;
        for (int yi = 0; yi < ys; yi++) {
            Term yyy = yy.sub(reverse ? ((ys - 1) - yi) : yi);
            int sdt = yyy.subtermTime(x);
            if (sdt != DTERNAL)
                return sdt + offset;
            offset += idt + ((shift && yyy.op() == CONJ) ? yyy.dtRange() : 0);
        }

        return DTERNAL; //not found
    }

    @Override
    default void events(List<ObjectLongPair<Term>> events, long offset) {
        Op o = op();
        if (o == CONJ) {
            int dt = dt();

            if (dt != DTERNAL && dt != XTERNAL) {

                boolean reverse;
                if (dt < 0) {
                    dt = -dt;
                    reverse = true;
                } else reverse = false;

                TermContainer tt = subterms();
                int s = tt.size();
                long t = offset;
                for (int i = 0; i < s; i++) {
                    Term st = tt.sub(reverse ? (s - 1 - i) : i);
                    st.events(events, t);
                    t += dt + st.dtRange();
                }

                return;
            }

        }

        events.add(PrimitiveTuples.pair(this, offset));
    }

    @Override
    default int dtRange() {
        Op o = op();
        switch (o) {

//            case NEG:
//                return term(0).dtRange();

            case CONJ: {
                Compound c = (Compound) this;
                int dt = c.dt();
                if (c.size() == 2) {

                    switch (dt) {
                        case DTERNAL:
                        case XTERNAL:
                        case 0:
                            dt = 0;
                            break;
                        default:
                            dt = Math.abs(dt);
                            break;
                    }

                    return c.sub(0).dtRange() + (dt) + c.sub(1).dtRange();

                } else {
                    int s = 0;

                    TermContainer tt = subterms();
                    int l = tt.size();
                    for (int i = 0; i < l; i++) {
                        @NotNull Term x = tt.sub(i);
                        s = Math.max(s, x.dtRange());
                    }

                    return s;
                }
            }

            default:
                return 0;
        }

    }


    @Override
    default Term eval(TermContext index) {

        //the presence of these bits means that somewhere in the subterms is a functor to eval
        if (!isDynamic()) //!hasAll(Op.EvalBits))
            return this;

        //unwrap negation before recursion, it should be more efficient
        Op o = op();
        if (o == NEG) {
            Compound inner = compoundOrNull(unneg());
            if (inner == null)
                return this; //dont go further
            else {
                Term outer = $.neg(inner.eval(index));
                if (outer == null)
                    return this; //dont go further
                else
                    return outer;
            }
        }

        //TermContainer tt = subterms();

        @NotNull final Term[] xy = toArray();
        //any contained evaluables
        boolean subsModified = false;

        if (subterms().hasAll(OpBits)) {
            int s = xy.length;
            for (int i = 0, evalSubsLength = xy.length; i < evalSubsLength; i++) {
                Term x = xy[i];
                Term y = x.eval(index);
                if (y == null) {
                    //if a functor returns null, it means unmodified
                } else if (x != y) {
                    //the result comparing with the x
                    subsModified = true;
                    xy[i] = y;
                }
            }
        }


        //recursively compute contained subterm functors
        if (o == INH) {
            Term possibleArgs = xy[0];
            if (possibleArgs instanceof Compound && possibleArgs.op() == PROD) {
                Term possibleFunc = xy[1];
                if (possibleFunc instanceof Atomic && possibleFunc.op() == ATOM) {
                    Atomic ff = (Atomic) index.getIfPresentElse(possibleFunc);
                    if (ff instanceof Functor) {
                        Term t = ((Functor) ff).apply(((Compound) possibleArgs).subterms());
                        if (t != null)
                            return t;
                    }
                }
            }
        }

        if (!subsModified)
            return this;


        Term u;
        if (subsModified) {
            u = o.the(dt(), xy);
            if (!(u instanceof Compound))
                return u; //atomic, including Bool short-circuits on invalid term
        } else {
            return this;
        }


        if (u.equals(this))
            return this;

        try {
            return u.eval(index);
        } catch (StackOverflowError e) {
            logger.error("eval stack overflow: {} -> {}", this, u);
            return Null;
            //throw new RuntimeException("stack overflow on eval : " + t);
        }
    }

    final static Logger logger = LoggerFactory.getLogger(Compound.class);

    @Nullable
    default Term commonParent(List<byte[]> subpaths) {
        int subpathsSize = subpaths.size();
        assert (subpathsSize > 0);

        int c = 0;

        int shortest = Integer.MAX_VALUE;
        for (int i = 0, subpathsSize1 = subpaths.size(); i < subpathsSize1; i++) {
            shortest = Math.min(shortest, subpaths.get(i).length);
        }

        //find longest common prefix
        int i;
        done:
        for (i = 0; i < shortest; i++) {
            byte toMatch = 0;
            for (int j = 0, subPathsSize; j < subpathsSize; j++) {
                byte[] p = subpaths.get(j);
                if (j == 0) {
                    toMatch = p[i];
                } else if (toMatch != p[i]) {
                    break done; //first mismatch, done
                } //else: continue downwards
            }
            //all matched, proceed downward to the next layer
        }
        if (i == 0)
            return this;
        else {
            return sub(i, subpaths.get(0));
        }

    }


    //    default MutableSet<Term> toSetAtemporal() {
//        int ss = size();
//        MutableSet<Term> s = new UnifiedSet<>(ss);
//        for (int i = 0; i < ss; i++) {
//            s.add(Terms.atemporalize(term(i)));
//        }
//        return s;
//    }


    //    public int countOccurrences(final Term t) {
//        final AtomicInteger o = new AtomicInteger(0);
//
//        if (equals(t)) return 1;
//
//        recurseTerms((n, p) -> {
//            if (n.equals(t))
//                o.incrementAndGet();
//        });
//
//        return o.get();
//    }


//    public static class InvalidTermConstruction extends RuntimeException {
//        public InvalidTermConstruction(String reason) {
//            super(reason);
//        }
//    }


//    /**
//     * single term version of makeCompoundName without iteration for efficiency
//     */
//    @Deprecated
//    protected static CharSequence makeCompoundName(final Op op, final Term singleTerm) {
//        int size = 2; // beginning and end parens
//        String opString = op.toString();
//        size += opString.length();
//        final CharSequence tString = singleTerm.toString();
//        size += tString.length();
//        return new StringBuilder(size).append(COMPOUND_TERM_OPENER).append(opString).append(ARGUMENT_SEPARATOR).append(tString).append(COMPOUND_TERM_CLOSER).toString();
//    }

    //    @Deprecated public static class UnableToCloneException extends RuntimeException {
//
//        public UnableToCloneException(String message) {
//            super(message);
//        }
//
//        @Override
//        public synchronized Throwable fillInStackTrace() {
//            /*if (Parameters.DEBUG) {
//                return super.fillInStackTrace();
//            } else {*/
//                //avoid recording stack trace for efficiency reasons
//                return this;
//            //}
//        }
//
//
//    }


}


//    /** performs a deep comparison of the term structure which should have the same result as normal equals(), but slower */
//    @Deprecated public boolean equalsByTerm(final Object that) {
//        if (!(that instanceof CompoundTerm)) return false;
//
//        final CompoundTerm t = (CompoundTerm)that;
//
//        if (operate() != t.operate())
//            return false;
//
//        if (getComplexity()!= t.getComplexity())
//            return false;
//
//        if (getTemporalOrder()!=t.getTemporalOrder())
//            return false;
//
//        if (!equals2(t))
//            return false;
//
//        if (term.length!=t.term.length)
//            return false;
//
//        for (int i = 0; i < term.length; i++) {
//            if (!term[i].equals(t.term[i]))
//                return false;
//        }
//
//        return true;
//    }
//
//
//
//
//    /** additional equality checks, in subclasses, only called by equalsByTerm */
//    @Deprecated public boolean equals2(final CompoundTerm other) {
//        return true;
//    }

//    /** may be overridden in subclass to include other details */
//    protected int calcHash() {
//        //return Objects.hash(operate(), Arrays.hashCode(term), getTemporalOrder());
//        return name().hashCode();
//    }

//
//    /**
//     * Orders among terms: variable < atomic < compound
//     *
//     * @param that The Term to be compared with the current Term
//\     * @return The order of the two terms
//     */
//    @Override
//    public int compareTo(final AbstractTerm that) {
//        if (this == that) return 0;
//
//        if (that instanceof CompoundTerm) {
//            final CompoundTerm t = (CompoundTerm) that;
//            if (size() == t.size()) {
//                int opDiff = this.operate().ordinal() - t.operate().ordinal(); //should be faster faster than Enum.compareTo
//                if (opDiff != 0) {
//                    return opDiff;
//                }
//
//                int tDiff = this.getTemporalOrder() - t.getTemporalOrder(); //should be faster faster than Enum.compareTo
//                if (tDiff != 0) {
//                    return tDiff;
//                }
//
//                for (int i = 0; i < term.length; i++) {
//                    final int diff = term[i].compareTo(t.term[i]);
//                    if (diff != 0) {
//                        return diff;
//                    }
//                }
//
//                return 0;
//            } else {
//                return size() - t.size();
//            }
//        } else {
//            return 1;
//        }
//    }



    /*
    @Override
    public boolean equals(final Object that) {
        return (that instanceof Term) && (compareTo((Term) that) == 0);
    }
    */


//
//
//
//
//    /**
//     * Orders among terms: variable < atomic < compound
//     *
//     * @param that The Term to be compared with the current Term
//\     * @return The order of the two terms
//     */
//    @Override
//    public int compareTo(final Term that) {
//        /*if (!(that instanceof CompoundTerm)) {
//            return getClass().getSimpleName().compareTo(that.getClass().getSimpleName());
//        }
//        */
//        return -name.compareTo(that.name());
//            /*
//            if (size() == t.size()) {
//                int opDiff = this.operate().ordinal() - t.operate().ordinal(); //should be faster faster than Enum.compareTo
//                if (opDiff != 0) {
//                    return opDiff;
//                }
//
//                for (int i = 0; i < term.length; i++) {
//                    final int diff = term[i].compareTo(t.term[i]);
//                    if (diff != 0) {
//                        return diff;
//                    }
//                }
//
//                return 0;
//            } else {
//                return size() - t.size();
//            }
//        } else {
//            return 1;
//            */
//    }


//    @Override
//    public int compareTo(final Object that) {
//        if (that == this) return 0;
//
//        // variables have earlier sorting order than non-variables
//        if (!(that instanceof Compound)) return 1;
//
//        final Compound c = (Compound) that;
//
//        int opdiff = compareClass(this, c);
//        if (opdiff != 0) return opdiff;
//
//        return compare(c);
//    }

//    public static int compareClass(final Object b, final Object c) {
//        Class c1 = b.getClass();
//        Class c2 = c.getClass();
//        int h = Integer.compare(c1.hashCode(), c2.hashCode());
//        if (h != 0) return h;
//        return c1.getName().compareTo(c2.getName());
//    }

//    /**
//     * compares only the contents of the subterms; assume that the other term is of the same operator type
//     */
//    public int compareSubterms(final Compound otherCompoundOfEqualType) {
//        return Terms.compareSubterms(term, otherCompoundOfEqualType.term);
//    }


//    final static int maxSubTermsForNameCompare = 2; //tunable
//
//    protected int compare(final Compound otherCompoundOfEqualType) {
//
//        int l = length();
//
//        if ((l != otherCompoundOfEqualType.length()) || (l < maxSubTermsForNameCompare))
//            return compareSubterms(otherCompoundOfEqualType);
//
//        return compareName(otherCompoundOfEqualType);
//    }
//
//
//    public int compareName(final Compound c) {
//        return super.compareTo(c);
//    }

//    public final void recurseSubtermsContainingVariables(final TermVisitor v, Term parent) {
//        if (hasVar()) {
//            v.visit(this, parent);
//            //if (this instanceof Compound) {
//            for (Term t : term) {
//                t.recurseSubtermsContainingVariables(v, this);
//            }
//            //}
//        }
//    }

//    @Override
//    public boolean equals(final Object that) {
//        if (this == that)
//            return true;
//
//        if (!(that instanceof Compound)) return false;
//        Compound c = (Compound) that;
//        if (contentHash != c.contentHash ||
//                structureHash != c.structureHash ||
//                volume != c.volume)
//            return false;
//
//        final int s = this.length();
//        Term[] x = this.term;
//        Term[] y = c.term;
//        if (x != y) {
//            boolean canShare =
//                    (structureHash &
//                    ((1 << Op.SEQUENCE.ordinal()) | (1 << Op.PARALLEL.ordinal()))) == 0;
//
//            for (int i = 0; i < s; i++) {
//                Term a = x[i];
//                Term b = y[i];
//                if (!a.equals(b))
//                    return false;
//            }
//            if (canShare) {
//                this.term = (T[]) c.term;
//            }
//            else {
//                this.term = this.term;
//            }
//        }
//
//        if (structure2() != c.structure2() ||
//                op() != c.op())
//            return false;
//
//        return true;
//    }

//    @Override
//    public boolean equals(final Object that) {
//        if (this == that)
//            return true;
//        if (!(that instanceof Compound)) return false;
//
//        Compound c = (Compound) that;
//        if (contentHash != c.contentHash ||
//                structureHash != c.structureHash
//                || volume() != c.volume()
//                )
//            return false;
//
//        final int s = this.length();
//        Term[] x = this.term;
//        Term[] y = c.term;
//        for (int i = 0; i < s; i++) {
//            Term a = x[i];
//            Term b = y[i];
//            if (!a.equals(b))
//                return false;
//        }
//
//        return true;
//    }

    /* UNTESTED
    public Compound clone(VariableTransform t) {
        if (!hasVar())
            throw new RuntimeException("this VariableTransform clone should not have been necessary");

        Compound result = cloneVariablesDeep();
        if (result == null)
            throw new RuntimeException("unable to clone: " + this);

        result.transformVariableTermsDeep(t);

        result.invalidate();

        return result;
    } */


//    /**
//     * true if equal operate and all terms contained
//     */
//    public boolean containsAllTermsOf(final Term t) {
//        if ((op() == t.op())) {
//            return Terms.containsAll(term, ((Compound) t).term);
//        } else {
//            return this.containsTerm(t);
//        }
//    }

//    /**
//     * Try to add a component into a compound
//     *
//     * @param t1 The compound
//     * @param t2 The component
//     * @param memory Reference to the memory
//     * @return The new compound
//     */
//    public static Term addComponents(final CompoundTerm t1, final Term t2, final Memory memory) {
//        if (t2 == null)
//            return t1;
//
//        boolean success;
//        Term[] terms;
//        if (t2 instanceof CompoundTerm) {
//            terms = t1.cloneTerms(((CompoundTerm) t2).term);
//        } else {
//            terms = t1.cloneTerms(t2);
//        }
//        return Memory.make(t1, terms, memory);
//    }


//    /**
//     * Recursively check if a compound contains a term
//     * This method DOES check the equality of this term itself.
//     * Although that is how Term.containsTerm operates
//     *
//     * @param target The term to be searched
//     * @return Whether the target is in the current term
//     */
//    @Override
//    public boolean equalsOrContainsTermRecursively(final Term target) {
//        if (this.equals(target)) return true;
//        return containsTermRecursively(target);
//    }

/**
 * override in subclasses to avoid unnecessary reinit
 */
    /*public CompoundTerm _clone(final Term[] replaced) {
        if (Terms.equals(term, replaced)) {
            return this;
        }
        return clone(replaced);
    }*/

//    @Override
//    public int containedTemporalRelations() {
//        if (containedTemporalRelations == -1) {
//
//            /*if ((this instanceof Equivalence) || (this instanceof Implication))*/
//            {
//                int temporalOrder = this.getTemporalOrder();
//                switch (temporalOrder) {
//                    case TemporalRules.ORDER_FORWARD:
//                    case TemporalRules.ORDER_CONCURRENT:
//                    case TemporalRules.ORDER_BACKWARD:
//                        containedTemporalRelations = 1;
//                        break;
//                    default:
//                        containedTemporalRelations = 0;
//                        break;
//                }
//            }
//
//            for (final Term t : term)
//                containedTemporalRelations += t.containedTemporalRelations();
//        }
//        return this.containedTemporalRelations;
//    }


//    /**
//     * Gives a set of all (unique) contained term, recursively
//     */
//    public Set<Term> getContainedTerms() {
//        Set<Term> s = Global.newHashSet(complexity());
//        for (Term t : term) {
//            s.add(t);
//            if (t instanceof Compound)
//                s.addAll(((Compound) t).getContainedTerms());
//        }
//        return s;
//    }


//    /**
//     * forced deep clone of terms
//     */
//    public ArrayList<Term> cloneTermsListDeep() {
//        ArrayList<Term> l = new ArrayList(length());
//        for (final Term t : term)
//            l.add(t.clone());
//        return l;
//    }



    /*static void shuffle(final Term[] list, final Random randomNumber) {
        if (list.length < 2)  {
            return;
        }


        int n = list.length;
        for (int i = 0; i < n; i++) {
            // between i and n-1
            int r = i + (randomNumber.nextInt() % (n-i));
            Term tmp = list[i];    // swap
            list[i] = list[r];
            list[r] = tmp;
        }
    }*/

/*        public static void shuffle(final Term[] ar,final Random rnd)
        {
            if (ar.length < 2)
                return;



          for (int i = ar.length - 1; i > 0; i--)
          {
            int index = randomNumber.nextInt(i + 1);
            // Simple swap
            Term a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
          }

        }*/

///**
// * Check whether the compound contains a certain component
// * Also matches variables, ex: (&&,<a --> b>,<b --> c>) also contains <a --> #1>
// *  ^^^ is this right? if so then try containsVariablesAsWildcard
// *
// * @param t The component to be checked
// * @return Whether the component is in the compound
// */
//return Terms.containsVariablesAsWildcard(term, t);
//^^ ???

//    /**
//     * Try to replace a component in a compound at a given index by another one
//     *
//     * @param index   The location of replacement
//     * @param subterm The new component
//     * @return The new compound
//     */
//    public Term cloneReplacingSubterm(final int index, final Term subterm) {
//
//        final boolean e = (subterm != null) && (op() == subterm.op());
//
//        //if the subterm is alredy equivalent, just return this instance because it will be equivalent
//        if (subterm != null && (e) && (term[index].equals(subterm)))
//            return this;
//
//        List<Term> list = asTermList();//Deep();
//
//        list.remove(index);
//
//        if (subterm != null) {
//            if (!e) {
//                list.add(index, subterm);
//            } else {
//                //splice in subterm's subterms at index
//                for (final Term t : term) {
//                    list.add(t);
//                }
//
//                /*Term[] tt = ((Compound) subterm).term;
//                for (int i = 0; i < tt.length; i++) {
//                    list.add(index + i, tt[i]);
//                }*/
//            }
//        }
//
//        return Memory.term(this, list);
//    }


//    /**
//     * Check whether the compound contains all term of another term, or
//     * that term as a whole
//     *
//     * @param t The other term
//     * @return Whether the term are all in the compound
//     */
//    public boolean containsAllTermsOf_(final Term t) {
//        if (t instanceof CompoundTerm) {
//        //if (operate() == t.operate()) {
//            //TODO make unit test for containsAll
//            return Terms.containsAll(term, ((CompoundTerm) t).term );
//        } else {
//            return Terms.contains(term, t);
//        }
//    }


//    @Override
//    public boolean equals(final Object that) {
//        if (!(that instanceof CompoundTerm))
//            return false;
//
//        final CompoundTerm t = (CompoundTerm)that;
//        return name().equals(t.name());
//
//        /*if (hashCode() != t.hashCode())
//            return false;
//
//        if (operate() != t.operate())
//            return false;
//
//        if (size() != t.size())
//            return false;
//
//        for (int i = 0; i < term.size(); i++) {
//            final Term c = term.get(i);
//            if (!c.equals(t.componentAt(i)))
//                return false;
//        }
//
//        return true;*/
//
//    }


//boolean transform(CompoundTransform<Compound<Term>, T> trans, int depth);


//    /**
//     * returns result of applySubstitute, if and only if it's a CompoundTerm.
//     * otherwise it is null
//     */
//    default Compound applySubstituteToCompound(Map<Term, Term> substitute) {
//        Term t = Term.substituted(this,
//                new MapSubst(substitute));
//        if (t instanceof Compound)
//            return ((Compound) t);
//        return null;
//    }

//    /**
//     * from: http://stackoverflow.com/a/19333201
//     */
//    public static <Term> void shuffle(final T[] array, final Random random) {
//        int count = array.length;
//
//        //probabality for no shuffle at all:
//        if (random.nextInt(factorial(count)) == 0) return;
//
//        for (int i = count; i > 1; i--) {
//            final int a = i - 1;
//            final int b = random.nextInt(i);
//            if (b!=a) {
//                final T t = array[b];
//                array[b] = array[a];
//                array[a] = t;
//            }
//        }
//    }

//    static Term unwrap(Term x, boolean unwrapLen1SetExt, boolean unwrapLen1SetInt, boolean unwrapLen1Product) {
//        if (x instanceof Compound) {
//            Compound c = (Compound) x;
//            if (c.size() == 1) {
//                if ((unwrapLen1SetInt && (c instanceof SetInt)) ||
//                        (unwrapLen1SetExt && (c instanceof SetExt)) ||
//                        (unwrapLen1Product && (c instanceof Product))
//                        ) {
//                    return c.term(0);
//                }
//            }
//        }
//
//        return x;
//    }


//    @NotNull
//    default Set<Term> recurseTermsToSet() {
//        Set<Term> t = $.newHashSet(volume() /* estimate */);
//        recurseTerms(t::add);
//        return t;
//    }

//    @NotNull
//    default SortedSet<Term> recurseTermsToSortedSet() {
//        TreeSet<Term> t = new TreeSet();
//        recurseTerms((x) -> t.add(x));
//        return t;
//    }
//
//    @NotNull
//    default MutableBiMap<Term, Short> recurseTermsToBiMap() {
//        MutableBiMap<Term, Short> t = new HashBiMap(volume() /* estimate */); //BiMaps.mutable.empty();
//        recurseTerms((x) -> t.putIfAbsent(x, (short) t.size()));
//        return t;
//    }

//
//    @NotNull
//    default boolean termsToSet(@NotNull Collection<Term> t, boolean addOrRemoved) {
//        return termsToSet(-1, t, addOrRemoved);
//    }
