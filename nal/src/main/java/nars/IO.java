package nars;


import jcog.Texts;
import jcog.bag.Prioritized;
import jcog.data.byt.DynByteSeq;
import nars.budget.Budgeted;
import nars.index.term.TermIndex;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.compound.SerialCompound;
import nars.term.container.TermContainer;
import nars.term.var.AbstractVariable;
import nars.term.var.GenericVariable;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import static nars.IO.TaskSerialization.TermFirst;
import static nars.Op.ATOM;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * Created by me on 5/29/16.
 */
public class IO {


    public static final byte SPECIAL_OP = (byte) (Op.values().length + 1);

    static boolean hasTruth(char punc) {
        return punc == Op.BELIEF || punc == Op.GOAL;
    }


    @NotNull
    public static MutableTask readTask(@NotNull DataInput in, @NotNull TermIndex t) throws IOException {

        Term term = readTerm(in, t);

        //TODO combine these into one byte
        char punc = (char) in.readByte();

        Truth truth;
        if (hasTruth(punc)) {
            truth = readTruth(in);
        } else {
            truth = null;
        }

        long start = in.readLong();
        long end = in.readLong();

        long[] evi = readEvidence(in);

        float pri = in.readFloat();
        float qua = in.readFloat();

        long cre = in.readLong();


        MutableTask mm = new MutableTask(term, punc, truth).time(cre, start, end);
        mm.evidence(evi);
        mm.setBudget(pri, qua);
        return mm;
    }

    @NotNull
    public static long[] readEvidence(@NotNull DataInput in) throws IOException {
        int eviLength = in.readByte();
        long[] evi = new long[eviLength];
        for (int i = 0; i < eviLength; i++) {
            evi[i] = in.readLong();
        }
        return evi;
    }

    @NotNull
    public static Truth readTruth(@NotNull DataInput in) throws IOException {
        return Truth.intToTruth(in.readInt());
    }


    /**
     * with Term first
     */
    public static void writeTask(@NotNull DataOutput out, @NotNull Task t) throws IOException {

        writeTerm(out, t.term());

        char p = t.punc();
        out.writeByte(p);

        if (hasTruth(p))
            writeTruth(out, t);

        out.writeLong(t.start());
        out.writeLong(t.end());

        writeEvidence(out, t.evidence());

        writeBudget(out, t);

        out.writeLong(t.creation()); //put this last because it is the least useful really

    }

    /**
     * with Term last
     */
    public static void writeTask2(@NotNull DataOutput out, @NotNull Task t) throws IOException {

        char p = t.punc();
        out.writeByte(p);

        writeBudget(out, t);

        out.writeLong(t.start());
        out.writeLong(t.end());

        if (hasTruth(p)) {
            out.writeFloat(t.freq());
            out.writeFloat(t.conf());
        }

        //writeEvidence(out, t.evidence());

        //out.writeLong(t.creation()); //put this last because it is the least useful really

        writeTermStringUTF(out, t);
    }

    public static void writeTermStringUTF(@NotNull DataOutput out, @NotNull Termed t) throws IOException {
        writeStringUTF(out, t.term().toString());
    }

    public static void writeStringUTF(@NotNull DataOutput out, String s) throws IOException {

        //byte[] bb = s.getBytes(Charset.defaultCharset());
        byte[] bb = s.getBytes(Charset.defaultCharset()); //Hack.bytes(s);
        out.writeShort(bb.length);
        out.write(bb);
    }

    public static void writePriority(@NotNull DataOutput out, @NotNull Prioritized t) throws IOException {
        out.writeFloat(t.priSafe(0));
    }

    public static void writeBudget(@NotNull DataOutput out, @NotNull Budgeted t) throws IOException {
        writePriority(out, t);
        out.writeFloat(t.qua());
    }

    public static void writeEvidence(@NotNull DataOutput out, @NotNull long[] evi) throws IOException {
        int evil = evi.length;
        out.writeByte(evil);
        for (int i = 0; i < evil; i++)
            out.writeLong(evi[i]);
    }

