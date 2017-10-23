package nars;


import jcog.list.FasterList;
import nars.derive.match.EllipsisMatch;
import nars.derive.match.Ellipsislike;
import nars.op.mental.AliasConcept;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.compound.GenericCompound;
import nars.term.compound.UnitCompound1;
import nars.term.container.TermContainer;
import nars.term.var.UnnormalizedVariable;
import nars.time.Tense;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.primitive.ObjectByteMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Arrays.copyOfRange;
import static nars.term.Terms.flatten;
import static nars.time.Tense.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * NAL symbol table
 */
public enum Op {


    ATOM(".", Op.ANY_LEVEL, OpType.Other),

    NEG("--", 1, Args.One) {
        @Override
        public Term _the(int dt, Term[] u) {
            assert (u.length == 1); //assert (dt == DTERNAL || dt == XTERNAL);

            Term x = u[0];
            switch (x.op()) {
                case BOOL:
                    return x.neg();
                case NEG:
                    return x.unneg();
                default:
                    return compound(NEG, x);
            }
        }
    },

    INH("-->", 1, OpType.Statement, Args.Two),
    SIM("<->", true, 2, OpType.Statement, Args.Two),

    /**
     * extensional intersection
     */
    SECTe("&", true, 3, Args.GTETwo) {
        @Override
        public Term _the(int dt, Term[] u) {
            return intersect(Int.intersect(u),
                    SECTe,
                    SETe,
                    SETi);
        }
    },

    /**
     * intensional intersection
     */
    SECTi("|", true, 3, Args.GTETwo) {
        @Override
        public Term _the(int dt, Term[] u) {
            return intersect(Int.intersect(u),
                    SECTi,
                    SETi,
                    SETe);
        }
    },

    /**
     * extensional difference
     */
    DIFFe("-", false, 3, Args.Two) {
        @Override
        public Term _the(int dt, Term[] u) {
            return newDiff(this, u);
        }
    },

    /**
     * intensional difference
     */
    DIFFi("~", false, 3, Args.Two) {
        @Override
        public Term _the(int dt, Term[] u) {
            return newDiff(this, u);
        }
    },

    /**
     * PRODUCT
     * classically this is considered NAL4 but due to the use of functors
     * it is much more convenient to classify it in NAL1 so that it
     * along with inheritance (INH), which comprise the functor,
     * can be used to compose the foundation of the system.
     */
    PROD("*", 1, Args.GTEZero),


