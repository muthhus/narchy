package nars;


import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.time.Tense;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static nars.time.Tense.XTERNAL;
import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * NAL symbol table
 */
public enum Op {

    /**
     * an atomic term (includes interval and variables); this value is set if not a compound term
     */
    ATOM(".", Op.ANY, OpType.Other),
    //        public final Atom get(String i) {
//            return Atom.the(i);
//        }}
//

    /*
    <sseehh__> is it true thatdepvar only introduces across conjuncion subterms
    <sseehh__> whlie indepvar only introduces across statement subterms
    <sseehh__> or is there any other cases or exceptions to this
    <patham9> yes, thats true for all introduction rules
    <patham9> the reason is that independent variables are meant to capture the forall semantics by making use of the extension: if something is an A, it also is a B" (analogical with intension), so depends on this ==> copula
    <patham9> but also on the --> on both sides
    <patham9> while a dependent variable captures the some semantics, "something is A and B", which is a conjunction where both parts share this something
    <sseehh__> ok that makes it simple
    <sseehh__> we could call them 'conjunction variables' and 'statement variables'
     */
    VAR_INDEP(Symbols.VAR_INDEPENDENT,  6 /*NAL6 for Indep Vars */, OpType.Variable),
    VAR_DEP(Symbols.VAR_DEPENDENT, Op.ANY, OpType.Variable),
    VAR_QUERY(Symbols.VAR_QUERY, Op.ANY, OpType.Variable),


    NEG("--", 5, Args.One),

    INH("-->", 1, OpType.Relation, Args.Two),
    SIM("<->", true, 2, OpType.Relation, Args.Two),

    /** extensional intersection */
    SECTe("&", true, 3, Args.GTETwo),

    /** intensional intersection */
    SECTi("|", true, 3, Args.GTETwo),

    /** extensional difference */
    DIFFe("-", 3, Args.Two),

    /** intensional difference */
    DIFFi("~", 3, Args.Two),

    /** PRODUCT */
    PROD("*", 4, Args.GTEZero),

    /** extensional image */
    IMGe("/", 4, Args.GTEOne),

    /** intensional image */
    IMGi("\\", 4, Args.GTEOne),


    /** conjunction */
    CONJ("&&", true, 5, Args.GTETwo),

    //SPACE("+", true, 7, Args.GTEOne),


    /** intensional set */
    SETi("[", true, 2, Args.GTEOne), //OPENER also functions as the symbol for the entire compound

    /** extensional set */
    SETe("{", true, 2, Args.GTEOne), //OPENER also functions as the symbol for the entire compound


    /** implication */
    IMPL("==>", 5, OpType.Relation, Args.Two),

    /** equivalence */
    EQUI("<=>", true, 5, OpType.Relation, Args.Two),


    // keep all items which are invlved in the lower 32 bit structuralHash above this line
    // so that any of their ordinal values will not exceed 31
    //-------------
    //NONE('\u2205', Op.ANY, null),

//    /** Termject */
//    OBJECT("`", Op.ANY, OpType.Other),
    INT("`i", Op.ANY, OpType.Other),

    VAR_PATTERN(Symbols.VAR_PATTERN, Op.ANY, OpType.Variable),


    //VIRTUAL TERMS
    @Deprecated INSTANCE("-{-", 2, OpType.Relation),
    @Deprecated PROPERTY("-]-", 2, OpType.Relation),
    @Deprecated INSTANCE_PROPERTY("{-]", 2, OpType.Relation),
    @Deprecated DISJ("||", true, 5, Args.GTETwo),

    /** for ellipsis, when seen as a term */
    SUBTERMS("...", 1, OpType.Other );

    //-----------------------------------------------------


    /** Image index ("imdex") symbol */
    public static final Atomic Imdex = $.the("_");
    public static final int StatementBits = Op.or(Op.INH,Op.SIM,Op.IMPL,Op.EQUI);
    public static final int IMGbits = Op.or(Op.IMGe, Op.IMGi);
    public static final int OpBits = Op.or(Op.ATOM, Op.INH, Op.PROD);
    public static final int InhAndIMGbits = Op.or(Op.INH, Op.IMGe, Op.IMGi);

    /**
     * symbol representation of this getOperator
     */
    @NotNull
    public final String str;

    /**
     * character representation of this getOperator if symbol has length 1; else ch = 0
     */
    public final char ch;

    public final OpType type;

    /** arity limits, range is inclusive >= <=
     *  -1 for unlimited */
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
    public final boolean image;

    /** whether this involves an additional numeric component: 'dt' (for temporals) or 'relation' (for images) */
    public final boolean hasNumeric;


//    Op(char c, int minLevel) {
//        this(c, minLevel, Args.NoArgs);
//    }

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

        this.minSize= size.getOne();
        this.maxSize = size.getTwo();

        this.var = (type == OpType.Variable);

        this.statement = str.equals("-->") || str.equals("==>") || str.equals("<=>") || str.equals("<->");
        this.temporal = str.equals("&&") || str.equals("==>") || str.equals("<=>");
            //in(or(CONJUNCTION, IMPLICATION, EQUIV));

        this.image = str.equals("/") || str.equals("\\");

        this.hasNumeric = image || temporal;

        //negation does not contribute to structure vector
        this.bit = (1 << ordinal());

