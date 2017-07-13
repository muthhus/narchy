package nars;


import jcog.bag.impl.hijack.HijackMemoize;
import jcog.util.Memoize;
import nars.derive.meta.match.Ellipsislike;
import nars.index.term.AppendProtoCompound;
import nars.index.term.ProtoCompound;
import nars.index.term.TermContext;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.AtomicSingleton;
import nars.term.compound.GenericCompound;
import nars.term.compound.UnitCompound1;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.util.InvalidTermException;
import nars.term.var.UnnormalizedVariable;
import nars.time.Tense;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.api.tuple.primitive.ObjectBytePair;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.copyOfRange;
import static nars.term.Terms.flatten;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * NAL symbol table
 */
public enum Op {


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
            } else if (x instanceof AtomicSingleton) {
                if (isFalse(x)) return True;
                if (isTrue(x)) return False;
                else return Null;
            }

            return new UnitCompound1(NEG, x);

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
     */
    PROD("*", 4, Args.GTEZero),


    /**
     * conjunction
     */
    CONJ("&&", true, 5, Args.GTETwo) {
        @Override
        public Term the(int dt, Term... uu) {
            Term[] u = uu.length > 1 ? conjTrueFalseFilter(uu) : uu /* avoid true false filter if fall-through only-term anyway */;

            final int n = u.length;
            if (n == 1) {
                Term only = u[0];

                //preserve unitary ellipsis for patterns etc
                return only instanceof Ellipsislike ?
                        new GenericCompound(CONJ, subterms(only)) //special
                        :
                        only;

            }

            if (n == 0)
                return Null;


            if (dt == XTERNAL) {
                //assert (n == 2); //throw new InvalidTermException(CONJ, XTERNAL, "XTERNAL only applies to 2 subterms, as dt placeholder", u);

                //Arrays.sort(u); //sort but dont deduplicate
                return conjPost(compound(CONJ, XTERNAL, subterms(u)));
            }

            boolean commutive = concurrent(dt);
            if (commutive) {

                return conjPost(junctionFlat(dt, u));

            } else {
                //NON-COMMUTIVE

                //assert (n == 2);
                if (n != 2) {
                    throw new InvalidTermException(CONJ, u, "invalid non-commutive conjunction");
                }

                Term a = u[0];
                Term b = u[1];
                boolean changed = false;
                if (a.equals(b)) {
                    if (dt < 0) {
                        //make dt positive to avoid creating both (x &&+1 x) and (x &&-1 x)
                        dt = -dt;
                    }
                } else {
                    if (a.compareTo(b) > 0) {
                        //ensure lexicographic ordering

                        Term x = u[0];
                        u[0] = u[1];
                        u[1] = x; //swap
                        dt = -dt; //and invert time
                        changed = true;
                    }
                }

                return conjPost(
                        !changed ?
                                compound(CONJ, dt, subterms(u)) :
                                CONJ.the(dt, u)
                );

            }
        }

        /**
         * flattening conjunction builder, for (commutive) multi-arg conjunction and disjunction (dt == 0 ar DTERNAL)
         * see: https://en.wikipedia.org/wiki/Boolean_algebra#Monotone_laws
         */
        @NotNull
        private Term junctionFlat(int dt, @NotNull final Term... u) {

            //TODO if there are no negations in u then an accelerated construction is possible

            assert (u.length > 0 && (dt == 0 || dt == DTERNAL || dt == XTERNAL)); //throw new RuntimeException("should only have been called with dt==0 or dt==DTERNAL");

            ObjectByteHashMap<Term> s = new ObjectByteHashMap<>(u.length * 2);

            if (flatten(CONJ, u, dt, s) && !s.isEmpty()) {
                TreeSet<Term> cs = junctionGroupNonDTSubterms(s);
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
                            Set<Term> disjSubs = disj.toSet();
                            //factor out occurrences of the disj's contents outside the disjunction, so remove from inside it
                            if (disjSubs.removeAll(cs)) {
                                //reconstruct disj if changed
                                csi.remove();

                                if (!disjSubs.isEmpty()) {
                                    Term y = NEG.the(CONJ.the(disj.dt(), disjSubs.toArray(new Term[disjSubs.size()])));
                                    if (csa == null)
                                        csa = $.newArrayList(1);
                                    csa.add(y);
                                }
                            }
                        }
                    }
                    if (csa != null)
                        cs.addAll(csa);

                    Term[] scs = Terms.sorted(cs);
                    if (scs.length == 1)
                        return scs[0];

                    if (!Arrays.equals(scs, u))
                        return CONJ.the(dt, scs);
                    else
                        return compound(CONJ, dt, subterms(scs));
                }
            }

            return False;
        }

        /**
         * array implementation of the conjunction true/false filter
         */
        @NotNull
        private Term[] conjTrueFalseFilter(@NotNull Term... u) {
            int trues = 0; //# of True subterms that can be eliminated
            for (Term x : u) {
                if (isTrue(x)) {
                    trues++;
                } else if (isFalse(x)) {

                    //false subterm in conjunction makes the entire condition false
                    //this will eventually reduce diectly to false in this method's only callee HACK
                    return FalseArray;
                }
            }

            if (trues == 0)
                return u;

            int ul = u.length;
            if (ul == trues)
                return TrueArray; //reduces to an Imdex itself

            Term[] y = new Term[ul - trues];
            int j = 0;
            for (int i = 0; j < y.length; i++) {
                Term uu = u[i];
                if (!isTrue(uu)) // && (!uu.equals(False)))
                    y[j++] = uu;
            }

            assert (j == y.length);

            return y;
        }

        /**
         * this is necessary to keep term structure consistent for intermpolation.
         * by grouping all non-sequence subterms into its own subterm, future
         * flattening and intermpolation is prevented from destroying temporal
         * measurements.
         *
         */
        private @NotNull TreeSet<Term> junctionGroupNonDTSubterms(@NotNull ObjectByteHashMap<Term> s) {

            TreeSet<Term> outer = new TreeSet();

            for (ObjectBytePair<Term> xn : s.keyValuesView()) {
                Term x = xn.getOne();
                outer.add((xn.getTwo() < 0) ? NEG.the(x) : x);
            }
            return outer;


        }


        private Term conjPost(final Term x /* possibly a conjunction */) {

            if (x == null)
                return null;

            Op xo = x.op();
            if (xo != CONJ)
                return x;


            //conjunction/implication reduction:
            if (x.hasAny(IMPL)) {
                //if there is only one implication subterm, then fold into that.
                //if there are more than one, don't do anything (for now)
                Compound cx = (Compound) x;
                Compound c = cx;
                int whichImpl = -1;
                int cs = c.size();
                for (int i = 0; i < cs; i++) {
                    if (c.subIs(i, Op.IMPL)) {
                        if (whichImpl != -1) {
                            //a 2nd implication was found; don't continue
                            whichImpl = -1;
                            break;
                        }
                        whichImpl = i;
                    }
                }

                if (whichImpl != -1) {

//                Term[] precond = c.subterms().terms(
//                        (IntObjectPredicate<Term>)((i,s)->(i != ww)));
                    Compound css = (Compound) c.sub(whichImpl);
                    Term implPre = css.sub(0);
                    Term implPost = css.sub(1);
                    Term other;
                    if (x.size() == 2) {
                        other = x.sub(1 - whichImpl, null);
                    } else {
                        //more than 2
                        @NotNull Set<Term> ss = cx.toSet();
                        if (ss.remove(css))
                            other = x.op().the(cx.dt(), Terms.sorted(ss) /* assumes commutive since > 2 */);
                        else {
                            throw new RuntimeException("shouldnt happen");
                        }
                    }
                    Term newPre = CONJ.the(
                            cx.dt(),
                            other, implPre);
                    return IMPL.the(css.dt(), newPre, implPost);

                }

            }

            return x;
        }

    },

    //SPACE("+", true, 7, Args.GTEOne),


    /**
     * intensional set
     */
    SETi("[", true, 2, Args.GTEOne) {
        @Override public boolean isSet() {
            return true;
        }
    },

    /**
     * extensional set
     */
    SETe("{", true, 2, Args.GTEOne) {
        @Override public boolean isSet() {
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


    VAR_PATTERN('%', Op.ANY_LEVEL, OpType.Variable),


    //VIRTUAL TERMS
    @Deprecated
    INSTANCE("-{-", 2, OpType.Statement),

    @Deprecated
    PROPERTY("-]-", 2, OpType.Statement),

    @Deprecated
    INSTANCE_PROPERTY("{-]", 2, OpType.Statement),

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

    public static final int StatementBits = Op.or(Op.INH, Op.SIM, Op.IMPL, Op.EQUI);

    public static final int OpBits = Op.or(Op.ATOM, Op.INH, Op.PROD);
    public static final int EvalBits = OpBits; //just an alias for code readabiliy

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
    public static final char FalseSym = 'Ø';
    public static final char NullSym = (char) 133 /* horizontal ellipsis */;

    /**
     * absolutely invalid
     */
    public static final AtomicSingleton Null = new AtomicSingleton(String.valueOf(NullSym));

    /**
     * absolutely true
     */
    public static final AtomicSingleton True = new AtomicSingleton(String.valueOf(TrueSym)) {
        @NotNull
        @Override
        public Term unneg() {
            return False;
        }
    };

    /**
     * absolutely false
     */
    public static final AtomicSingleton False = new AtomicSingleton(String.valueOf(FalseSym)) {
        @NotNull
        @Override
        public Term unneg() {
            return True;
        }
    };

    public static final Term[] TrueArray = {True};
    public static final Term[] FalseArray = {False};


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
     * -1 for unlimited
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

    Op(char c, int minLevel, OpType type) {
        this(c, minLevel, type, Args.None);
    }

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

    }


    public static boolean hasAll(int existing, int possiblyIncluded) {
        return ((existing | possiblyIncluded) == existing);
    }

    public static boolean isAbsolute(@NotNull Term x) {
        return x instanceof AtomicSingleton;
    }

    public static boolean isTrueOrFalse(@NotNull Term x) {
        return isTrue(x) || isFalse(x);
    }

    public static boolean isTrue(@NotNull Term x) {
        return x == True;
    }

    public static boolean isFalse(@NotNull Term x) {
        return x == False;
    }

    public static boolean concurrent(int dt) {
        return (dt == DTERNAL) || (dt == 0);
    }

    @NotNull
    private static Term newDiff(@NotNull Op op, @NotNull Term... t) {

        //corresponding set type for reduction:
        Op set = op == DIFFe ? SETe : SETi;

        switch (t.length) {
            case 1:
                Term t0 = t[0];
                return t0 instanceof Ellipsislike ?
                        compound(op, DTERNAL, subterms(t0)) :
                        Null;
            case 2:
                Term et0 = t[0], et1 = t[1];
                if (et0.equals(et1) || et0.containsRecursively(et1) || et1.containsRecursively(et0))
                    return Null;
                else if ((et0.op() == set && et1.op() == set))
                    return difference(set, (Compound) et0, (Compound) et1);
                else
                    return compound(op, DTERNAL, subterms(t));


        }

        throw new InvalidTermException(op, t, "diff requires 2 terms");

    }

    @NotNull
    public static Term difference(@NotNull Op o, @NotNull Compound a, @NotNull TermContainer b) {

        if (a.equals(b))
            return Null; //empty set

        //quick test: intersect the mask: if nothing in common, then it's entirely the first term
        if ((a.structure() & b.structure()) == 0) {
            return a;
        }

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
    public static Pair<Atomic, Compound> functor(@NotNull Term maybeOperation, TermContext index, boolean mustFunctor) {
        if (maybeOperation instanceof Compound && maybeOperation.hasAll(Op.OpBits)) {
            Compound c = (Compound) maybeOperation;
            if (c.op() == INH) {
                Term s0 = c.sub(0);
                if (s0 instanceof Compound && s0.op() == PROD) {
                    Term s1 = c.sub(1);
                    if (s1 instanceof Atom /*&& s1.op() == ATOM*/) {
                        Atom ff = (Atom) index.getIfPresentElse(s1);
                        if (!mustFunctor || ff instanceof Functor) {
                            return Tuples.pair(
                                    ff,
                                    ((Compound) s0)
                            );
                        }

                    }
                }
            }
        }
        return null;
    }

    @Nullable
    public static Pair<Atomic, Compound> functor(Op cOp, @NotNull Term[] subs, TermContext index, boolean mustFunctor) {
        if (cOp == INH) {
            Term s0 = subs[0];
            if (s0.op() == PROD) {
                Term s1 = subs[1];
                if (s1.op() == ATOM) {
                    Atomic ff = (Atomic) index.getIfPresentElse(s1);
                    if (!mustFunctor || ff instanceof Functor) {
                        return Tuples.pair(
                                ff,
                                ((Compound) s0)
                        );

                    }
                }
            }

        }
        return null;
    }

    /*
    used only by Termlike.hasAny
    public static boolean hasAny(int existing, int possiblyIncluded) {
        return (existing & possiblyIncluded) != 0;
    }*/


    @NotNull
    @Override
    public String toString() {
        return str;
    }


    /**
     * writes this operator to a Writer in (human-readable) expanded UTF16 mode
     */
    public final void append(@NotNull Compound c, @NotNull Appendable w) throws IOException {
        append(c, w, false);
    }

    /**
     * writes this operator to a Writer in (human-readable) expanded UTF16 mode
     */
    public final void append(@NotNull Compound c, @NotNull Appendable w, boolean invertDT) throws IOException {
        int t = c.dt();
        boolean hasTime = t != Tense.DTERNAL;

        if (hasTime)
            w.append(' ');

        char ch = this.ch;
        if (ch == 0)
            w.append(str);
        else
            w.append(ch);

        if (hasTime) {

            if (invertDT)
                t = -t;

            if (t >= 0) w.append('+');
            String ts;
            if (t == XTERNAL)
                ts = "-";
            else
                ts = Integer.toString(t);
            w.append(ts).append(' ');
        }
    }

    public boolean commute(int dt, Term[] u) {
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
    public final Term the(Term... u) {
        return the(DTERNAL, u);
    }


    public Term the(int dt, @NotNull Term... u) {
        if (statement) {
            return statement(this, dt, u[0], u[1]);
        } else {
            return compound(this, dt, subterms(commute(dt, u) ? Terms.sorted(u) : u));
        }
    }

    final static Logger logger = LoggerFactory.getLogger(Op.class);

    static final Function<ProtoCompound, Termlike> buildTerm = (C) -> {
        try {

            Op o = C.op();
            if (o != null) {
                return _compound(o, C.subterms());
            } else
                return _subterms(C.subterms());

        } catch (InvalidTermException e) {
            if (Param.DEBUG_EXTRA)
                logger.error("Term Build: {}, {}", C, e);
            return Null;
        } catch (Throwable t) {
            logger.error("{}", t);
            return Null;
        }
    };

    private static Term _compound(Op o, Term[] subterms) {
        int s = subterms.length;
        assert (s > 0);
        switch (s) {
            case 1:
                return new UnitCompound1(o, subterms[0]);

            default:
                return new GenericCompound(o, subterms(subterms));
        }
    }

    public static final Memoize<ProtoCompound, Termlike> cache =
            new HijackMemoize<>(buildTerm, 256 * 1024 + 1, 4);
    //CaffeineMemoize.build(buildTerm, 128 * 1024, true /* Param.DEBUG*/);


    static TermContainer _subterms(@NotNull Term[] s) {
        return TermVector.the(s);
    }


    static public @NotNull TermContainer subterms(@NotNull Term... s) {
//        if (s.length < 2) {
//            return _subterms(s);
//        } else {
            return (TermContainer) cache.apply(new AppendProtoCompound(null, s).commit());
//        }
    }



    @NotNull public static Term compound(Op op, int dt, TermContainer subterms) {
        assert (!op.atomic);
        AppendProtoCompound apc = new AppendProtoCompound(op, subterms);
        return compound(apc, dt);
    }

    @NotNull public static Term compound(AppendProtoCompound apc, int dt) {
        Term x = (Term) cache.apply(apc.commit());
        if (dt!=DTERNAL && x instanceof Compound)
            return x.dt(dt);
        else
            return x;
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


    static boolean in(int needle, int haystack) {
        return (needle & haystack) == needle;
    }

    public static int or(@NotNull Op... o) {
        int bits = 0;
        for (Op n : o)
            bits |= n.bit;
        return bits;
    }

    /**
     * specifier for any NAL level
     */
    public static final int ANY_LEVEL = 0;




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

        static final IntIntPair GTEZero = pair(0, -1);
        static final IntIntPair GTEOne = pair(1, -1);
        static final IntIntPair GTETwo = pair(2, -1);

    }


    public static final int SetBits = or(Op.SETe, Op.SETi);
    public static final int ImplicationOrEquivalenceBits = or(Op.EQUI, Op.IMPL);
    public static final int TemporalBits = or(Op.CONJ, Op.EQUI, Op.IMPL);
    public static final int VariableBits = or(Op.VAR_PATTERN, Op.VAR_INDEP, Op.VAR_DEP, Op.VAR_QUERY);


    public static final int[] NALLevelEqualAndAbove = new int[8 + 1]; //indexed from 0..7, meaning index 7 is NAL8, index 0 is NAL1

    private static final int InvalidEquivalenceTerm = or(IMPL, EQUI);
    private static final int InvalidImplicationSubj = or(EQUI, IMPL);
    private static final int InvalidImplicationPred = or(EQUI);

    static boolean validEquivalenceTerm(@NotNull Term t) {
        //return !t.opUnneg().in(InvalidEquivalenceTerm);
        return !t.hasAny(InvalidEquivalenceTerm);
//        if ( instanceof Implication) || (subject instanceof Equivalence)
//                || (predicate instanceof Implication) || (predicate instanceof Equivalence) ||
//                (subject instanceof CyclesInterval) || (predicate instanceof CyclesInterval)) {
//            return null;
//        }
    }

    @NotNull
    static Term statement(@NotNull Op op, int dt, @NotNull Term subject, @NotNull Term predicate) {

        if (subject == Null || predicate == Null)
            return Null;

        if (dt == XTERNAL) {
            return compound(op, XTERNAL, //subterms(op != EQUI || subject.compareTo(predicate) <= 0 ?
                    subterms(subject, predicate));
                    //new Term[]{predicate, subject}));
        }

        switch (op) {

            case SIM:

                if (subject.equals(predicate))
                    return True;
                if (isAbsolute(subject) || isAbsolute(predicate))
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
                } else if (sNeg && !pNeg) {
                    return NEG.the(statement(op, dt, subject.unneg(), predicate)); //TODO loop and not recurse, needs negation flag to be applied at the end before returning
                } else if (pNeg && !sNeg) {
                    return NEG.the(statement(op, dt, subject, predicate.unneg()));
                }

                break;


            case EQUI:

                //if (isTrue(subject)) return predicate;
                //if (isTrue(predicate)) return subject;
                //if (isFalse(subject)) return neg(predicate);
                //if (isFalse(predicate)) return neg(subject);
                if (concurrent(dt) && subject.equals(predicate))
                    return True;
                if (isTrue(subject) || isFalse(subject))
                    return False; //otherwise they are absolutely inequal

                if (!validEquivalenceTerm(subject))
                    throw new InvalidTermException(op, dt, "Invalid equivalence subject", subject, predicate);
                if (!validEquivalenceTerm(predicate))
                    throw new InvalidTermException(op, dt, "Invalid equivalence predicate", subject, predicate);

                boolean subjNeg = subject.op() == NEG;
                boolean predNeg = predicate.op() == NEG;
                if (subjNeg && predNeg) {
                    subject = subject.unneg();
                    predicate = predicate.unneg();
                } else if (!subjNeg && predNeg) {
                    //factor out (--, ...)
                    return NEG.the(op.the(dt, subject, predicate.unneg()));
                } else if (subjNeg && !predNeg) {
                    //factor out (--, ...)
                    return NEG.the(op.the(dt, subject.unneg(), predicate));
                }

                boolean equal = subject.equals(predicate);
                if (concurrent(dt)) {
                    if (equal) {
                        return True;
                    }
                } else {
                    if (subject.compareTo(predicate) > 0) {
                        //swap
                        Term x = subject;
                        subject = predicate;
                        predicate = x;
                        dt = -dt;
                    }

                    if (equal && dt < 0) {
                        dt = -dt; //use only the forward direction on a repeat
                    }
                }

                break;

            case IMPL:

                //special case for implications: reduce to --predicate if the subject is False
                if (isTrueOrFalse(subject /* antecedent */)) {
                    if (concurrent(dt))
                        return $.negIf(predicate, isFalse(subject));
                    else {
                        return Null; //no temporal basis
                    }
                }
                if (isAbsolute(predicate /* consequence */))
                    return Null;
                if (subject.hasAny(InvalidImplicationSubj))
                    return Null; //throw new InvalidTermException(op, dt, "Invalid equivalence subject", subject, predicate);
                if (predicate.hasAny(InvalidImplicationPred))
                    return Null; //throw new InvalidTermException(op, dt, "Invalid equivalence predicate", subject, predicate);


                if (predicate.op() == NEG) {
                    //negated predicate gets unwrapped to outside
                    return NEG.the(op.the(dt, subject, predicate.unneg()));
                }


                if (concurrent(dt)) {
                    if (subject.equals(predicate))
                        return True;
                } //else: allow repeat


                // (C ==>+- (A ==>+- B))   <<==>>  ((C &&+- A) ==>+- B)
                if (predicate.op() == IMPL) {
                    Compound cpr = (Compound) predicate;
                    int cprDT = cpr.dt();
                    if (cprDT != XTERNAL) {
                        Term a = cpr.sub(0);

                        subject = CONJ.the(dt, subject, a);
                        predicate = cpr.sub(1);
                        return IMPL.the(cprDT, subject, predicate);
                    }
                }


                break;
        }


        //factor out any common subterms iff concurrent
        if (concurrent(dt)) {

            Term pu = predicate.unneg();
            Term su = subject.unneg();
            //first layer only, not recursively
            if ((pu.varPattern() == 0 && (subject.equals(pu) || subject.containsRecursively(pu))) ||
                    (su.varPattern() == 0 && (predicate.equals(su) || predicate.containsRecursively(su))))
                //(!(su instanceof Variable) && predicate.contains(su)))
                return False; //cyclic

//            if (subject.varPattern() == 0 && predicate.varPattern() == 0 &&
//                    !(subject instanceof Variable) && !(predicate instanceof Variable) &&
//                    (subject.containsRecursively(predicate) || predicate.containsRecursively(subject))) //first layer only, not recursively
//                return False; //cyclic

            if ((op == IMPL || op == EQUI)) { //TODO verify this works as it should


                boolean subjConj = subject.op() == CONJ && concurrent(((Compound) subject).dt());
                boolean predConj = predicate.op() == CONJ && concurrent(((Compound) predicate).dt());
                if (subjConj && !predConj) {
                    final Compound csub = (Compound) subject;
                    //TermContainer subjs = csub.subterms();
                    if (csub.containsRecursively(predicate)) {
                        return False;
//                        Term finalPredicate = predicate;
//                        subject = the(CONJ, csub.dt(), subjs.asFiltered(z -> z.equals(finalPredicate)).toArray());
//                        predicate = False;
//                        return statement(op, dt, subject, predicate);
                    }
                } else if (predConj && !subjConj) {
                    final Compound cpred = (Compound) predicate;
                    //TermContainer preds = cpred.subterms();
                    if (cpred.containsRecursively(subject)) {
                        return False;
//                        Term finalSubject = subject;
//                        predicate = the(CONJ, cpred.dt(), preds.asFiltered(z -> z.equals(finalSubject)).toArray());
//                        subject = False;
//                        return statement(op, dt, subject, predicate);
                    }

                } else if (subjConj && predConj) {
                    final Compound csub = (Compound) subject;
                    TermContainer subjs = csub.subterms();
                    final Compound cpred = (Compound) predicate;
                    TermContainer preds = cpred.subterms();

                    Term[] common = TermContainer.intersect(subjs, preds);
                    if (common != null) {

                        @NotNull Set<Term> sss = subjs.toSet();
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

                        @NotNull Set<Term> ppp = preds.toSet();
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
                                    if (predicate == null)
                                        return Null;
                                    break;
                            }
                        }

                        return op.the(dt, subject, predicate);
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

            //normalize co-negation
            boolean sn = subject.op() == NEG;
            boolean pn = predicate.op() == NEG;

            if ((sn == pn) && (subject.compareTo(predicate) > 0)) {
                Term x = predicate;
                predicate = subject;
                subject = x;
                if (!concurrent(dt))
                    dt = -dt;
            }

            //System.out.println( "\t" + subject + " " + predicate + " " + subject.compareTo(predicate) + " " + predicate.compareTo(subject));

        }



        return compound(op, dt, subterms(subject, predicate)); //use the calculated ordering, not the TermContainer default for commutives
    }


    /**
     * index of operators which are encoded by 1 byte: must be less than 31 because this is the range for control characters
     */
    static final int numByteSymbols = 15;
    static final Op[] byteSymbols = new Op[numByteSymbols];
    static final ImmutableMap<String, Op> stringToOperator;
    //static final CharObjectHashMap<Op> _charToOperator = new CharObjectHashMap(values().length * 2);

    public static Op fromString(String s) {
        return stringToOperator.get(s);
    }

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
            int ordinal = r.ordinal();
            if (ordinal < 15)
                Op.byteSymbols[ordinal] = r;
        }
        stringToOperator = Maps.immutable.ofMap(_stringToOperator);

        //System.out.println(Arrays.toString(byteSymbols));

        //VERIFICATION: Look for any empty holes in the byteSymbols table, indicating that the representation is not contigous
        //index 0 is always 0 to maintain \0's semantics
        //if # of operators are reduced in the future, then this will report that the table size should be reduced (avoiding unnecessary array lookups)
        for (int i = 1; i < Op.byteSymbols.length; i++) {
            if (null == Op.byteSymbols[i])
                throw new RuntimeException("Invalid byteSymbols encoding: index " + i + " is null");
        }

//        //Setup NativeOperator Character index hashtable
//        for (Op r : Op.values()) {
//            char c = r.ch;
//            if (c!=0)
//                Op._charToOperator.put(c, r);
//        }
    }


    @NotNull
    private static Term intersect(@NotNull Term[] t, @NotNull Op intersection, @NotNull Op setUnion, @NotNull Op setIntersection) {

        int trues = 0;
        for (Term x : t) {
            if (isTrue(x)) {
                //everything intersects with the "all", so remove this TRUE below
                trues++;
            } else if (isFalse(x)) {
                return Null;
            }
        }
        if (trues > 0) {
            if (trues == t.length) {
                return True; //all were true
            } else if (t.length - trues == 1) {
                //find the element which is not true and return it
                for (Term x : t) {
                    if (!isTrue(x))
                        return x;
                }
            } else {
                //filter the True statements from t
                Term[] t2 = new Term[t.length - trues];
                int yy = 0;
                for (Term x : t) {
                    if (!isTrue(x))
                        t2[yy++] = x;
                }
                t = t2;
            }
        }

        switch (t.length) {

            case 1:

                Term single = t[0];
                return single instanceof Ellipsislike ?
                        new GenericCompound(intersection, subterms(single)) :
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

        return compound(intersection, DTERNAL, subterms(Terms.sorted(args)));
    }


    public static class InvalidPunctuationException extends RuntimeException {
        public InvalidPunctuationException(byte c) {
            super("Invalid punctuation: " + c);
        }
    }

}