    /**
     * conjunction
     */
    CONJ("&&", true, 5, Args.GTETwo) {
        @Override
        public Term _the(int dt, Term[] u) {

            final int n = u.length;
            switch (n) {

                case 0:
                    return True;

                case 1:
                    Term only = u[0];

                    if (only instanceof EllipsisMatch) {
                        EllipsisMatch em = (EllipsisMatch) only;
                        Term[] x = em.theArray();
                        return _the(dt, x); //unwrap
                    }

                    //preserve unitary ellipsis for patterns etc
                    return only instanceof Ellipsislike ?
                            new UnitCompound1(CONJ, only) //special; preserve the surrounding conjunction
                            :
                            only;

                default:
                    //continue below
                    break;

            }


            int absoluteness = 0, trues = 0;
            for (Term t : u) {
                if (t instanceof Bool) {
                    if (t == Null) return Null;
                    if (t == False) {
                        absoluteness = -1;
                    } else if (t == True) {
                        trues++;
                        if (absoluteness >= 0)
                            absoluteness = +1; //only if not false, so false overrides
                    }
                }
            }
            if (absoluteness == -1) return False;
            if (absoluteness == +1) {
                if (concurrent(dt)) {

                    //TODO special case where only one item is left, can avoid reconstructing

                    //filter out all boolean terms

                    int size = u.length - trues;
                    if (size == 0)
                        return True;

                    Term[] y = new Term[size];
                    int j = 0;
                    for (int i = 0; j < y.length; i++) {
                        Term uu = u[i];
                        if (uu != True) // && (!uu.equals(False)))
                            y[j++] = uu;
                    }

                    assert (j == y.length);

                    return CONJ.the(dt, y);
                } else {
                    //nothing we can really do. maybe insert a depvar
                    return Null;
                }
            }


            if (dt == DTERNAL || dt == 0) {

                //eternal or commutive
                return implInConjReduction(junctionFlat(dt, u));

            } else {

                //sequence or xternal
                assert(n==2): "invalid non-commutive conjunction arity!=2, arity=" + n;

                if (dt == XTERNAL) {
                    Arrays.sort(u); //pre-sort
                }

                //rebalance and align
                //convention: left align all sequences
                //ex: (x &&+ (y &&+ z))
                //      becomes
                //    ((x &&+ y) &&+ z)

                Term a = u[0];
                Term b = u[1];
//                int eventsLeft = a.eventCount();
//                int eventsRight = b.eventCount();
//                assert(eventsLeft > 0);
//                assert(eventsRight > 0);
//                boolean heavyLeft = (eventsLeft - eventsRight) > 1;
//                boolean heavyRight = (eventsRight - eventsLeft) > 0; // notice the difference in 0, 1. if the # of events is odd, left gets it


                if (dt == XTERNAL) {

                    int va = a.volume();
                    int vb = b.volume();

                    boolean heavyLeft, heavyRight;

                    if (va > vb && a.op() == CONJ && a.dt() == XTERNAL && a.subs() == 2) {
                        int va0 = a.sub(0).volume();
                        int va1 = a.sub(1).volume();

                        int vamin = Math.min(va0, va1);

                        //if left remains heavier by donating its smallest
                        if ((va - vamin) > (vb + vamin)) {
                            int min = va0 <= va1 ? 0 : 1;
                            Term aToB = a.sub(min);
                            return CONJ.the(XTERNAL,
                                    CONJ.the(XTERNAL, b, aToB), a.sub(1 - min));
                        }
                    }

                    //b volume should not be larger than a, it is guaranteed by commutive ordinality convention

                    /*else if (vb > va && b.op() == CONJ && b.dt() == XTERNAL && b.subs() == 2) {
                        int vb0 = b.sub(0).volume();
                        int vb1 = b.sub(1).volume();

                        if (vb - va > Math.min(vb0, vb1)) {
                            int min = vb0 <= vb1 ? 0 : 1;
                            Term bToA = b.sub(min);
                            return CONJ.the(XTERNAL,
                                    CONJ.the(XTERNAL, a, bToA), b.sub(1 - min));
                        }
                    }*/


                    return compound(CONJ, XTERNAL, a, b); //a and b should already be sorted
                } else {


                    {

                        if (dt < 0) { //&& (dt != XTERNAL)
                            Term x = a;
                            a = b;
                            b = x;
                            return conjMerge(a, 0, b, -dt + a.dtRange());
                        } else {
//                                if (heavyRight) {
//                                    return conjMerge(b, 0, a, dt + a.dtRange());
//                                } else {
                            return conjMerge(a, 0, b, dt + a.dtRange());
//                                }
                        }
                    }
                }

//                {
//
//                    int order = a.compareTo(b);
//                    if (order == 0) {
//                        dt = Math.abs(dt);
//                    } else if (order > 0) {
//                        //ensure lexicographic ordering
//                        Term x = u[0];
//                        u[0] = u[1];
//                        u[1] = x; //swap
//                        dt = -dt;
//                    }
//                }
//
//                return implInConjReduction(
//                        compound(CONJ, dt, u)
//                );

            }
        }


        /**
         * flattening conjunction builder, for (commutive) multi-arg conjunction and disjunction (dt == 0 ar DTERNAL)
         * see: https://en.wikipedia.org/wiki/Boolean_algebra#Monotone_laws
         */
        @NotNull
        private Term junctionFlat(int dt, @NotNull final Term... u) {

            //TODO if there are no negations in u then an accelerated construction is possible

            assert (u.length > 1 && (dt == 0 || dt == DTERNAL)); //throw new RuntimeException("should only have been called with dt==0 or dt==DTERNAL");


            //simple accelerated case:
            if (u.length == 2) {
                if ((u[0].op() == NEG && u[0].unneg().equals(u[1])) ||
                        (u[1].op() == NEG && u[1].unneg().equals(u[0])))
                    return False; //co-neg

                //it will already have been sorted and de-duplicated upon arriving here
                // if (u[0].equals(u[1])) return u[0];
                // u = Terms.sorted(u)

                if (!u[0].hasAny(CONJ) && !u[1].hasAny(CONJ)) //if it's simple
                    return compound(CONJ, dt, u);
            }


            ObjectByteHashMap<Term> s = new ObjectByteHashMap<>(u.length);

            if (!flatten(CONJ, u, dt, s))
                return False;

            if (s.isEmpty())
                return True; //? does this happen

            SortedSet<Term> cs = junctionGroupNonDTSubterms(s);
            if (!cs.isEmpty()) {


                //annihilate common terms inside and outside of disjunction
                //      ex:
                //          -X &&  ( X ||  Y)
                //          -X && -(-X && -Y)  |-   -X && Y
                Iterator<Term> csi = cs.iterator();
                List<Term> csa = null;
                while (csi.hasNext()) {
                    Term x = csi.next();

                    if (x.op() == NEG && x.subIs(0, CONJ)) { //DISJUNCTION
                        Term disj = x.unneg();
                        SortedSet<Term> disjSubs = disj.subterms().toSortedSet();
                        //factor out occurrences of the disj's contents outside the disjunction, so remove from inside it
                        if (disjSubs.removeAll(cs)) {
                            //reconstruct disj if changed
                            csi.remove();

                            if (!disjSubs.isEmpty()) {
                                if (csa == null)
                                    csa = $.newArrayList(1);
                                csa.add(CONJ.the(disj.dt(), Terms.sorted(disjSubs)).neg());
                            }
                        }
                    }
                }
                if (csa != null)
                    cs.addAll(csa);

                if (cs.size() == 1)
                    return cs.first();

                Term[] scs = Terms.sorted(cs);
                return !Arrays.equals(scs, u) ?
                        CONJ.the(dt, scs) : //changed, recurse
                        compound(CONJ, dt, scs);
            }

            return Null;
        }


//        /**
//         * array implementation of the conjunction true/false filter
//         */
//        @NotNull
//        private Term[] conjTrueFalseFilter(@NotNull Term... u) {
//            int trues = 0; //# of True subterms that can be eliminated
//            for (Term x : u) {
//                if (x == True) {
//                    trues++;
//                } else if (x == False) {
//
//                    //false subterm in conjunction makes the entire condition false
//                    //this will eventually reduce diectly to false in this method's only callee HACK
//                    return FalseArray;
//                }
//            }
//
//            if (trues == 0)
//                return u;
//
//            int ul = u.length;
//            if (ul == trues)
//                return TrueArray; //reduces to an Imdex itself
//
//            Term[] y = new Term[ul - trues];
//            int j = 0;
//            for (int i = 0; j < y.length; i++) {
//                Term uu = u[i];
//                if (!(uu == True)) // && (!uu.equals(False)))
//                    y[j++] = uu;
//            }
//
//            assert (j == y.length);
//
//            return y;
//        }

        /**
         * this is necessary to keep term structure consistent for intermpolation.
         * by grouping all non-sequence subterms into its own subterm, future
         * flattening and intermpolation is prevented from destroying temporal
         * measurements.
         *
         */
        private SortedSet<Term> junctionGroupNonDTSubterms(ObjectByteMap<Term> s) {

            TreeSet<Term> outer = new TreeSet<>();

            for (ObjectBytePair<Term> xn : s.keyValuesView()) {
                Term x = xn.getOne();
                outer.add(x.negIf(xn.getTwo() < 0));
            }
            return outer;


        }

    },


    /**
     * intensional set
     */
    SETi("[", true, 2, Args.GTEOne) {
        @Override
        public boolean isSet() {
            return true;
        }
    },

    /**
     * extensional set
     */
    SETe("{", true, 2, Args.GTEOne) {
        @Override
        public boolean isSet() {
            return true;
        }
    },


    /**
     * implication
     */
    IMPL("==>", 5, OpType.Statement, Args.Two),


    ///-----------------------------------------------------


    VAR_DEP('#', Op.ANY_LEVEL, OpType.Variable),
    VAR_INDEP('$', 5 /*NAL5..6 for Indep Vars */, OpType.Variable),
    VAR_QUERY('?', Op.ANY_LEVEL, OpType.Variable),
    VAR_PATTERN('%', Op.ANY_LEVEL, OpType.Variable),

    INT("+", Op.ANY_LEVEL, OpType.Other) {

    },

    BOOL("B", Op.ANY_LEVEL, OpType.Other) {

    },
    //SPACE("+", true, 7, Args.GTEOne),


    //VIRTUAL TERMS
    @Deprecated
    INSTANCE("-{-", 2, OpType.Statement, Args.Two) {
        @Override
        @NotNull Term _the(int dt, Term[] u) {
            assert (u.length == 2);
            return INH.the(SETe.the(u[0]), u[1]);
        }
    },

    @Deprecated
    PROPERTY("-]-", 2, OpType.Statement, Args.Two) {
        @Override
        @NotNull Term _the(int dt, Term[] u) {
            assert (u.length == 2);
            return INH.the(u[0], SETi.the(u[1]));
        }
    },

    @Deprecated
    INSTANCE_PROPERTY("{-]", 2, OpType.Statement, Args.Two) {
        @Override
        @NotNull Term _the(int dt, Term[] u) {
            assert (u.length == 2);
            return INH.the(SETe.the(u[0]), SETi.the(u[1]));
        }
    },