    public static void writeTruth(@NotNull DataOutput out, @NotNull Truthed t) throws IOException {
        Truth tt = t.truth();
        out.writeInt(Truth.truthToInt(tt.freq(), tt.conf()));
    }


    @Nullable
    public static Atomic readVariable(@NotNull DataInput in, @NotNull Op o, @NotNull TermIndex t) throws IOException {
        return $.v(o, in.readByte());
    }

    @Nullable
    public static Atomic readAtomic(@NotNull DataInput in, @NotNull Op o, @NotNull TermIndex t) throws IOException {
        String s = in.readUTF();
        switch (o) {

            case ATOM:
                return (Atomic) t.get(new Atom(s), true);

            default:
                int maybeI = Texts.i(s, Integer.MIN_VALUE);
                if (maybeI != Integer.MIN_VALUE) {
                    return $.the(maybeI);
                }

                try {
                    return $.$(s);
                } catch (Narsese.NarseseException e) {
                    throw new UnsupportedEncodingException(e.getMessage());
                }
                //throw new UnsupportedOperationException();
        }

        //return (Atomic) t.get(key, true); //<- can cause synchronization deadlocks
    }


    /**
     * called by readTerm after determining the op type
     */
    @Nullable
    public static Term readTerm(@NotNull DataInput in, @NotNull TermIndex t) throws IOException {

        byte ob = in.readByte();
        if (ob == SPECIAL_OP)
            return readSpecialTerm(in, t);

        Op o = Op.values()[ob];
        if (o.var)
            return readVariable(in, o, t);
        else if (o.atomic)
            return readAtomic(in, o, t);
        else
            return readCompound(in, o, t);
    }

    public
    @Nullable
    static Term readSpecialTerm(@NotNull DataInput in, @NotNull TermIndex t) throws IOException {
        try {
            return t.parseRaw(in.readUTF());
        } catch (Narsese.NarseseException e) {
            throw new IOException(e);
        }
    }

    public static void writeTerm(@NotNull DataOutput out, @NotNull Term term) throws IOException {


        if (term instanceof SerialCompound) {
            //it's already serialized in a SerialCompound
            ((SerialCompound)term).appendTo(out);
            return;
        }

        //HACK this should not be necessary
        if (isSpecial(term)) {
            out.writeByte(SPECIAL_OP);
            out.writeUTF(term.toString());
            return;
        }

        Op o = term.op();
        out.writeByte(o.ordinal());

        if (term instanceof Atomic) {

            if (term instanceof AbstractVariable) {
                out.writeByte(((AbstractVariable) term).id);
            } else {
                out.writeUTF(term.toString()); //TODO use StringHack ?
            }
        } else {

            @NotNull TermContainer c = ((Compound) term).subterms();
            int siz = c.size();

            out.writeByte(siz);

            for (int i = 0; i < siz; i++) {
                writeTerm(out, c.term(i));
            }

            writeCompoundSuffix(out, ((Compound)term).dt(), o);
        }
    }

    public static void writeCompoundSuffix(@NotNull DataOutput out, int dt, Op o) throws IOException {
        if (o.image)
            out.writeByte((byte) dt);
        else if (o.temporal)
            out.writeInt(dt);
    }

    public static boolean isSpecial(@NotNull Term term) {
        return term instanceof GenericVariable;
    }


    @Nullable
    public static Term[] readTermContainer(@NotNull DataInput in, @NotNull TermIndex t) throws IOException {
        int siz = in.readByte();
        Term[] s = new Term[siz];
        for (int i = 0; i < siz; i++) {
            s[i] = readTerm(in, t);
        }

        return s;
    }

    /**
     * called by readTerm after determining the op type
     * TODO make a version which reads directlyinto TermIndex
     */
    @Nullable
    static Term readCompound(@NotNull DataInput in, @NotNull Op o, @NotNull TermIndex t) throws IOException {

        Term[] v = readTermContainer(in, t);

        int dt = DTERNAL;

        if (o.image)
            dt = in.readByte();
        else if (o.temporal)
            dt = in.readInt();

        return t.the(o, dt, v);
//        if (key == null)
//            throw new UnsupportedOperationException();
//        return (Compound) t.normalize(key, true);
    }

