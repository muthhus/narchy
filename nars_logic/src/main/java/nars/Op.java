package nars;


import com.gs.collections.api.tuple.primitive.IntIntPair;
import nars.nal.Tense;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static com.gs.collections.impl.tuple.primitive.PrimitiveTuples.pair;

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
    VAR_INDEP(Symbols.VAR_INDEPENDENT, 6 /*NAL6 for Indep Vars */, OpType.Variable),
    VAR_DEP(Symbols.VAR_DEPENDENT, Op.ANY, OpType.Variable),
    VAR_QUERY(Symbols.VAR_QUERY, Op.ANY, OpType.Variable),

    OPERATOR("^", 8, Args.One),

    NEGATE("--", 5, Args.One),

    /* Relations */
    INHERIT("-->", 1, OpType.Relation, Args.Two),
    SIMILAR("<->", true, 2, OpType.Relation, Args.Two),


    /* CompountTerm operators */
    INTERSECT_EXT("&", true, 3, Args.GTETwo),
    INTERSECT_INT("|", true, 3, Args.GTETwo),

    DIFF_EXT("-", 3, Args.Two),
    DIFF_INT("~", 3, Args.Two),

    PRODUCT("*", 4, Args.GTEZero),

    IMAGE_EXT("/", 4, Args.GTEOne),
    IMAGE_INT("\\", 4, Args.GTEOne),

    /* CompoundStatement operators, length = 2 */
    DISJUNCTION("||", true, 5, Args.GTETwo),
    CONJUNCTION("&&", true, 5, Args.GTETwo),

    SPACE("+", true, 7, Args.GTEOne),


    /* CompountTerm delimiters, must use 4 different pairs */
    SET_INT_OPENER("[", true, 2, Args.GTEZero), //OPENER also functions as the symbol for the entire compound
    SET_EXT_OPENER("{", true, 2, Args.GTEZero), //OPENER also functions as the symbol for the entire compound


    IMPLICATION("==>", 5, OpType.Relation, Args.Two),

    EQUIV("<=>", true, 5, OpType.Relation, Args.Two),


    // keep all items which are invlved in the lower 32 bit structuralHash above this line
    // so that any of their ordinal values will not exceed 31
    //-------------
    NONE('\u2205', Op.ANY, null),

    VAR_PATTERN(Symbols.VAR_PATTERN, Op.ANY, OpType.Variable),

    INSTANCE("{--", 2, OpType.Relation), //should not be given a compact representation because this will not exist internally after parsing
    PROPERTY("--]", 2, OpType.Relation), //should not be given a compact representation because this will not exist internally after parsing
    INSTANCE_PROPERTY("{-]", 2, OpType.Relation); //should not be given a compact representation because this will not exist internally after parsing


    //-----------------------------------------------------


    /** Image index ("imdex") symbol */
    public static final Atom Imdex = $.the("_");



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
     * opener?
     */
    public final boolean opener;

    /**
     * closer?
     */
    public final boolean closer;

    /**
     * minimum NAL level required to use this operate, or 0 for N/A
     */
    public final int minLevel;

    private final boolean commutative;