    @Deprecated
    DISJ("||", true, 5, Args.GTETwo) {
        @Override
        @NotNull Term _the(int dt, Term[] u) {
            assert (dt == DTERNAL);
            if (u.length == 1 && u[0].op() != VAR_PATTERN)
                return u[0];
            return NEG.the(CONJ.the(Terms.neg(u)));
        }
    },
//    /**
//     * extensional image
//     */
//    IMGe("/", 4, Args.GTEOne),
//
//    /**
//     * intensional image
//     */
//    IMGi("\\", 4, Args.GTEOne),

    /**
     * for ellipsis, when seen as a term
     */
    //SUBTERMS("...", 1, OpType.Other)
    ;
    @NotNull
    public static final Compound ZeroProduct = new GenericCompound(Op.PROD, TermContainer.NoSubterms);

    public static final int StatementBits = Op.or(Op.INH, Op.SIM, Op.IMPL);

    public static final int funcBits = Op.or(Op.ATOM, Op.INH, Op.PROD);
    public static final int funcInnerBits = Op.or(Op.ATOM, Op.PROD);

    public static final byte BELIEF = '.';
    public static final byte QUESTION = '?';
    public static final byte GOAL = '!';
    public static final byte QUEST = '@';
    public static final byte COMMAND = ';';

    public static final String TENSE_PAST = ":\\:";
    public static final String TENSE_PRESENT = ":|:";
    public static final String TENSE_FUTURE = ":/:";

    public static final String TENSE_ETERNAL = ":-:"; //ascii infinity symbol
    public static final String TASK_RULE_FWD = "|-";

    public static final char BUDGET_VALUE_MARK = '$';
    public static final char TRUTH_VALUE_MARK = '%';
    public static final char VALUE_SEPARATOR = ';';

    public static final char ARGUMENT_SEPARATOR = ',';

    public static final char SET_INT_CLOSER = ']';
    public static final char SET_EXT_CLOSER = '}';
    public static final char COMPOUND_TERM_OPENER = '(';
    public static final char COMPOUND_TERM_CLOSER = ')';

    @Deprecated
    public static final char OLD_STATEMENT_OPENER = '<';
    @Deprecated
    public static final char OLD_STATEMENT_CLOSER = '>';

    public static final char STAMP_OPENER = '{';
    public static final char STAMP_CLOSER = '}';
    public static final char STAMP_SEPARATOR = ';';
    public static final char STAMP_STARTER = ':';


    /**
     * bitvector of non-variable terms which can not be part of a goal term
     */
    public static final int NonGoalable = or(IMPL);
    public static final int varBits = Op.or(VAR_PATTERN, VAR_DEP, VAR_QUERY, VAR_INDEP);


    public final boolean allowsBool;

    /**
     * Image index ("imdex") symbol for products, and anonymous variable in products
     */
    public final static char ImdexSym = '_';
    public static final Atomic Imdex =
            new UnnormalizedVariable(Op.VAR_DEP, String.valueOf(ImdexSym)) {

                final int RANK = Term.opX(VAR_PATTERN, 20 /* different from normalized variables with a subOp of 0 */);

                @Override
                public int opX() {
                    return RANK;
                }
            };


    public static final char TrueSym = '†';
    public static final char FalseSym = 'Ⅎ';
    public static final char NullSym = '☢';

    /**
     * absolutely nonsense
     */
    public static final Bool Null = new BoolNull();

    /**
     * absolutely false
     */
    public static final Bool False = new BoolFalse();

    /**
     * absolutely true
     */
    public static final Bool True = new BoolTrue();

    /**
     * specifier for any NAL level
     */
    private static final int ANY_LEVEL = 0;
    public static final int SetBits = or(Op.SETe, Op.SETi);
    public static final int TemporalBits = or(Op.CONJ, Op.IMPL);
    public static final int VariableBits = or(Op.VAR_PATTERN, Op.VAR_INDEP, Op.VAR_DEP, Op.VAR_QUERY);
    public static final int[] NALLevelEqualAndAbove = new int[8 + 1]; //indexed from 0..7, meaning index 7 is NAL8, index 0 is NAL1


    /**
     * whether it is a special or atomic term that isnt conceptualizable.
     * negation is an exception to this, being unconceptualizable itself
     * but it will have conceptualizable=true.
     */
    public final boolean conceptualizable;

    public final boolean beliefable, goalable;

//    public interface TermInstancer {
//
//
//        default @NotNull Term compound(@NotNull NewCompound apc, int dt) {
//            return compound(apc.op(), apc.theArray()).dt(dt);
//        }
//
//        @NotNull Compound compound(Op o, Term[] subterms);
//
//        @NotNull TermContainer subterms(@NotNull Term... x);
//
//    }
//
//    /**
//     * memoization
//     */
//    public static class MemoizedTermInstancer implements TermInstancer {
//
//        final Function<ProtoCompound, Termlike> buildTerm = (C) -> {
//            try {
//
//                Op o = C.op();
//                if (o != null) {
//                    return compound(C);
//                } else
//                    return subterms(C.subterms());
//
//            } catch (InvalidTermException e) {
//                if (Param.DEBUG_EXTRA)
//                    logger.error("Term Build: {}, {}", C, e);
//                return Null;
//            } catch (Throwable t) {
//                logger.error("{}", t);
//                return Null;
//            }
//        };
//
//
//    public static boolean internable(@NotNull Term[] x) {
//        boolean internable = true;
//        for (Term y : x) {
//            if (y instanceof NonInternable) { //"must not intern non-internable" + y + "(" +y.getClass() + ")";
//                internable = false;
//                break;
//            }
//        }
//        return internable;
//    }
//        public static final Memoize<ProtoCompound, Termlike> cache =
//                new HijackMemoize<>(buildTerm, 128 * 1024 + 1, 3);
//        //CaffeineMemoize.builder(buildTerm, 128 * 1024, true /* Param.DEBUG*/);
//
//
//        @NotNull
//        @Override
//        public Term compound(NewCompound apc, int dt) {
//
//            if (apc.OR(x -> x instanceof NonInternable)) {
//                return compound(apc.op, apc, false).dt(dt);
//            } else {
//                Term x = (Term) cache.apply(apc.commit());
//
//                if (dt != DTERNAL && x instanceof Compound)
//                    return x.dt(dt);
//                else
//                    return x;
//            }
//        }
//
//        @Override
//        public @NotNull TermContainer subterms(@NotNull Term... x) {
////        if (s.length < 2) {
////            return _subterms(s);
////        } else {
//
//            boolean internable = internable(x);
//
//            if (internable) {
//                return (TermContainer) cache.apply(new NewCompound(null, x).commit());
//            } else {
//                return TermVector.the(x);
//            }
//
//        }
//    }