    public static byte[] asBytes(@NotNull Task t) {
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            IO.writeTask(new DataOutputStream(bs), t);
            return bs.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] asBytes(@NotNull Term t) {
        try {
            DynByteSeq d = new DynByteSeq(t.volume() * 4 /* estimate */);
            //ByteArrayOutputStream bs = new ByteArrayOutputStream();
            IO.writeTerm(d, t);
            return d.array(); //bs.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveTasksToTemporaryTSVFile(NAR nar) throws IOException {
        Path f = Files.createTempFile(Paths.get("/tmp"), "nar", ".tsv");
        System.out.println("saving tasks: " + f);
        FileOutputStream os = new FileOutputStream(f.toFile());
        PrintStream ps = new PrintStream(os);
        nar.forEachTask(t -> {
            Task tt = nar.post(t);
            try {
                tt.appendTSV(ps);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void saveTasksToTemporaryTextFile(NAR nar) throws IOException {
        Path f = Files.createTempFile(Paths.get("/tmp"), "nar", ".nal");
        System.out.println("saving tasks: file://" + f);
        FileOutputStream os = new FileOutputStream(f.toFile());
        PrintStream ps = new PrintStream(os);
        nar.forEachTask(t -> {
            Task tt = nar.post(t);
            try {
                tt.appendTo(ps);
                ps.append('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public enum TaskSerialization {
        TermFirst,
        TermLast
    }

    @Nullable
    public static byte[] taskToBytes(@NotNull Task x) {
        return taskToBytes(x, TermFirst);
    }

    public static byte[] taskToBytes(@NotNull Task x, @NotNull TaskSerialization mode) {
        try {
            DynByteSeq dos = new DynByteSeq(x.volume() * 16);
            switch (mode) {
                case TermFirst:
                    IO.writeTask(dos, x);
                    break;
                case TermLast:
                    IO.writeTask2(dos, x);
                    break;
            }
            return dos.array();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Task taskFromBytes(@NotNull byte[] b, @NotNull TermIndex index) {
        try {
            return IO.readTask(input(b), index);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Term termFromBytes(@NotNull byte[] b, @NotNull TermIndex index) {
        try {
            return IO.readTerm(input(b), index);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static DataInputStream input(@NotNull byte[] b) {
        return new DataInputStream(new ByteArrayInputStream(b));
    }

    public static DataInputStream input(@NotNull byte[] b, int offset) {
        return new DataInputStream(new ByteArrayInputStream(b, offset, b.length - offset));
    }


    public interface Printer {

        //    static void appendSeparator(@NotNull Appendable p) throws IOException {
        //        p.append(ARGUMENT_SEPARATOR);
        //        //if (pretty) p.append(' ');
        //    }
        //
        //    static void writeCompound1(@NotNull Op op, @NotNull Term singleTerm, @NotNull Appendable writer) throws IOException {
        //        writer.append(COMPOUND_TERM_OPENER);
        //        writer.append(op.str);
        //        writer.append(ARGUMENT_SEPARATOR);
        //        singleTerm.append(writer);
        //        writer.append(COMPOUND_TERM_CLOSER);
        //    }

        static void compoundAppend(@NotNull Compound c, @NotNull Appendable p) throws IOException {

            p.append(Op.COMPOUND_TERM_OPENER);

            c.op().append(c, p);

            if (c.size() == 1)
                p.append(Op.ARGUMENT_SEPARATOR);

            appendArgs(c, p);

            appendCloser(p);

        }

        static void compoundAppend(String o, @NotNull TermContainer c, @NotNull Function<Term, Term> filter, @NotNull Appendable p) throws IOException {

            p.append(Op.COMPOUND_TERM_OPENER);

            p.append(o);

            if (c.size() == 1)
                p.append(Op.ARGUMENT_SEPARATOR);

            appendArgs(c, filter, p);

            appendCloser(p);

        }


        static void appendArgs(@NotNull Compound c, @NotNull Appendable p) throws IOException {
            int nterms = c.size();

            boolean bb = nterms > 1;
            for (int i = 0; i < nterms; i++) {
                if ((i != 0) || bb) {
                    p.append(Op.ARGUMENT_SEPARATOR);
                }
                c.term(i).append(p);
            }
        }

        static void appendArgs(@NotNull TermContainer c, @NotNull Function<Term, Term> filter, @NotNull Appendable p) throws IOException {
            int nterms = c.size();

            boolean bb = nterms > 1;
            for (int i = 0; i < nterms; i++) {
                if ((i != 0) || bb) {
                    p.append(Op.ARGUMENT_SEPARATOR);
                }
                filter.apply(c.term(i)).append(p);
            }
        }

        static void appendCloser(@NotNull Appendable p) throws IOException {
            p.append(Op.COMPOUND_TERM_CLOSER);
        }

        static void append(@NotNull Compound c, @NotNull Appendable p) throws IOException {
            final Op op = c.op();

            switch (op) {

                case SETi:
                case SETe:
                    setAppend(c, p);
                    return;
                case PROD:
                    productAppend(c, p);
                    return;
                case IMGi:
                case IMGe:
                    imageAppend(c, p);
                    return;
                //case INHERIT: inheritAppend(c, p, pretty); return;
                //case SIMILAR: similarAppend(c, p, pretty); return;

                case NEG:
                    //special case disjunction: (--,(&&,.....))
                    if (Terms.isDisjunction(c)) {
                        compoundAppend(Op.DISJ.toString(), ((Compound) c.term(0)).subterms(), $::neg, p);
                        return;
                    }
            }

            if (op.statement || c.size() == 2) {
                Term subj = c.term(0);

                //special case: functional form
                if (subj.op() == Op.PROD) {
                    Term pred = c.term(1);
                    Op pOp = pred.op();
                    if (pOp == ATOM) {
                        operationAppend((Compound) c.term(0), (Atomic) pred, p);
                        return;
                    }
                }

                statementAppend(c, p, op);

            } else {
                compoundAppend(c, p);
            }
        }

//        static void inheritAppend(@NotNull Compound c, @NotNull Appendable p) throws IOException {
//            Term a = Statement.subj(c);
//            Term b = Statement.pred(c);
//
//            p.append(Symbols.COMPOUND_TERM_OPENER);
//            b.append(p);
//            p.append(Symbols.INHERIT_SEPARATOR);
//            a.append(p);
//            p.append(Symbols.COMPOUND_TERM_CLOSER);
//        }
//        static void similarAppend(@NotNull Compound c, @NotNull Appendable p) throws IOException {
//            Term a = Statement.subj(c);
//            Term b = Statement.pred(c);
//
//            p.append(Symbols.COMPOUND_TERM_OPENER);
//            a.append(p);
//            p.append(Symbols.SIMILAR_SEPARATOR);
//            b.append(p);
//            p.append(Symbols.COMPOUND_TERM_CLOSER);
//        }

        static void statementAppend(@NotNull Compound c, @NotNull Appendable p, @NotNull Op op) throws IOException {
            Term a = Terms.subj(c);
            Term b = Terms.pred(c);

            int dt = c.dt();
            boolean reversedDT;
            if (dt < 0 && c.isCommutative() && dt != DTERNAL && dt != XTERNAL) {
                reversedDT = true;
                Term x = a;
                a = b;
                b = x;
            } else {
                reversedDT = false;
            }

            p.append(Op.COMPOUND_TERM_OPENER);
            a.append(p);

            op.append(c, p, reversedDT);

            b.append(p);

            p.append(Op.COMPOUND_TERM_CLOSER);
        }


        static void productAppend(@NotNull Compound product, @NotNull Appendable p) throws IOException {

            int s = product.size();
            p.append(Op.COMPOUND_TERM_OPENER);
            for (int i = 0; i < s; i++) {
                product.term(i).append(p);
                if (i < s - 1) {
                    p.append(",");
                }
            }
            p.append(Op.COMPOUND_TERM_CLOSER);
        }

        static void imageAppend(@NotNull Compound image, @NotNull Appendable p) throws IOException {

            int len = image.size();

            p.append(Op.COMPOUND_TERM_OPENER);
            p.append(image.op().str);

            int relationIndex = image.dt();
            int i;
            for (i = 0; i < len; i++) {
                Term tt = image.term(i);

                p.append(Op.ARGUMENT_SEPARATOR);
                //if (pretty) p.append(' ');

                if (i == relationIndex) {
                    p.append(Op.IMAGE_PLACE_HOLDER);
                    p.append(Op.ARGUMENT_SEPARATOR);
                    //if (pretty) p.append(' ');
                }

                tt.append(p);
            }
            if (i == relationIndex) {
                p.append(Op.ARGUMENT_SEPARATOR);
                //if (pretty) p.append(' ');
                p.append(Op.IMAGE_PLACE_HOLDER);
            }

            p.append(Op.COMPOUND_TERM_CLOSER);

        }

        static void setAppend(@NotNull Compound set, @NotNull Appendable p) throws IOException {

            int len = set.size();

            //duplicated from above, dont want to store this as a field in the class
            char opener, closer;
            if (set.op() == Op.SETe) {
                opener = Op.SETe.ch;
                closer = Op.SET_EXT_CLOSER;
            } else {
                opener = Op.SETi.ch;
                closer = Op.SET_INT_CLOSER;
            }

            p.append(opener);
            for (int i = 0; i < len; i++) {
                Term tt = set.term(i);
                if (i != 0) p.append(Op.ARGUMENT_SEPARATOR);
                tt.append(p);
            }
            p.append(closer);
        }

        static void operationAppend(@NotNull Compound argsProduct, @NotNull Atomic operator, @NotNull Appendable p) throws IOException {

            //Term predTerm = operator.identifier(); //getOperatorTerm();
            //        if ((predTerm.volume() != 1) || (predTerm.hasVar())) {
            //            //if the predicate (operator) of this operation (inheritance) is not an atom, use Inheritance's append format
            //            appendSeparator(p, pretty);
            //            return;
            //        }


            Term[] xt = argsProduct.terms();

            p.append(operator.toString());

            p.append(Op.COMPOUND_TERM_OPENER);

            int n = 0;
            for (Term t : xt) {
                if (n != 0) {
                    p.append(Op.ARGUMENT_SEPARATOR);
                    /*if (pretty)
                        p.append(' ');*/
                }

                t.append(p);


                n++;
            }

            p.append(Op.COMPOUND_TERM_CLOSER);

        }


        @NotNull
        static StringBuilder stringify(@NotNull Compound c) {
            StringBuilder sb = new StringBuilder(/* conservative estimate */ c.volume() * 2);
            try {
                c.append(sb);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return sb;
        }
    }

//    public static Term fromJSON(String json) {
//        JsonValue v = Json.parse(json);
//        return fromJSON(v);
//    }
//
//    public static Term toJSON(Term term) {
//        return $.func("json", $.quote(toJSONValue(term)));
//    }
//
//    public static JsonValue toJSONValue(Term term) {
//        switch (term.op()) {
//
//            //TODO other types
//
//            /*case SETe: {
//                JsonObject o = Json.object();
//                for (Term x : term)
//                    o.add
//            }*/
//            case PROD:
//                JsonArray a = (JsonArray) Json.array();
//                for (Term x : ((Compound) term))
//                    a.add(toJSONValue(x));
//                return a;
//            default:
//                return Json.value(term.toString());
//        }
//    }
//
//    public static Term fromJSON(JsonValue v) {
//        if (v instanceof JsonObject) {
//            JsonObject o = (JsonObject) v;
//            int s = o.size();
//            List<Term> members = $.newArrayList(s);
//            o.forEach(m -> members.add($.inh(fromJSON(m.getValue()), $.the(m.getName()))));
//            return $.
//                    //parallel
//                            sete
//                    //secte
//                            (members/*.toArray(new Term[s])*/);
//
//        } else if (v instanceof JsonArray) {
//            JsonArray o = (JsonArray) v;
//            List<Term> vv = $.newArrayList(o.size());
//            o.forEach(x -> vv.add(fromJSON(x)));
//            return $.p(vv);
//        }
//        String vv = v.toString();
//        return $.the(vv);
//        //return $.quote(vv);
//    }

    /**
     * Writes a string to the specified DataOutput using
     * <a href="DataInput.html#modified-utf-8">modified UTF-8</a>
     * encoding in a machine-independent manner.
     * <p>
     * First, two bytes are written to out as if by the <code>writeShort</code>
     * method giving the number of bytes to follow. This value is the number of
     * bytes actually written out, not the length of the string. Following the
     * length, each character of the string is output, in sequence, using the
     * modified UTF-8 encoding for the character. If no exception is thrown, the
     * counter <code>written</code> is incremented by the total number of
     * bytes written to the output stream. This will be at least two
     * plus the length of <code>str</code>, and at most two plus
     * thrice the length of <code>str</code>.
     *
     * @param out destination to write to
     * @param str a string to be written.
     * @return The number of bytes written out.
     * @throws IOException if an I/O error occurs.
     */
    public static void writeUTFWithoutLength(@NotNull DataOutput out, @NotNull String str) throws IOException {


        //int c, count = 0;

//        /* use charAt instead of copying String to char array */
//        for (int i = 0; i < strlen; i++) {
//            c = str.charAt(i);
//            if ((c >= 0x0001) && (c <= 0x007F)) {
//                utflen++;
//            } else if (c > 0x07FF) {
//                utflen += 3;
//            } else {
//                utflen += 2;
//            }
//        }
//
//        if (utflen > 65535)
//            throw new UTFDataFormatException(
//                    "encoded string too long: " + utflen + " bytes");

        //byte[] bytearr = null;
//        if (out instanceof DataOutputStream) {
//            DataOutputStream dos = (DataOutputStream)out;
//            if(dos.bytearr == null || (dos.bytearr.length < (utflen+2)))
//                dos.bytearr = new byte[(utflen*2) + 2];
//            bytearr = dos.bytearr;
//        } else {
        //bytearr = new byte[utflen];
//        }

        //Length information, not written
        //bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
        //bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);

        int strlen = str.length();
        int i, c;
        for (i = 0; i < strlen; i++) {
            c = str.charAt(i);
            if (!((c >= 0x0001) && (c <= 0x007F))) break;
            out.writeByte((byte) c);
        }

        for (; i < strlen; i++) {
            c = str.charAt(i);
            if ((c >= 0x0001) && (c <= 0x007F)) {
                out.writeByte((byte) c);

            } else if (c > 0x07FF) {
                out.writeByte((byte) (0xE0 | ((c >> 12) & 0x0F)));
                out.writeByte((byte) (0x80 | ((c >> 6) & 0x3F)));
                out.writeByte((byte) (0x80 | ((c >> 0) & 0x3F)));
            } else {
                out.writeByte((byte) (0xC0 | ((c >> 6) & 0x1F)));
                out.writeByte((byte) (0x80 | ((c >> 0) & 0x3F)));
            }
        }
    }

    /**
     * visits each subterm of a compound and stores a tuple of integers for it
     */


    @FunctionalInterface  public interface EachTerm {
        void nextTerm(Op o, int depth, int byteStart);
    }

    public static void mapSubTerms(byte[] term, EachTerm t) throws IOException {

        int l = term.length;
        int i = 0;

        int level = 0;
        final int MAX_LEVELS = 32;
        byte[][] levels = new byte[MAX_LEVELS][2]; //level stack x (op, subterms remaining) tuple

        do {

            int termStart = i;
            byte ob = term[i]; i++;
            Op o = Op.values()[ob];
            t.nextTerm(o, level, termStart);


            if (o.var) {
                i += 1; //int id = input(term, i).readByte();
            } else if (o.atomic) {
                //bytearr[count++] = (byte) ((utflen >>> 8) & 0xFF);
                //bytearr[count++] = (byte) ((utflen >>> 0) & 0xFF);
//                DataInputStream ii = input(term, i);
                int hi = term[i++] & 0xff;
                int lo = term[i++] & 0xff; //ii.readUnsignedByte();
                int utfLen = //input(term, i).readUnsignedShort();
                        (hi << 8) | lo;
                //i += 2;
                i += utfLen;

                //null-terminated mode
//                while (term[i++]!=0 /* null terminator */) { }
            } else {

                int subterms = input(term, i).readUnsignedByte();
                i++;
                levels[level][0] = ob;
                levels[level][1] = (byte) (subterms  /* include this? */);
                level++;

            }

            pop: while (level > 0) {
                byte[] ll = levels[level - 1];
                byte subtermsRemain = ll[1];
                if (subtermsRemain == 0) {
                    //end of compound:
                    Op ol = Op.values()[ll[0]];
                    if (ol.image)
                        i++; //skip image index byte
                    else if (ol.temporal)
                        i += 4; //skip temporal dt (32 bits)
                    level--;
                    continue pop; //see if the next level up is finished
                } else {
                    ll[1] = (byte)(subtermsRemain - 1);
                    break; //continue to next subterm
                }
            }

        } while (i < l);

        if (i != l) {
            throw new IOException("decoding error");
        }
    }

}
//    /**
//     * serialization and deserialization of terms, tasks, etc.
//     */
//    public static class DefaultCodec extends FSTConfiguration {
//
//        final TermIndex index;
//
//        public DefaultCodec(TermIndex t) {
//            super(null);
//
//            this.index = t;
//
//            createDefaultConfiguration();
//            //setStreamCoderFactory(new FBinaryStreamCoderFactory(this));
//            setForceSerializable(true);
//
//
//            //setCrossPlatform(false);
//            setShareReferences(false);
//            setPreferSpeed(true);
//            setCrossPlatform(false);
//
//
//            registerClass(Atom.class, GenericCompound.class,
//                    AbstractTask.class,
//                    Term[].class,
//                    TermContainer.class,
//                    //long[].class, char.class,
//                    Op.class);
//
//
//            registerSerializer(AbstractTask.class, new FSTBasicObjectSerializer() {
//
//                @Override
//                public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
//                }
//
//                @NotNull
//                @Override
//                public Object instantiate(Class objectClass, @NotNull FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws IOException {
//                    return readTask(in, index);
//                }
//
//                @Override
//                public void writeObject(@NotNull FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
//                    writeTask(out, (Task) toWrite);
//                }
//            }, true);
//
//
//            registerSerializer(Atom.class, terms, true);
//            registerSerializer(Atomic.class, terms, true);
//
//            registerSerializer(GenericCompound.class, terms, true);
//
//            registerSerializer(AtomConcept.class, terms, true);
//            registerSerializer(CompoundConcept.class, terms, true);
//
//        }
//
////        @Nullable
////        final FSTBasicObjectSerializer termContainers = new FSTBasicObjectSerializer() {
////
////            @Override
////            public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
////            }
////
////            @Nullable
////            @Override
////            public Object instantiate(Class objectClass, @NotNull FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws IOException {
////                return readTermContainer(in, index);
////            }
////
////            @Override
////            public void writeObject(@NotNull FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
////                writeTermContainer(out, (TermContainer) toWrite);
////            }
////        };
//
//        @Nullable
//        final FSTBasicObjectSerializer terms = new FSTBasicObjectSerializer() {
//
//            @Override
//            public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
//            }
//
//            @Nullable
//            @Override
//            public Object instantiate(Class objectClass, @NotNull FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws IOException {
//                return readTerm(in, index);
//            }
//
//            @Override
//            public void writeObject(@NotNull FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
//                writeTerm(out, (Term) toWrite);
//            }
//        };
//
//
//    }
//
