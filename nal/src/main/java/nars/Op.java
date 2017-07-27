package nars;


import com.google.common.collect.Range;
import nars.derive.match.Ellipsislike;
import nars.index.term.TermContext;
import nars.term.*;
import nars.term.atom.*;
import nars.term.compound.GenericCompound;
import nars.term.compound.UnitCompound1;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

import static java.util.Arrays.copyOfRange;
import static nars.derive.match.Ellipsis.firstEllipsis;
import static nars.term.Term.nullIfNull;
import static nars.term.Terms.flatten;
import static nars.time.Tense.*;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * NAL symbol table
 */
public enum Op implements $ {


    ATOM(".", Op.ANY_LEVEL, OpType.Other),

    VAR_INDEP('$', 6 /*NAL6 for Indep Vars */, OpType.Variable),
    VAR_DEP('#', Op.ANY_LEVEL, OpType.Variable),
    VAR_QUERY('?', Op.ANY_LEVEL, OpType.Variable),


    NEG("--", 5, Args.One) {
        private Term neg(Term x) {

            if (x instanceof Compound) {
                // (--,(--,P)) = P
                if (x.op() == NEG)
                    return x.unneg();
            } else if (x instanceof Bool) {
                return x.unneg();
            }

            return new UnitCompound1(NEG, x);

        }

        @Override
        public Term the(Term... u) {
            assert (u.length == 1);
            return neg(u[0]);
        }

        @Override
        public Term the(int dt, Term[] u) {
            assert (u.length == 1);
            //assert (dt == DTERNAL || dt == XTERNAL);
            return neg(u[0]);
        }
    },

    INH("-->", 1, OpType.Statement, Args.Two),
    SIM("<->", true, 2, OpType.Statement, Args.Two),