    /**
     * creates new instance
     */
    @NotNull
    protected static Term compound(Op o, Term... subterms) {
        return Builder.Compound.the.apply(o, subterms);
    }

    @NotNull
    public static TermContainer subterms(Term... s) {
        return Builder.Subterms.the.apply(s);
    }


    static final ImmutableMap<String, Op> stringToOperator;

    static {
        for (Op o : Op.values()) {
            int l = o.minLevel;
            if (l < 0) l = 0; //count special ops as level 0, so they can be detected there
            for (int i = l; i <= 8; i++) {
                NALLevelEqualAndAbove[i] |= o.bit;
            }
        }

        final Map<String, Op> _stringToOperator = new HashMap<>(values().length * 2);

        //Setup NativeOperator String index hashtable
        for (Op r : Op.values()) {
            _stringToOperator.put(r.toString(), r);

        }
        stringToOperator = Maps.immutable.ofMap(_stringToOperator);

        //System.out.println(Arrays.toString(byteSymbols));


//        //Setup NativeOperator Character index hashtable
//        for (Op r : Op.values()) {
//            char c = r.ch;
//            if (c!=0)
//                Op._charToOperator.put(c, r);
//        }
    }

    /**
     * string representation
     */
    @NotNull
    public final String str;
    /**
     * character representation if symbol has length 1; else ch = 0
     */
    public final char ch;
    public final OpType type;

    /**
     * arity limits, range is inclusive >= <=
     * TODO replace with an IntPredicate
     */
    public final int minSize, maxSize;
    /**
     * minimum NAL level required to use this operate, or 0 for N/A
     */
    public final int minLevel;
    public final boolean commutative;
    public final boolean temporal;
    public final int bit;
    public final boolean var;
    public final boolean atomic;
    public final boolean statement;
    /**
     * whether this involves an additional numeric component: 'dt' (for temporals) or 'relation' (for images)
     */
    public final boolean hasNumeric;
    public final byte id;

    /**
     * whether these are not actual terms, being immediately constructed from other non-virtual types
     */
    public final boolean virtual;

    Op(char c, int minLevel, OpType type) {
        this(c, minLevel, type, Args.None);
    }

    /*
    used only by Termlike.hasAny
    public static boolean hasAny(int existing, int possiblyIncluded) {
        return (existing & possiblyIncluded) != 0;
    }*/


    Op(@NotNull String s, boolean commutative, int minLevel, @NotNull IntIntPair size) {
        this(s, commutative, minLevel, OpType.Other, size);
    }


    Op(char c, int minLevel, OpType type, @NotNull IntIntPair size) {
        this(Character.toString(c), minLevel, type, size);
    }

    Op(@NotNull String string, int minLevel, @NotNull IntIntPair size) {
        this(string, minLevel, OpType.Other, size);
    }

    Op(@NotNull String string, int minLevel, OpType type) {
        this(string, false /* non-commutive */, minLevel, type, Args.None);
    }


    Op(@NotNull String string, int minLevel, OpType type, @NotNull IntIntPair size) {
        this(string, false /* non-commutive */, minLevel, type, size);
    }


    Op(@NotNull String string, boolean commutative, int minLevel, OpType type, @NotNull IntIntPair size) {

        this.id = (byte) (ordinal());
        this.str = string;

        this.commutative = commutative;
        this.minLevel = minLevel;
        this.type = type;

        this.ch = string.length() == 1 ? string.charAt(0) : 0;

        this.minSize = size.getOne();
        this.maxSize = size.getTwo();

        this.var = (type == OpType.Variable);

        this.statement = str.equals("-->") || str.equals("==>") || str.equals("<=>") || str.equals("<->");
        this.temporal = str.equals("&&") || str.equals("==>") || str.equals("<=>");
        //in(or(CONJUNCTION, IMPLICATION, EQUIV));

        this.hasNumeric = temporal;

        //negation does not contribute to structure vector
        this.bit = (1 << ordinal());

        final Set<String> ATOMICS = Set.of(".", "+", "B");
        this.atomic = var || ATOMICS.contains(str);

        switch (str) {
            case "||":
            case "{-]":
            case "-]-":
            case "-{-":
                this.virtual = true;
                break;
            default:
                this.virtual = false;
                break;
        }

        switch (str) {
            case "==>":
            case "&&":
            case "*": //HACK necessary for ellipsematch
                allowsBool = true;
                break;
            default:
                allowsBool = false;
                break;
        }

        conceptualizable = !(var || virtual ||
                str.equals("+") /* INT */ || str.equals("B") /* Bool */);

        goalable = conceptualizable && !str.equals("==>");

        beliefable = conceptualizable;


    }

    public static boolean hasAll(int existing, int possiblyIncluded) {
        return ((existing | possiblyIncluded) == existing);
    }

    public static boolean isTrueOrFalse(@NotNull Term x) {
        return x == True || x == False;
    }

    //CaffeineMemoize.builder(buildTerm, -1 /* softref */, true /* Param.DEBUG*/);
    //new NullMemoize<>(buildTerm);

    public static boolean concurrent(int dt) {
        return (dt == DTERNAL) || (dt == 0);
    }

    final static Comparator<ObjectLongPair<Term>> conjEventComparator = Comparator.comparingLong(ObjectLongPair<Term>::getTwo).thenComparing(ObjectLongPair::getOne);

    /*@NotNull*/
    static public Term conjMerge(Term a, long aStart, Term b, long bStart) {

        TreeSet<ObjectLongPair<Term>> eventSet = new TreeSet(conjEventComparator);

        a.events(eventSet::add, aStart);
        b.events(eventSet::add, bStart);

        int ee = eventSet.size();
        assert (ee > 0);
        if (ee == 1) {
            return eventSet.first().getOne();
        }

        List<ObjectLongPair<Term>> events = new FasterList<>(eventSet);

        //group all parallel clusters
        ObjectLongPair<Term> e0 = events.get(0);

        long headAt = e0.getTwo();
        int groupStart = -1;
        for (int i = 1; i <= ee; i++) {
            long nextAt = (i != ee) ? events.get(i).getTwo() : ETERNAL;
            if (nextAt == headAt) {
                if (groupStart == -1) groupStart = i - 1;
            } else {
                if (groupStart != -1) {
                    int groupEnd = i;
                    Term[] p = new Term[groupEnd - groupStart];
                    assert (p.length > 1);
                    long when = events.get(groupStart).getTwo();
                    for (int k = 0, j = groupStart; j < groupEnd; j++) {
                        p[k++] = events.get(groupStart).getOne();
                        events.remove(groupStart);
                        i--;
                        ee--;
                    }
                    Term replacement = p.length > 1 ? CONJ.the(0, p) : p[0];
                    if (events.isEmpty()) {
                        //got them all here
                        return replacement;
                    }
                    events.add(i, PrimitiveTuples.pair(replacement, when));
                    i++;
                    ee++;
                    groupStart = -1; //reset
                }
            }
            headAt = nextAt;
        }

        return conj(events);
    }