//    Op(char c, int minLevel) {
//        this(c, minLevel, Args.NoArgs);
//    }

    Op(char c, int minLevel, OpType type) {
        this(c, minLevel, type, Args.None);
    }

    Op(@NotNull String s, boolean commutative, int minLevel) {
        this(s, minLevel, OpType.Other, Args.None);
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

        this.opener = name().endsWith("_OPENER");
        this.closer = name().endsWith("_CLOSER");

        this.minSize= size.getOne();
        this.maxSize = size.getTwo();


    }

    public static boolean isOperation(Term t) {
        if (!(t.isCompound())) return false;
        Compound c = (Compound)t;
        return !c.impossibleStructureMatch(OperationBits) &&
               c.size() == 2 &&
                c.op() == Op.INHERIT &&
                c.term(1).op() == Op.OPERATOR &&
                c.term(0).op() == Op.PRODUCT;
    }


    @NotNull
    @Override
    public String toString() {
        return str;
    }

    /**
     * alias
     */
    public static final Op SET_EXT = Op.SET_EXT_OPENER;
    public static final Op SET_INT = Op.SET_INT_OPENER;


    @NotNull
    public final String toString(@NotNull Compound c)  {
        int t = c.t();
        boolean hasTime = t != Tense.ITERNAL;

        if (!hasTime) {
            return str;
        } else {
            return str + ( (t >= 0) ? "+" : "") + (Integer.toString(t));
        }
    }

    /**
     * writes this operator to a Writer in (human-readable) expanded UTF16 mode
     */
    public final void append(@NotNull Compound c, @NotNull Appendable w) throws IOException {
        int t = c.t();
        boolean hasTime = t != Tense.ITERNAL;

        if (hasTime)
            w.append(' ');

        char ch = this.ch;
        if (ch == 0)
            w.append(str);
        else
            w.append(ch);

        if (hasTime) {
            if (t >= 0) w.append('+');
            w.append(Integer.toString(t)).append(' ');
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
            bits |= n.bit();
        return bits;
    }

    public final int bit() {
        return (1 << ordinal());
    }

    public static int or(int bits, @NotNull Op o) {
        return bits | o.bit();
    }


    /**
     * specifier for any NAL level
     */
    public static final int ANY = 0;

    public final boolean isVar() {
        return type == OpType.Variable;
    }

    public boolean isCommutative() {
        return commutative;
    }

    public final static int TemporalBits = or(CONJUNCTION, IMPLICATION, EQUIV);

    /** whether this op allows temporal relation (true) or ignores it  (false) */
    public boolean isTemporal() {
        return in(TemporalBits);
    }

    public boolean validSize(int length) {
        int min = this.minSize;
        if (min!=-1 && length < min) return false;
        int max = this.maxSize;
        return !(max != -1 && length > max);
    }

    public boolean isImage() {
        return in(ImageBits);
    }

    public boolean isConjunctive() {
        return this == CONJUNCTION; //in(ConjunctivesBits);
    }


    public boolean isStatement() {
        return in(StatementBits);
    }

    /** true if matches any of the on bits of the vector */
    public boolean in(int vector) {
        return in(bit(), vector);
    }

    static boolean in(int needle, int haystack) {
        return (needle & haystack) == needle;
    }

    public boolean isSet() {
        return in(SetsBits);
    }

    public boolean isImplication() {
        return this == IMPLICATION; //in(ImplicationsBits);
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
            Op.or(Op.SET_EXT, Op.SET_INT);

    /** all Operations will have these 3 elements in its subterms: */
    public static final int OperationBits =
            Op.or(Op.INHERIT, Op.PRODUCT, OPERATOR);

    public static final int StatementBits =
            Op.or(Op.INHERIT, Op.SIMILAR,
                    Op.EQUIV,
                    Op.IMPLICATION
            );

    public static final int ProductOrImageBits = or(Op.PRODUCT, Op.IMAGE_EXT, Op.IMAGE_INT);
    public static final int ImplicationOrEquivalenceBits = or(Op.EQUIV, Op.IMPLICATION);

    public static final int ImageBits =
        Op.or(Op.IMAGE_EXT,Op.IMAGE_INT);

    public static final int VariableBits =
        Op.or(Op.VAR_PATTERN,Op.VAR_INDEP,Op.VAR_DEP,Op.VAR_QUERY);
    public static final int WildVariableBits =
            Op.or(Op.VAR_PATTERN,Op.VAR_QUERY);



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
                NALLevelEqualAndAbove[i] |= o.bit();
            }
        }
    }


    public static class InvalidPunctuationException extends RuntimeException {
        public InvalidPunctuationException(char c) {
            super("Invalid punctuation: " + c);
        }
    }
}