    /**
     * extensional intersection
     */
    SECTe("&", true, 3, Args.GTETwo) {
        @Override
        public Term the(int dt, Term[] u) {
            return intersect(u,
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
        public Term the(int dt, Term[] u) {
            u = Int.intersect(u);
            return intersect(u,
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
        public Term the(int dt, Term[] u) {
            return newDiff(this, u);
        }
    },

    /**
     * intensional difference
     */
    DIFFi("~", false, 3, Args.Two) {
        @Override
        public Term the(int dt, Term[] u) {
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
        public Term the(int dt, Term... tt) {
            //Term[] u = uu.length > 1 ? conjTrueFalseFilter(uu) : uu /* avoid true false filter if fall-through only-term anyway */;


            final int n = tt.length;
            assert (n > 0);
            switch (n) {

                case 1:
                    Term only = tt[0];
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
            for (Term t : tt) {
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

                    int size = tt.length - trues;
                    if (size == 0)
                        return True;

                    Term[] y = new Term[size];
                    int j = 0;
                    for (int i = 0; j < y.length; i++) {
                        Term uu = tt[i];
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


            if (dt == XTERNAL) {
                //leave un-sorted, un-de-duplicated
                return /*conjImplReduction*/compound(CONJ, XTERNAL, tt);
            }

            boolean commutive = concurrent(dt);
            if (commutive) {

                return implInConjReduction(junctionFlat(dt, tt));

            } else {
                //NON-COMMUTIVE


                assert (n == 2) : "invalid non-commutive conjunction arity!=2";

                Term a = tt[0];
                Term b = tt[1];

                //convention: left align all sequences
                //ex: (x &&+ (y &&+ z))
                //      becomes
                //    ((x &&+ y) &&+ z)
                //TODO may need to count # of events, not number of conjunctions.
                boolean aConj = a instanceof Compound && a.op() == CONJ;
                int subEventsLeft = aConj ? conjSubEventCount(a) : 0;
                boolean bConj = b instanceof Compound && b.op() == CONJ;
                int subEventsRight = bConj ? conjSubEventCount(b) : 0;
                if (subEventsLeft > 1 || subEventsRight > 0) {
                    //rebalance and align
                    boolean imbalanced = false;
                    if (Math.abs(subEventsLeft - subEventsRight) > 1)
                        imbalanced = true; //one side has 2 or more than the other

                    if (dt > 0 && (imbalanced || (bConj && !concurrent(b.dt())))) {
                        return nullIfNull(conjMerge(a, 0, b, dt + a.dtRange()));
                    } else if (dt < 0 && (imbalanced || (aConj && !concurrent(a.dt())))) {
                        return nullIfNull(conjMerge(b, 0, a, -dt - b.dtRange()));
                    }
                }

                int order = a.compareTo(b);
                if (order == 0) {
                    dt = Math.abs(dt);
                } else if (order > 0) {
                    //ensure lexicographic ordering
                    Term x = tt[0];
                    tt[0] = tt[1];
                    tt[1] = x; //swap
                    dt = -dt;
                }

                return implInConjReduction(
                        compound(CONJ, dt, tt)
                );

            }
        }

        private int conjSubEventCount(Term a) {
            //TODO make an int reducer method for Term
            final int[] subevents = {0};
            ((Compound) a).recurseTerms(sub -> {
                if (sub.op() == CONJ)
                    subevents[0] += sub.size();
            });
            return subevents[0];

            //return ((Compound)a).count(t -> t.op()==CONJ && !concurrent(t.dt()));
        }

        /**
         * flattening conjunction builder, for (commutive) multi-arg conjunction and disjunction (dt == 0 ar DTERNAL)
         * see: https://en.wikipedia.org/wiki/Boolean_algebra#Monotone_laws
         */
        @NotNull
        private Term junctionFlat(int dt, @NotNull final Term... u) {

            //TODO if there are no negations in u then an accelerated construction is possible

            assert (u.length > 0 && (dt == 0 || dt == DTERNAL || dt == XTERNAL)); //throw new RuntimeException("should only have been called with dt==0 or dt==DTERNAL");

            ObjectByteHashMap<Term> s = new ObjectByteHashMap<>(u.length);

            if (flatten(CONJ, u, dt, s) && !s.isEmpty()) {
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
                            Compound disj = (Compound) x.unneg();
                            SortedSet<Term> disjSubs = disj.toSortedSet();
                            //factor out occurrences of the disj's contents outside the disjunction, so remove from inside it
                            if (disjSubs.removeAll(cs)) {
                                //reconstruct disj if changed
                                csi.remove();

                                if (!disjSubs.isEmpty()) {
                                    Term y = NEG.the(CONJ.the(disj.dt(), Terms.sorted(disjSubs)));
                                    if (csa == null)
                                        csa = $.newArrayList(1);
                                    csa.add(y);
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
            }

            return False;
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
        private @NotNull SortedSet<Term> junctionGroupNonDTSubterms(@NotNull ObjectByteMap<Term> s) {

            TreeSet<Term> outer = new TreeSet<>();

            for (ObjectBytePair<Term> xn : s.keyValuesView()) {
                Term x = xn.getOne();
                outer.add($.negIf(x, xn.getTwo() < 0));
            }
            return outer;


        }

        /** precondition combiner: a combination nconjunction/implication reduction */
        private Term implInConjReduction(final Term x /* possibly a conjunction */) {

            Op xo = x.op();
            if (xo != CONJ || !x.hasAny(IMPL))
                return x; //fall-through
            int dt = x.dt();

            if (/*dt==DTERNAL || */dt == XTERNAL)
                return x;


            //if there is only one implication subterm (first layer only), then fold into that.
            final Compound conj = (Compound) x;
            int whichImpl = -1;
            int conjSize = conj.size();
            Compound implication = null;
            int implDT = XTERNAL;
            for (int i = 0; i < conjSize; i++) {
                if (conj.subIs(i, Op.IMPL)) {
                    //only handle the first implication in this iteration
                    whichImpl = i;
                    implication = (Compound) conj.sub(whichImpl);
                    implDT = implication.dt();
                    if (implDT == XTERNAL) {
                        //dont proceed any further if XTERNAL
                        return x;
                    }
                    break;
                }
            }

            if (implication == null)
                return x;

            Term others;
            int conjDT = conj.dt();
            if (conjSize == 2) {
                others = x.sub(1 - whichImpl, null);
            } else {
                //more than 2; group them as one term
                @NotNull TreeSet<Term> ss = conj.toSortedSet();
                assert (ss.remove(implication)) : "must have removed something";

                others = xo.the(conjDT, Terms.sorted(ss) /* assumes commutive since > 2 */);
            }

            if (whichImpl == 0)
                conjDT = -conjDT; //reverse dt if impl is from the 0th subterm

            Term ia =
                    CONJ.the(conjDT, others, implication.sub(0) /* impl precond */);

            @NotNull Term ib = implication.sub(1); /* impl postcondition */
            return IMPL.the(implDT, ia, ib);
        }
    },


    //SPACE("+", true, 7, Args.GTEOne),

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

    /**
     * equivalence
     */
    EQUI("<=>", true, 5, OpType.Statement, Args.Two),


    // keep all items which are invlved in the lower 32 bit structuralHash above this line
    // so that any of their ordinal values will not exceed 31
    //-------------
    //NONE('\u2205', Op.ANY, null),

    INT("+", Op.ANY_LEVEL, OpType.Other) {

    },

    VAR_PATTERN('%', Op.ANY_LEVEL, OpType.Variable),


    //VIRTUAL TERMS
    @Deprecated
    INSTANCE("-{-", 2, OpType.Statement, Args.Two),

    @Deprecated
    PROPERTY("-]-", 2, OpType.Statement, Args.Two),

    @Deprecated
    INSTANCE_PROPERTY("{-]", 2, OpType.Statement, Args.Two),

    @Deprecated
    DISJ("||", true, 5, Args.GTETwo) {
        @Override
        public Term the(int dt, Term... u) {
            assert (dt == DTERNAL);
            assert (u.length > 1 || u[0].op() == VAR_PATTERN);
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

    public static final int StatementBits = Op.or(Op.INH, Op.SIM, Op.IMPL, Op.EQUI);

    public static final int opBits = Op.or(Op.ATOM, Op.INH, Op.PROD);
    public static final int EvalBits = opBits; //just an alias for code readabiliy

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

    public static final char IMAGE_PLACE_HOLDER = '_';
    public static final char SET_INT_CLOSER = ']';
    public static final char SET_EXT_CLOSER = '}';
    public static final char COMPOUND_TERM_OPENER = '(';
    public static final char COMPOUND_TERM_CLOSER = ')';

    @Deprecated
    public static final char STATEMENT_OPENER = '<';
    @Deprecated
    public static final char STATEMENT_CLOSER = '>';

    public static final char STAMP_OPENER = '{';
    public static final char STAMP_CLOSER = '}';
    public static final char STAMP_SEPARATOR = ';';
    public static final char STAMP_STARTER = ':';

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
    public static final int ImplicationOrEquivalenceBits = or(Op.EQUI, Op.IMPL);
    public static final int TemporalBits = or(Op.CONJ, Op.EQUI, Op.IMPL);
    public static final int VariableBits = or(Op.VAR_PATTERN, Op.VAR_INDEP, Op.VAR_DEP, Op.VAR_QUERY);
    public static final int[] NALLevelEqualAndAbove = new int[8 + 1]; //indexed from 0..7, meaning index 7 is NAL8, index 0 is NAL1

    final static Logger logger = LoggerFactory.getLogger(Op.class);
    public final boolean conceptualizable;

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
    private static Term compound(Op o, Term... subterms) {

        assert (!o.atomic);

        if (Param.DEBUG) {
            if (!o.allowsBool) {
                for (Term x : subterms)
                    if (x instanceof Bool)
                        return Null;
            }
        }

        int s = subterms.length;
        assert (o.maxSize >= s) : "subterm overflow: " + o + " " + Arrays.toString(subterms);
        assert (o.minSize <= s || (firstEllipsis(subterms) != null)) : "subterm underflow: " + o + " " + Arrays.toString(subterms);

        switch (s) {
            case 1:
                return new UnitCompound1(o, subterms[0]);

            default:
                return new GenericCompound(o, subterms(subterms));
        }
    }

    @NotNull
    public static TermContainer subterms(@NotNull Term... s) {
        return TermVector.the(s);
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

        this.atomic = var || str.equals(".") /* atom */ || str.equals("`i") || str.equals("^") || str.equals("`");

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
            case "<=>":
            case "&&":
                allowsBool = true;
                break;
            default:
                allowsBool = false;
                break;
        }
        conceptualizable = !(var || virtual || str.equals("+") /* INT */);
    }

    public static boolean hasAll(int existing, int possiblyIncluded) {
        return ((existing | possiblyIncluded) == existing);
    }

    public static boolean bool(@NotNull Term x) {
        return x instanceof Bool;
    }

    public static boolean isTrueOrFalse(@NotNull Term x) {
        return x == True || x == False;
    }

    //CaffeineMemoize.builder(buildTerm, -1 /* softref */, true /* Param.DEBUG*/);
    //new NullMemoize<>(buildTerm);

    public static boolean concurrent(int dt) {
        return (dt == DTERNAL) || (dt == 0);
    }

    @Nullable
    static public Term conjMerge(@NotNull Term a, long aStart, @NotNull Term b, long bStart) {

        List<ObjectLongPair<Term>> events = $.newArrayList();

        a.events(events, aStart);
        b.events(events, bStart);

        events.sort(Comparator.comparingLong(ObjectLongPair::getTwo));

        int ee = events.size();
        assert (ee > 1);

        //group all parallel clusters
        ObjectLongPair<Term> e0 = events.get(0);

        //Term head = events.get(0).getOne();
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
                    Term replacement = CONJ.the(0, p);
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
    public static Term conj(List<ObjectLongPair<Term>> events) {

        int ee = events.size();
        switch (ee) {
            case 0:
                return True;
            case 1:
                return events.get(0).getOne();
            default:
                return conj(events, 0, events.size() - 1);
        }
    }

    /**
     * constructs a correctly merged conjunction from a list of events, in the sublist specified by from..to (inclusive)
     */
    public static Term conj(List<ObjectLongPair<Term>> events, int from, int to) {
        int ee = to - from;
        switch (ee) {
            case 0:
                return events.get(from).getOne();
            case 1:
                Term left = events.get(from).getOne();
                Term right = events.get(to).getOne();
                return conjNonCommFinal(
                    /* dt */ (int) (events.get(to).getTwo() - events.get(from).getTwo()),
                        left, right);
        }

        int center = (from + to) / 2;
        int dt = (int) (events.get(center + 1).getTwo() - events.get(center).getTwo());


        Term left = conj(events, from, center);

        Term right = conj(events, center + 1, to);
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

        //System.out.println(left + " " + right + " " + left.compareTo(right));
        //return CONJ.the(dt, left, right);
        if (left.compareTo(right) > 0) {
            //larger on left
            dt = -dt;
            Term t = right;
            right = left;
            left = t;
        }
        return new GenericCompoundDT(new GenericCompound(CONJ,
                TermVector.the(left, right)
        ), dt);
    }

//    protected static Term conjMergeLeftAlign(List<ObjectLongPair<Term>> events, int from, int to) {
//        //Left aligned method:
//        Term head = e0.getOne();
//        long headAt = e0.getTwo();
//        for (int i = 1; i < ee; i++) {
//
//            ObjectLongPair<Term> ei = events.get(i);
//            Term next = ei.getOne();
//            long nextAt = ei.getTwo();
//
//            int dt = (int) (nextAt - headAt);
//            head = CONJ.the(dt, head, next);
//
//            headAt = nextAt;
//        }
//        return head;
//    }


    @NotNull
    private static Term newDiff(@NotNull Op op, @NotNull Term... t) {

        //corresponding set type for reduction:
        Op set = op == DIFFe ? SETe : SETi;

        switch (t.length) {
            case 1:
                Term t0 = t[0];
                return t0 instanceof Ellipsislike ?
                        new UnitCompound1(op, t0) :
                        Null;
            case 2:
                Term et0 = t[0], et1 = t[1];
                if (et0.equals(et1) || et0.containsRecursively(et1, nonProduct) || et1.containsRecursively(et0, nonProduct))
                    return Null;
                else if ((et0.op() == set && et1.op() == set))
                    return difference(set, (Compound) et0, (Compound) et1);
                else
                    return compound(op, DTERNAL, t);


        }

        throw new InvalidTermException(op, t, "diff requires 2 terms");

    }

    @NotNull
    public static Term difference(@NotNull Op o, @NotNull Compound a, @NotNull TermContainer b) {

        if (a.equals(b))
            return Null; //empty set

//        //quick test: intersect the mask: if nothing in common, then it's entirely the first term
//        if ((a.structure() & b.structure()) == 0) {
//            return a;
//        }

        int size = a.size();
        List<Term> terms = $.newArrayList(size);

        for (int i = 0; i < size; i++) {
            Term x = a.sub(i);
            if (!b.contains(x)) {
                terms.add(x);
            }
        }

        int retained = terms.size();
        if (retained == size) { //same as 'a'
            return a;
        } else if (retained == 0) {
            return Null; //empty set
        } else {
            return o.the(terms.toArray(new Term[retained]));
        }

    }

    /**
     * decode a term which may be a functor, return null if it isnt
     */
    @Nullable
    public static Pair<Atom, Compound> functor(@NotNull Term maybeOperation, TermContext index, boolean mustFunctor) {
        if (maybeOperation instanceof Compound && maybeOperation.hasAll(Op.opBits)) {
            Compound c = (Compound) maybeOperation;
            if (c.op() == INH) {
                Term s0 = c.sub(0);
                if (s0 instanceof Compound && s0.op() == PROD) {
                    Term s1 = c.sub(1);
                    if (s1 instanceof Atom /*&& s1.op() == ATOM*/) {
                        Termed ff = index.getIfPresentElse(s1);
                        if (!mustFunctor || ff instanceof Functor) {
                            return Tuples.pair(
                                    ((Atom) ff),
                                    ((Compound) s0)
                            );
                        }

                    }
                }
            }
        }
        return null;
    }


    @NotNull
    private static Term compound(Op op, int dt, Term... subterms) {

        //if (!subterms.internable())
        Term c = compound(op, subterms);
        if (dt != DTERNAL)
            return new GenericCompoundDT((Compound) c, dt);
        return c;

//        else
//            return compound(new NewCompound(op, subterms), dt);
    }


    static boolean in(int needle, int haystack) {
        return (needle & haystack) == needle;
    }

    public static int or(@NotNull Op... o) {
        int bits = 0;
        for (Op n : o)
            bits |= n.bit;
        return bits;
    }

    public static final Predicate<Compound> nonProduct = c -> c.op() != PROD;
    private static final int InvalidEquivalenceTerm = or(IMPL, EQUI);
    private static final int InvalidImplicationSubj = or(EQUI, IMPL);
    private static final int InvalidImplicationPred = or(EQUI);

    @NotNull
    static Term statement(@NotNull Op op, int dt, @NotNull Term subject, @NotNull Term predicate) {

        if (subject == Null || predicate == Null)
            return Null;


        boolean polarity = true;

        switch (op) {

            case SIM:

                if (subject.equals(predicate))
                    return True;
                if (bool(subject) || bool(predicate))
                    return False;
                break;

            case INH:

                if (subject.equals(predicate)) //equal test first to allow, ex: False<->False to result in True
                    return True;
                if (isTrueOrFalse(subject) || isTrueOrFalse(predicate))
                    return False;


                boolean sNeg = subject.op() == NEG;
                boolean pNeg = predicate.op() == NEG;
                if (sNeg && pNeg) {
                    subject = subject.unneg();
                    predicate = predicate.unneg();
                } else if (sNeg/* && !pNeg*/) {
                    subject = subject.unneg();
                    polarity = !polarity;
                } else if (pNeg/* && !sNeg*/) {
                    predicate = predicate.unneg();
                    polarity = !polarity;
                }

                break;


            case EQUI:

                if (concurrent(dt)) {
                    if (subject == True) return predicate;
                    if (subject == False) return NEG.the(predicate);
                    if (predicate == True) return subject;
                    if (predicate == False) return NEG.the(subject);
                } else {
                    if (subject == True || subject == False || predicate == True || predicate == False)
                        return Null; //no temporal basis
                }


                boolean sn = subject.op() == NEG;
                boolean pn = predicate.op() == NEG;
                if (sn && pn) {
                    subject = subject.unneg();
                    predicate = predicate.unneg();
                } else if (sn && !pn) {
                    subject = subject.unneg();
                    polarity = !polarity;
                } else if (!sn && pn) {
                    predicate = predicate.unneg();
                    polarity = !polarity;
                }

                if (concurrent(dt) && subject.equals(predicate))
                    return polarity ? True : False;
                //else not concurrent and equivalent, allow


                //return !t.opUnneg().in(InvalidEquivalenceTerm);
                //        if ( instanceof Implication) || (subject instanceof Equivalence)
//                || (predicate instanceof Implication) || (predicate instanceof Equivalence) ||
//                (subject instanceof CyclesInterval) || (predicate instanceof CyclesInterval)) {
//            return null;
//        }
                if (subject.hasAny(InvalidEquivalenceTerm))
                    //throw new InvalidTermException(op, dt, "Invalid equivalence subject", subject, predicate);
                    return Null;

                if (predicate.hasAny(InvalidEquivalenceTerm))
                    //throw new InvalidTermException(op, dt, "Invalid equivalence predicate", subject, predicate);
                    return Null;

//                boolean subjNeg = subject.op() == NEG;
//                boolean predNeg = predicate.op() == NEG;
//                if (subjNeg && predNeg) {
//                    subject = subject.unneg();
//                    predicate = predicate.unneg();
//                } else if (!subjNeg && predNeg) {
//                    //factor out (--, ...)
//                    return NEG.the(op.the(dt, subject, predicate.unneg()));
//                } else if (subjNeg && !predNeg) {
//                    //factor out (--, ...)
//                    return NEG.the(op.the(dt, subject.unneg(), predicate));
//                }
                if (dt == XTERNAL) {
                    return compound(op, XTERNAL, //op != EQUI || subject.compareTo(predicate) <= 0 ?
                            subject, predicate);
                    //subterms(predicate,subject));
                }

                boolean equal = subject.equals(predicate);
                if (!concurrent(dt)) {
//                    if (!equal && subject.compareTo(predicate) > 0) {
//                        //swap
//                        Term x = subject;
//                        subject = predicate;
//                        predicate = x;
//                        dt = -dt;
//                    }

                    if (equal && dt < 0) {
                        dt = -dt; //use only the forward direction on a repeat
                    }
                }

                break;

            case IMPL:

                //special case for implications: reduce to --predicate if the subject is False
                if (isTrueOrFalse(subject /* antecedent */)) {
                    if (concurrent(dt))
                        return $.negIf(predicate, polarity ? (subject == False) : (subject != False));
                    else {
                        return Null; //no temporal basis
                    }
                }
                if (bool(predicate /* absolute truth is immutable so it can not be the consequence of anything mutable */))
                    return Null;

                if (subject.hasAny(InvalidImplicationSubj))
                    return Null; //throw new InvalidTermException(op, dt, "Invalid equivalence subject", subject, predicate);
                if (predicate.hasAny(InvalidImplicationPred))
                    return Null; //throw new InvalidTermException(op, dt, "Invalid equivalence predicate", subject, predicate);


                if (predicate.op() == NEG) {
                    //negated predicate gets unwrapped to outside
                    predicate = predicate.unneg();
                    polarity = !polarity;
                }


                if (concurrent(dt)) {
                    if (subject.equals(predicate))
                        return polarity ? True : False;
                } //else: allow repeat


                // (C ==>+- (A ==>+- B))   <<==>>  ((C &&+- A) ==>+- B)
                if (predicate.op() == IMPL) {
                    Compound cpr = (Compound) predicate;
                    int cprDT = cpr.dt();
                    //if (cprDT != XTERNAL) {
                    Term a = cpr.sub(0);

                    subject = CONJ.the(dt, subject, a);
                    predicate = cpr.sub(1);
                    return $.negIf(IMPL.the(cprDT, subject, predicate), !polarity);
                    //}
                }


                break;
        }


        //factor out any common subterms iff concurrent
        if (concurrent(dt)) {

            Term pu = predicate.unneg();
            Term su = subject.unneg();
            //first layer only, not recursively
            if ((pu.varPattern() == 0 && (subject.equals(pu) || subject.containsRecursively(pu, nonProduct))) ||
                    (su.varPattern() == 0 && (predicate.equals(su) || predicate.containsRecursively(su, nonProduct))))
                //(!(su instanceof Variable) && predicate.contains(su)))
                return False; //cyclic

//            if (subject.varPattern() == 0 && predicate.varPattern() == 0 &&
//                    !(subject instanceof Variable) && !(predicate instanceof Variable) &&
//                    (subject.containsRecursively(predicate) || predicate.containsRecursively(subject))) //first layer only, not recursively
//                return False; //cyclic

            if ((op == IMPL || op == EQUI)) { //TODO verify this works as it should


                boolean subjConj = subject.op() == CONJ && concurrent(subject.dt());
                boolean predConj = predicate.op() == CONJ && concurrent(predicate.dt());
                if (subjConj && predConj) {
                    final Compound csub = (Compound) subject;
                    TermContainer subjs = csub.subterms();
                    final Compound cpred = (Compound) predicate;
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


                        return $.negIf(op.the(dt, subject, predicate), !polarity);
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

        if (op.commutative) {

//            //normalize co-negation
//            boolean sn = subject.op() == NEG;
//            boolean pn = predicate.op() == NEG;
//
            if (/*(sn == pn) && */(subject.compareTo(predicate) > 0)) {
                Term x = predicate;
                predicate = subject;
                subject = x;
                if (dt != XTERNAL && !concurrent(dt))
                    dt = -dt;
            }

            //assert (subject.compareTo(predicate) <= 0);
            //System.out.println( "\t" + subject + " " + predicate + " " + subject.compareTo(predicate) + " " + predicate.compareTo(subject));

        }


        Term x = compound(op, dt, subject, predicate); //use the calculated ordering, not the TermContainer default for commutives
        return $.negIf(x, !polarity);
    }

    public static Op fromString(String s) {
        return stringToOperator.get(s);
    }

    @NotNull
    private static Term intersect(@NotNull Term[] t, @NotNull Op intersection, @NotNull Op setUnion, @NotNull Op setIntersection) {

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
    private static Term intersect2(@NotNull Term term1, @NotNull Term term2, @NotNull Op intersection, @NotNull Op setUnion, @NotNull Op setIntersection) {

        if (term1.equals(term2))
            return term1;

        Op o1 = term1.op();
        Op o2 = term2.op();

        if ((o1 == setUnion) && (o2 == setUnion)) {
            //the set type that is united
            return Terms.union(setUnion, (Compound) term1, (Compound) term2);
        }


        if ((o1 == setIntersection) && (o2 == setIntersection)) {
            //the set type which is intersected
            return Terms.intersect(setIntersection, (Compound) term1, (Compound) term2);
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
            ((TermContainer) term1).forEach(args::add);
            if (o2 == intersection)
                ((TermContainer) term2).forEach(args::add);
            else
                args.add(term2);
        } else {
            args.add(term1);
            args.add(term2);
        }

        return compound(intersection, DTERNAL, Terms.sorted(args));
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
                case EQUI:
                    s = ("<|>");
                    break;
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

    public boolean commute(int dt) {
        if (commutative) {
            if (temporal) {
                /*if (dt == XTERNAL) {

                }*/
                if (!Op.concurrent(dt))
                    return false;
            }
            return true;
        }
        return false;
    }

    @NotNull
    public Term the(Term... u) {
        return the(DTERNAL, u);
    }

    public Term the(int dt, @NotNull Collection<Term> sub) {
        int ss = sub.size();
        @NotNull Term[] u = sub.toArray(new Term[ss]);
        return the(dt, u);
    }

    @NotNull
    public Term the(int dt, @NotNull Term... u) {
        if (statement) {
            return statement(this, dt, u[0], u[1]);
        } else {
            Term[] uu = commute(dt) ? Terms.sorted(u) : u;
            return compound(this, dt, uu);
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

        @Override
        public @NotNull Term unneg() {
            return this;
        }
    }

    private static class BoolFalse extends Bool {
        public BoolFalse() {
            super(String.valueOf(Op.FalseSym));
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

        @NotNull
        @Override
        public Term unneg() {
            return False;
        }
    }
}