    /**
     * constructs a correctly merged conjunction from a list of events
     */
    @NotNull
    public static Term conj(List<ObjectLongPair<Term>> events) {

        if (events.size() > 1) {
            events.sort(Comparator.comparingLong(ObjectLongPair::getTwo));
            ListIterator<ObjectLongPair<Term>> ii = events.listIterator();
            long prevtime = ETERNAL;
            while (ii.hasNext()) {
                ObjectLongPair<Term> x = ii.next();
                long now = x.getTwo();
                if (prevtime != ETERNAL && prevtime == now) {
                    ii.remove();
                    ObjectLongPair<Term> y = ii.previous();
                    Term xyt = CONJ.the(0, x.getOne(), y.getOne());
                    if (xyt == Null) return Null;
                    if (xyt == False) return False;
                    ObjectLongPair<Term> xy = pair(xyt, now);
                    ii.set(xy);
                    ii.next();
                }
                prevtime = now;
            }
        }

        int ee = events.size();


        switch (ee) {
            case 0:
                return True;
            case 1:
                return events.get(0).getOne();
            default:
                return conjSeq(events);
        }
    }

    /**
     * constructs a correctly merged conjunction from a list of events, in the sublist specified by from..to (inclusive)
     * all of the events should have distinct times before calling here.
     */
    private static Term conjSeq(List<ObjectLongPair<Term>> events) {

        int ee = events.size();


        ObjectLongPair<Term> first = events.get(0);
        switch (ee) {
            case 0:
                throw new NullPointerException("should not be called with empty events list");
            case 1:
                return first.getOne();
            case 2:
                Term left = first.getOne();
                ObjectLongPair<Term> second = events.get(1);
                Term right = second.getOne();
                return conjNonCommFinal(
                        (int) (second.getTwo() - first.getTwo()),
                        left, right);
        }

        int to = ee - 1;
        int center = to / 2;

        Term left = conjSeq(events.subList(0, center + 1));
        if (left == Null) return Null;
        if (left == False) return False; //early fail shortcut

        Term right = conjSeq(events.subList(center + 1, to + 1));
        if (right == Null) return Null;
        if (right == False) return False; //early fail shortcut

        int dt = (int) (events.get(center + 1).getTwo() - first.getTwo() - left.dtRange());

        return conjNonCommFinal(dt, left, right);
    }

    /**
     * HACK
     */
    private static Term conjNonCommFinal(int dt, Term left, Term right) {
        if (left == False) return False;
        if (left == Null) return Null;

        if (right == False) return False;
        if (right == Null) return Null;

        if (left == True) return right;
        if (right == True) return left;


        if (dt == 0 || dt == DTERNAL) {
            return CONJ.the(dt, left, right); //send through again
        } else {

            //System.out.println(left + " " + right + " " + left.compareTo(right));
            //return CONJ.the(dt, left, right);
            if (left.compareTo(right) > 0) {
                //larger on left
                if (dt != XTERNAL)
                    dt = -dt;
                Term t = right;
                right = left;
                left = t;
            }

            int ldt = left.dt();
            int rdt = right.dt();
            if (left.op() == CONJ && !concurrent(ldt) && ldt != XTERNAL &&
                    right.op() == CONJ && !concurrent(rdt) && rdt != XTERNAL &&
                    ((left.subs() > 1 + right.subs()) || (right.subs() > left.subs()))) {
                //seq imbalance
                return CONJ.the(dt, left, right); //send through again
            }

            return implInConjReduction(compound(compound(CONJ, left, right), dt));
        }
    }

    /**
     * precondition combiner: a combination nconjunction/implication reduction
     */
    static private Term implInConjReduction(final Term conj /* possibly a conjunction */) {

        Op xo = conj.op();
        if (xo != CONJ || !conj.hasAny(IMPL))
            return conj; //fall-through
        int conjDT = conj.dt();

        if (/*dt==DTERNAL || */conjDT == XTERNAL)
            return conj;


        //if there is only one implication subterm (first layer only), then fold into that.
        int whichImpl = -1;
        int conjSize = conj.subs();
        Term implication = null;
        int implDT = XTERNAL;
        for (int i = 0; i < conjSize; i++) {
            if (conj.subIs(i, Op.IMPL)) {
                //only handle the first implication in this iteration
                whichImpl = i;
                implication = conj.sub(whichImpl);
                implDT = implication.dt();
                if (implDT == XTERNAL) {
                    //dont proceed any further if XTERNAL
                    return conj;
                }
                break;
            }
        }

        if (implication == null)
            return conj;

        Term other;
        if (conjSize == 2) {
            other = conj.sub(1 - whichImpl);
        } else {
            //more than 2; group them as one term
            TermContainer cs = conj.subterms();
            @NotNull TreeSet<Term> ss = cs.toSortedSet();
            assert (ss.remove(implication)) : "must have removed something";

            Term[] css = Terms.sorted(ss);
            if (conj.dt() == conjDT && cs.equalTerms(css))
                return conj; //prevent recursive loop
            else
                other = xo.the(conjDT, css /* assumes commutive since > 2 */);
        }


        if (whichImpl == 0 && conjDT != DTERNAL && conjDT != XTERNAL) {
            conjDT = -conjDT; //reverse dt if impl is from the 0th subterm
        }

        Term conjInner =
                CONJ.the(conjDT, other, implication.sub(0) /* impl precond */);

        if (conjInner instanceof Bool)
            return Null;

        @NotNull Term implPost = implication.sub(1); /* impl postcondition */

        int preInInner = conjInner.subtermTimeSafe(implication.sub(0));
        if (preInInner == DTERNAL)
            preInInner = 0; //HACK
        int d =
                implDT != DTERNAL ?
                        implDT + preInInner - conjInner.dtRange() :
                        DTERNAL;

        return IMPL.the(d,
                conjInner, implPost);

    }



    @NotNull
    private static Term newDiff(/*@NotNull*/ Op op, Term... t) {

        //corresponding set type for reduction:
        Op set = op == DIFFe ? SETe : SETi;

        switch (t.length) {
            case 1:
                Term single = t[0];
                if (single instanceof EllipsisMatch) {
                    return newDiff(op, ((EllipsisMatch)single).theArray());
                }
                return single instanceof Ellipsislike ?
                        new UnitCompound1(op, single) :
                        Null;
            case 2:
                Term et0 = t[0], et1 = t[1];
                if (et0.equals(et1)
                        || et0.containsRecursively(et1, recursiveCommonalityDelimeterWeak)
                        || et1.containsRecursively(et0, recursiveCommonalityDelimeterWeak))

                    return Null;
                else if ((et0.op() == set && et1.op() == set))
                    return difference(set, et0, et1);
                else
                    return compound(op, t);


        }

        throw new InvalidTermException(op, t, "diff requires 2 terms");

    }