        this.atomic = var || str.equals(".") /* atom */ || str.equals("`i") || str.equals("^") || str.equals("`");

    }

    @Deprecated public static boolean isOperation(@NotNull Termed _t) {
        Term t = _t.term();
        if (t.op() == Op.INH) { //Op.hasAll(t.structure(), Op.OperationBits) &&
            Compound c = (Compound) t;
            return c.isTerm(1, Op.ATOM) &&
                   c.isTerm(0, Op.PROD);
        }
        return false;
    }

    public static boolean hasAll(int existing, int possiblyIncluded) {
        return ((existing | possiblyIncluded) == existing);
    }

    public static boolean hasAny(int existing, int possiblyIncluded) {
        return (existing & possiblyIncluded) != 0;
    }


    @NotNull
    @Override
    public String toString() {
        return str;
    }



    @NotNull
    public final String toString(@NotNull Compound c)  {
        int t = c.dt();

        return t == Tense.DTERNAL ?
                    str :
                    str + ((t >= 0) ? "+" : "") + (Integer.toString(t));
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

    public static int or(@NotNull int... i) {
        int bits = 0;
        for (int x : i) {
            bits |= x;
        }
        return bits;
    }
    public static int or(@NotNull Op... o) {
        int bits = 0;
        for (Op n : o)
            bits |= n.bit;
        return bits;
    }

    public static int or(int bits, @NotNull Op o) {
        return bits | o.bit;
    }


    /**
     * specifier for any NAL level
     */
    public static final int ANY = 0;


//    public static boolean isTemporal(@NotNull Term t, int newDT) {
//        return isTemporal(t.op(), newDT, t.size());
//    }
//
//    public static boolean isTemporal(@NotNull Op o, int dt, int arity) {
//        return o.temporal && !(arity > 2 && dt != 0 && o == Op.CONJ && dt != DTERNAL && dt != XTERNAL);
//    }
//
//    public boolean validSize(int length) {
//        int min = this.minSize;
//        if (min!=-1 && length < min) return false;
//        int max = this.maxSize;
//        return !(max != -1 && length > max);
//    }


    /** true if matches any of the on bits of the vector */
    public final boolean in(int vector) {
        return in(bit, vector);
    }

    static boolean in(int needle, int haystack) {
        return (needle & haystack) == needle;
    }

    public boolean isSet() {
        return in(SetsBits);
    }

    public static boolean hasAny(int structure, @NotNull Op o) {
        return hasAny(structure, o.bit);
    }

    public final boolean isIntersect() {
        return this == Op.SECTe || this == Op.SECTi;
    }


    /** top-level Op categories */
    public enum OpType {
        Relation,
        Variable,
        Other
    }


//    @Deprecated public static final int ImplicationsBits =
//            Op.or(Op.IMPLICATION);
//
//    @Deprecated public static final int ConjunctivesBits =
//            Op.or(Op.CONJUNCTION);

//    @Deprecated public static final int EquivalencesBits =
//            Op.or(Op.EQUIV);

    public static final int SetsBits =
            Op.or(Op.SETe, Op.SETi);





//    public static int VarDepOrIndep = Op.or( Op.VAR_DEP, Op.VAR_INDEP );
//    public static final int ProductOrImageBits = or(Op.PRODUCT, Op.IMAGE_EXT, Op.IMAGE_INT);
    public static final int ImplicationOrEquivalenceBits = or(Op.EQUI, Op.IMPL);
    public static final int TemporalBits = or(Op.CONJ, Op.EQUI, Op.IMPL);

    public static final int ImageBits =
        Op.or(Op.IMGe,Op.IMGi);

    public static final int VariableBits =
        Op.or(Op.VAR_PATTERN,Op.VAR_INDEP,Op.VAR_DEP,Op.VAR_QUERY);
//    public static final int WildVariableBits =
//            Op.or(Op.VAR_PATTERN,Op.VAR_QUERY);



    //MACRO OPS as Strings only
//    /** Macro: DISJ("||", true, 5, Args.GTETwo) */
//    public static final String DISJ = "||";
//
//    /** Macro: INSTANCE("-{-", 2, OpType.Relation) */
//    public static final String INSTANCE = "-{-";
//
//    /** Macro: PROPERTY("-]-", 2, OpType.Relation) */
//    public static final String PROPERTY = "-]-";
//
//    /** Macro: INSTANCE_PROPERTY("{-]", 2, OpType.Relation) */
//    public static final String INSTANCE_PROPERTY = "{-]";




    enum Args {
        ;
        static final IntIntPair None = pair(0,0);
        static final IntIntPair One = pair(1,1);
        static final IntIntPair Two = pair(2,2);

        static final IntIntPair GTEZero = pair(0,-1);
        static final IntIntPair GTEOne = pair(1,-1);
        static final IntIntPair GTETwo = pair(2,-1);

    }

    public static final int[] NALLevelEqualAndAbove = new int[8+1]; //indexed from 0..7, meaning index 7 is NAL8, index 0 is NAL1
    static {
        for (Op o : Op.values()) {
            int l = o.minLevel;
            if (l < 0) l = 0; //count special ops as level 0, so they can be detected there
            for (int i = l; i <= 8; i++) {
                NALLevelEqualAndAbove[i] |= o.bit;
            }
        }
    }


    public static class InvalidPunctuationException extends RuntimeException {
        public InvalidPunctuationException(char c) {
            super("Invalid punctuation: " + c);
        }
    }
}