    @NotNull
    public static Term difference(/*@NotNull*/ Op o, @NotNull Term a, @NotNull Term b) {
        assert (!o.temporal) : "this impl currently assumes any constructed term will have dt=DTERNAL";

        if (a.equals(b))
            return Null; //empty set

//        //quick test: intersect the mask: if nothing in common, then it's entirely the first term
//        if ((a.structure() & b.structure()) == 0) {
//            return a;
//        }

        int size = a.subs();
        Collection<Term> terms = o.commutative ? new TreeSet() : $.newArrayList(size);

        for (int i = 0; i < size; i++) {
            Term x = a.sub(i);
            if (!b.contains(x)) {
                terms.add(x);
            }
        }

        int retained = terms.size();
        if (retained == size) { //same as 'a', quick re-use of instance
            return a;
        } else if (retained == 0) {
            return Null; //empty set
        } else {
            return o.the(DTERNAL, terms);
        }

    }

    /**
     * decode a term which may be a functor, return null if it isnt
     */
    @Nullable
    public static <X> Pair<X, Term> functor(Term maybeOperation, Function<Term, X> invokes) {
        if (maybeOperation.hasAll(Op.funcBits)) {
            Term c = maybeOperation;
            if (c.op() == INH) {
                Term s0 = c.sub(0);
                if (s0.op() == PROD) {
                    Term s1 = c.sub(1);
                    if (s1 instanceof Atom /*&& s1.op() == ATOM*/) {
                        X i = invokes.apply(s1);
                        if (i != null)
                            return Tuples.pair(i, s0);
                    }
                }
            }
        }
        return null;
    }


    /**
     * last stage constructor: use with caution
     */
    /*@NotNull*/
    static Term compound(Op op, int dt, Term... subterms) {
        return compound(compound(op, subterms), dt);
    }


//        else
//            return compound(new NewCompound(op, subterms), dt);


    /**
     * last stage constructor: use with caution
     */
    public static Term compound(Term c, int dt) {
        if (dt != DTERNAL && (c instanceof Compound)) {
//            if (c.dt() == dt)
//                return c;

            return new GenericCompoundDT((Compound) c, dt);
        }
        return c;
    }

    static boolean in(int needle, int haystack) {
        return (needle & haystack) == needle;
    }

    public static int or(/*@NotNull*/ Op... o) {
        int bits = 0;
        for (Op n : o)
            bits |= n.bit;
        return bits;
    }

    /**
     * ops across which reflexivity of terms is allowed
     */
    final static int relationDelimeterWeak = Op.or(Op.PROD, Op.CONJ, Op.NEG);
    public static final Predicate<Term> recursiveCommonalityDelimeterWeak =
            c -> !c.isAny(relationDelimeterWeak);
    final static int relationDelimeterStrong = Op.or(Op.PROD);
    public static final Predicate<Term> recursiveCommonalityDelimeterStrong =
            c -> !c.isAny(relationDelimeterStrong);

//    public static final Predicate<Term> onlyTemporal =
//            c -> concurrent(c.dt());
    //c.op()!=CONJ || concurrent(c.dt()); //!c.op().temporal || concurrent(c.dt());

    private static final int InvalidImplicationSubj = or(IMPL);

    /*@NotNull*/
    static Term statement(/*@NotNull*/ Op op, int dt, /*@NotNull*/ Term subject, /*@NotNull*/ Term predicate) {

        if (subject == Null || predicate == Null)
            return Null;


        boolean polarity = true;

        boolean dtConcurrent = concurrent(dt);
        switch (op) {

            case SIM:
            case INH:

                if (isTrueOrFalse(subject) || isTrueOrFalse(predicate))
                    return $.the(subject.equals(predicate));

                if (subject.equals(predicate) || subject.root().equals(predicate.root()))
                    return True;

                break;


            case IMPL:

                //special case for implications: reduce to --predicate if the subject is False
                if (isTrueOrFalse(subject /* antecedent */)) {
                    if (dtConcurrent) {
                        boolean negate = polarity ? (subject == False) : (subject != False);
                        return predicate.negIf(negate);
                    } else {
                        return Null; //no temporal basis
                    }
                }
                if (predicate instanceof Bool)
                    return Null;

                if (subject.hasAny(InvalidImplicationSubj))
                    return Null; //throw new InvalidTermException(op, dt, "Invalid equivalence subject", subject, predicate);
//                if (predicate.hasAny(InvalidImplicationPred))
//                    return Null; //throw new InvalidTermException(op, dt, "Invalid equivalence predicate", subject, predicate);


                if (predicate.op() == NEG) {
                    //negated predicate gets unwrapped to outside
                    predicate = predicate.unneg();
                    polarity = !polarity;
                }


                if (dtConcurrent) {
                    if (subject.unneg().equals(predicate))
                        return polarity ? True : /*False*/Null;
                } //else: allow repeat

                boolean subjConj = subject.op() == CONJ;
                boolean predConj = predicate.op() == CONJ;

                //factor out any common subterms iff concurrent
                if (dtConcurrent) {

                    //factor common events from subj/pred in concurrent impl

                    boolean subjComm = concurrent(subject.dt());
                    boolean predComm = concurrent(predicate.dt());

                    if (subjConj && !predConj && subjComm) {
                        TermContainer subjs = subject.subterms();
                        int i = subjs.indexOf(predicate);
                        if (i != -1) {
                            //probably need to drop from both but for now the safest thing is just to return Null
                            //subject = conjDrop(subject, i);
                            return Null;
                        }
                    } else if (!subjConj && predConj && predComm) {
                        TermContainer preds = predicate.subterms();
                        int i = preds.indexOf(subject);
                        if (i != -1) {
                            //probably need to drop from both but for now the safest thing is just to return Null
                            //predicate = conjDrop(predicate, i);
                            return Null;
                        }

                    }
                    if ((subjConj && predConj) && subjComm && predComm) {
                        final Term csub = subject;
                        TermContainer subjs = csub.subterms();
                        final Term cpred = predicate;
                        TermContainer preds = cpred.subterms();

                        Term[] common = TermContainer.intersect(subjs, preds);
                        if (common != null) {

                            @NotNull Set<Term> sss = subjs.toSortedSet();
                            boolean modifiedS = false;
                            for (Term cc : common)
                                modifiedS |= sss.remove(cc);

                            if (modifiedS) {
                                int s0 = sss.size();
                                switch (s0) {
                                    case 0:
                                        subject = True;
                                        break;
                                    case 1:
                                        subject = sss.iterator().next();
                                        break;
                                    default:
                                        subject = CONJ.the(/*DTERNAL?*/csub.dt(), Terms.sorted(sss));
                                        break;
                                }
                            }

                            @NotNull SortedSet<Term> ppp = preds.toSortedSet();
                            boolean modifiedP = false;
                            for (Term cc : common)
                                modifiedP |= ppp.remove(cc);

                            if (modifiedP) {
                                int s0 = ppp.size();
                                switch (s0) {
                                    case 0:
                                        predicate = True;
                                        break;
                                    case 1:
                                        predicate = ppp.iterator().next();
                                        break;
                                    default:
                                        predicate = CONJ.the(cpred.dt(), Terms.sorted(ppp));
                                        break;
                                }
                            }


                            return IMPL.the(dt, subject, predicate).negIf(!polarity);
                        }

                    }


                    if (subjConj && predConj) {
                        //filter duplicate events
                        int pre = subject.dtRange();
                        int edt = pre + (dt != DTERNAL ? dt : 0);

                        Set<ObjectLongPair<Term>> se = new HashSet();
                        subject.events(se::add);

                        FasterList<ObjectLongPair<Term>> pe = predicate.events(edt);
                        if (pe.removeIf(se::contains)) {
                            if (pe.isEmpty()) {
                                return Null;
                            } else {
                                //duplicates were removed, reconstruct new predicate
                                int ndt = (int) pe.minBy(ObjectLongPair::getTwo).getTwo() - pre;
                                return IMPL.the(ndt, subject, Op.conj(pe)).negIf(!polarity);
                            }
                        }

                    }
                }

//            if (op == INH || op == SIM || dt == 0 || dt == DTERNAL) {
//                if ((subject instanceof Compound && subject.varPattern() == 0 && subject.containsRecursively(predicate)) ||
//                        (predicate instanceof Compound && predicate.varPattern() == 0 && predicate.containsRecursively(subject))) {
//                    return False; //self-reference
//                }
//            }


                // (C ==>+- (A ==>+- B))   <<==>>  ((C &&+- A) ==>+- B)
                if (predicate.op() == IMPL) {
                    Term cpr = predicate;
                    int cprDT = cpr.dt();
                    //if (cprDT != XTERNAL) {
                    Term a = cpr.sub(0);

                    subject = CONJ.the(dt, subject, a);
                    predicate = cpr.sub(1);
                    boolean negate = !polarity;
                    return IMPL.the(cprDT, subject, predicate).negIf(negate);
                    //}
                }


                break;

            default:
                throw new UnsupportedOperationException();
        }


        Predicate<Term> delim = (op == IMPL && dtConcurrent) ?
                recursiveCommonalityDelimeterStrong : Op.recursiveCommonalityDelimeterWeak;

        if ((subject.varPattern() == 0 && predicate.varPattern() == 0) &&
                (op != IMPL || dtConcurrent)) { //apply to: inh, sim, and current impl
            if ((containEachOther(subject, predicate, delim))) {
                //(!(su instanceof Variable) && predicate.contains(su)))
                return Null; //cyclic
            }
            boolean sa = subject instanceof AliasConcept.AliasAtom;
            if (sa) {
                Term sd = ((AliasConcept.AliasAtom) subject).target;
                if (sd.equals(predicate) || containEachOther(sd, predicate, delim))
                    return Null;
            }
            boolean pa = predicate instanceof AliasConcept.AliasAtom;
            if (pa) {
                Term pd = ((AliasConcept.AliasAtom) predicate).target;
                if (pd.equals(subject) || containEachOther(pd, subject, delim))
                    return Null;
            }
            if (sa && pa) {
                if (containEachOther(((AliasConcept.AliasAtom) subject).target, ((AliasConcept.AliasAtom) predicate).target, delim))
                    return Null;
            }

        }

        //already sorted here if commutive
//        if (op.commutative) {
//
////            //normalize co-negation
////            boolean sn = subject.op() == NEG;
////            boolean pn = predicate.op() == NEG;
////
//            if (/*(sn == pn) && */(subject.compareTo(predicate) > 0)) {
//                Term x = predicate;
//                predicate = subject;
//                subject = x;
//                if (dt != XTERNAL && !dtConcurrent)
//                    dt = -dt;
//            }
//
//            //assert (subject.compareTo(predicate) <= 0);
//            //System.out.println( "\t" + subject + " " + predicate + " " + subject.compareTo(predicate) + " " + predicate.compareTo(subject));
//
//        }


        return compound(op, dt, subject, predicate).negIf(!polarity);
    }

    private static Term conjDrop(@NotNull Term conj, int i) {
        TermContainer cs = conj.subterms();
        if (cs.subs() == 2) {
            return conj.sub(1 - i);
        } else {
            Term[] s = cs.theArray();
            int sl = s.length;
            Term[] t = new Term[sl - 1];
            if (i > 0)
                System.arraycopy(s, 0, t, 0, i);
            if (i < s.length - 1)
                System.arraycopy(s, i + 1, t, i, sl - 1 - i);
            return CONJ.the(conj.dt(), t);
        }
    }

    private static boolean containEachOther(Term x, Term y, Predicate<Term> delim) {
        int xv = x.volume();
        int yv = y.volume();
        if (xv == yv)
            return x.containsRecursively(y, delim) || y.containsRecursively(x, delim);
        else if (xv > yv)
            return x.containsRecursively(y, delim);
        else
            return y.containsRecursively(x, delim);
    }

    @Nullable
    public static Op the(String s) {
        return stringToOperator.get(s);
    }

    @NotNull
    private static Term intersect(Term[] t, /*@NotNull*/ Op intersection, /*@NotNull*/ Op setUnion, /*@NotNull*/ Op setIntersection) {

        int trues = 0;
        for (Term x : t) {
            if (x == True) {
                //everything intersects with the "all", so remove this TRUE below
                trues++;
            } else if (x == False) {
                return Null;
            }
        }
        if (trues > 0) {
            if (trues == t.length) {
                return True; //all were true
            } else if (t.length - trues == 1) {
                //find the element which is not true and return it
                for (Term x : t) {
                    if (x != True)
                        return x;
                }
            } else {
                //filter the True statements from t
                Term[] t2 = new Term[t.length - trues];
                int yy = 0;
                for (Term x : t) {
                    if (x != True)
                        t2[yy++] = x;
                }
                t = t2;
            }
        }

        switch (t.length) {

            case 1:

                Term single = t[0];
                if (single instanceof EllipsisMatch) {
                    return intersect(((EllipsisMatch)single).theArray(), intersection, setUnion, setIntersection);
                }
                return single instanceof Ellipsislike ?
                        new UnitCompound1(intersection, single) :
                        single;

            case 2:
                return intersect2(t[0], t[1], intersection, setUnion, setIntersection);
            default:
                //HACK use more efficient way
                Term a = intersect2(t[0], t[1], intersection, setUnion, setIntersection);

                Term b = intersect(copyOfRange(t, 2, t.length), intersection, setUnion, setIntersection);

                return intersect2(a, b,
                        intersection, setUnion, setIntersection
                );
        }

    }

    @NotNull
    @Deprecated
    private static Term intersect2(Term term1, Term term2, /*@NotNull*/ Op intersection, /*@NotNull*/ Op setUnion, /*@NotNull*/ Op setIntersection) {

        if (term1.equals(term2))
            return term1;

        Op o1 = term1.op();
        Op o2 = term2.op();

        if ((o1 == setUnion) && (o2 == setUnion)) {
            //the set type that is united
            return Terms.union(setUnion, term1.subterms(), term2.subterms());
        }


        if ((o1 == setIntersection) && (o2 == setIntersection)) {
            //the set type which is intersected
            return Terms.intersect(setIntersection, term1.subterms(), term2.subterms());
        }

        if (o2 == intersection && o1 != intersection) {
            //put them in the right order so everything fits in the switch:
            Term x = term1;
            term1 = term2;
            term2 = x;
            o2 = o1;
            o1 = intersection;
        }

        //reduction between one or both of the intersection type

        TreeSet<Term> args = new TreeSet<>();
        if (o1 == intersection) {
            ((Iterable<Term>) term1).forEach(args::add);
            if (o2 == intersection)
                ((Iterable<Term>) term2).forEach(args::add);
            else
                args.add(term2);
        } else {
            args.add(term1);
            args.add(term2);
        }

        Term[] aa = args.toArray(new Term[args.size()]);
        if (aa.length == 1)
            return aa[0]; //reduction to one element

        return compound(intersection, aa);
    }

    public static boolean goalable(Term c) {
        return !c.hasAny(Op.NonGoalable);// && c.op().goalable;
    }

    public static TermContainer subterms(Collection<? extends Term> t) {
        return subterms(t.toArray(new Term[t.size()]));
    }

    @NotNull
    @Override
    public String toString() {
        return str;
    }

    /**
     * writes this operator to a Writer in (human-readable) expanded UTF16 mode
     */
    public final void append(@NotNull Compound c, @NotNull Appendable w) throws IOException {
        append(c.dt(), w, false);
    }

    /**
     * writes this operator to a Writer in (human-readable) expanded UTF16 mode
     */
    public final void append(int dt, @NotNull Appendable w, boolean invertDT) throws IOException {


        if (dt == 0) {
            //special case: parallel
            String s;
            switch (this) {
                case CONJ:
                    s = ("&|");
                    break;
                case IMPL:
                    s = ("=|>");
                    break;
//                case EQUI:
//                    s = ("<|>");
//                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            w.append(s);
            return;
        }

        boolean hasTime = dt != Tense.DTERNAL;

        if (hasTime)
            w.append(' ');

        char ch = this.ch;
        if (ch == 0)
            w.append(str);
        else
            w.append(ch);

        if (hasTime) {

            if (invertDT)
                dt = -dt;

            if (dt > 0) w.append('+');
            String ts;
            if (dt == XTERNAL)
                ts = "-";
            else
                ts = Integer.toString(dt);
            w.append(ts).append(' ');

        }
    }

    public boolean commute(int dt, int size) {
        return size > 1 &&
                (commutative && Op.concurrent(dt))
                ;
    }


    public final Term the(/*@NotNull*/ Term... u) {
        return the(DTERNAL, u);
    }

    public final Term the(int dt, /*@NotNull*/ Collection<Term> sub) {
        int s = sub.size();
        return _the(dt, commute(dt, s) ? Terms.sorted(sub) : sub.toArray(new Term[s]));
    }

    /*@NotNull*/
    public final Term the(int dt, Term... u) {
        return _the(dt, commute(dt, u.length) ? Terms.sorted(u) : u);
    }

    /*@NotNull*/ Term _the(int dt, Term[] u) {

        if (statement) {
            if (u.length == 1) { //similarity has been reduced
                assert (this == SIM);
                return u[0] == Null ? Null : True;
            }
            return statement(this, dt, u[0], u[1]);
        } else {
            return compound(this, dt, u);
        }
    }

    /**
     * true if matches any of the on bits of the vector
     */
    public final boolean in(int vector) {
        return in(bit, vector);
    }

    public boolean isSet() {
        return false;
    }

    public static Term without(Term container, Term content) {

        if (container.op().commutative) {
            TermContainer cs = container.subterms();

            int z = cs.subs();
            if (z > 1 && cs.contains(content)) {
                SortedSet<Term> s = cs.toSortedSet();
                if (s.remove(content)) {
                    int zs = s.size();
                    switch (zs) {
                        case 0:
                            return Null;
                        case 1:
                            return s.first();
                        default:
                            return container.op().the(container.dt(), s);
                    }
                }
            }
        } else {
            throw new UnsupportedOperationException("TODO"); //this one is easy
        }
        return Null; //wasnt contained
    }


    /**
     * top-level Op categories
     */
    public enum OpType {
        Statement,
        Variable,
        Other
    }

    enum Args {
        ;
        static final IntIntPair None = pair(0, 0);
        static final IntIntPair One = pair(1, 1);
        static final IntIntPair Two = pair(2, 2);

        static final IntIntPair GTEZero = pair(0, Param.COMPOUND_SUBTERMS_MAX);
        static final IntIntPair GTEOne = pair(1, Param.COMPOUND_SUBTERMS_MAX);
        static final IntIntPair GTETwo = pair(2, Param.COMPOUND_SUBTERMS_MAX);

    }

    public static class InvalidPunctuationException extends RuntimeException {
        public InvalidPunctuationException(byte c) {
            super("Invalid punctuation: " + c);
        }
    }

    private static class BoolNull extends Bool {
        public BoolNull() {
            super(String.valueOf(Op.NullSym));
        }

        final static int rankBoolNull = Term.opX(BOOL, 0);

        @Override
        public final int opX() {
            return rankBoolNull;
        }

        @Override
        public Term neg() {
            return this;
        }

        @Override
        public @NotNull Term unneg() {
            return this;
        }
    }

    private static class BoolFalse extends Bool {
        public BoolFalse() {
            super(String.valueOf(Op.FalseSym));
        }

        final static int rankBoolFalse = Term.opX(BOOL, 1);

        @Override
        public final int opX() {
            return rankBoolFalse;
        }

        @Override
        public Term neg() {
            return True;
        }

        @NotNull
        @Override
        public Term unneg() {
            return True;
        }
    }

    private static class BoolTrue extends Bool {
        public BoolTrue() {
            super(String.valueOf(Op.TrueSym));
        }

        final static int rankBoolTrue = Term.opX(BOOL, 2);

        @Override
        public final int opX() {
            return rankBoolTrue;
        }

        @Override
        public Term neg() {
            return False;
        }

        @NotNull
        @Override
        public Term unneg() {
            return True;
        } //doesnt change
    }
}
