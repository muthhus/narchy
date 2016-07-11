package nars.util;

import nars.$;
import nars.Op;
import nars.Symbols;
import nars.concept.AtomConcept;
import nars.concept.CompoundConcept;
import nars.index.TermIndex;
import nars.nal.Tense;
import nars.task.AbstractTask;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Operator;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.variable.AbstractVariable;
import nars.term.variable.GenericVariable;
import nars.term.variable.Variable;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nustaq.serialization.*;

import java.io.*;

/**
 * Created by me on 5/29/16.
 */
public class IO {


    public static final byte SPECIAL_OP = (byte)(Op.values().length+1);

    static boolean hasTruth(char punc) {
        return punc == Symbols.BELIEF || punc == Symbols.GOAL;
    }


    @NotNull
    public static MutableTask readTask(@NotNull DataInput in, @NotNull TermIndex t) throws IOException {

        Term term = readTerm(in, t);

        //TODO combine these into one byte
        char punc = (char) in.readByte();
        int eviLength = in.readByte();

        Truth truth;
        if (hasTruth(punc)) {
            truth = readTruth(in);
        } else {
            truth = null;
        }

        long occ = in.readLong();
        long cre = in.readLong();


        long[] evi = new long[eviLength];
        for (int i = 0; i < eviLength; i++) {
            evi[i] = in.readLong();
        }

        float pri = in.readFloat();
        float dur = in.readFloat();
        float qua = in.readFloat();

        MutableTask mm = new MutableTask(term, punc, truth).time(cre, occ);
        mm.evidence(evi);
        mm.budget(pri, dur, qua);
        return mm;
    }

    @NotNull
    public static Truth readTruth(@NotNull DataInput in) throws IOException {
        float f = in.readFloat();
        float c = in.readFloat();
        return $.t(f, c);
    }

    public static void writeTask(@NotNull DataOutput out, @NotNull Task t) throws IOException {

        writeTerm(out, t.term());

        char p = t.punc();
        long[] evi = t.evidence();
        int evil;
        evil = evi == null ? 0 : evi.length;

        //TODO combine these into one byte
        out.writeByte(p);
        out.writeByte(evil); //out.writeByte((byte) evil);

        if (hasTruth(p)) {
            writeTruth(out, t.freq(), t.conf());
        }
        out.writeLong(t.occurrence());
        out.writeLong(t.creation());

        for (int i = 0; i < evil; i++)
            out.writeLong(evi[i]);

        out.writeFloat(t.pri());
        out.writeFloat(t.dur());
        out.writeFloat(t.qua());
    }

    public static void writeTruth(@NotNull DataOutput out, float freq, float conf) throws IOException {
        out.writeFloat(freq);
        out.writeFloat(conf);
    }

    public static void writeAtomic(@NotNull DataOutput out, @NotNull Atomic a) throws IOException {
        out.writeUTF(a.toString());
    }

    @Nullable
    public static Atomic readVariable(@NotNull DataInput in, @NotNull Op o, @NotNull TermIndex t) throws IOException {
        int x = in.readByte();
        return $.v(o, x);
    }
    @Nullable
    public static void writeVariable(@NotNull DataOutput out, @NotNull AbstractVariable v) throws IOException {
        out.writeByte(v.id);
    }

    @Nullable
    public static Atomic readAtomic(@NotNull DataInput in, @NotNull Op o, @NotNull TermIndex t) throws IOException {
        String s = in.readUTF();
        Term key;
        switch (o) {
            case ATOM:
                key = new Atom(s);
                break;
            case OPER:
                key = new Operator(s);
                break;
            default:
                throw new UnsupportedOperationException();
        }

        return (Atomic)t.the(key);
    }


    /**
     * called by readTerm after determining the op type */
    @Nullable
    public static Term readTerm(@NotNull DataInput in, @NotNull TermIndex t) throws IOException {

        byte ob = in.readByte();

        if (ob == SPECIAL_OP) {
            String toParse = in.readUTF();
            Term x = t.parseRaw(toParse);
            if (x == null)
                throw new IOException("Undecoded term: " + toParse);
            return x;
        }

        Op o = Op.values()[ob];
        if (o.var)
            return readVariable(in, o, t);
        else if (o.atomic)
            return readAtomic(in, o, t);
        else
            return readCompound(in, o, t);
    }
    public static void writeTerm(@NotNull DataOutput out, @NotNull Term term) throws IOException {

        if (isSpecial(term)) {
            out.writeByte(SPECIAL_OP);
            out.writeUTF(term.toString());
            return;
        }

        out.writeByte(term.op().ordinal());

        if (term instanceof Atomic) {

            if (term instanceof Variable)
                writeVariable(out, (AbstractVariable)term);
            else
                writeAtomic(out, (Atomic) term);
        } else
            writeCompound(out, (Compound)term);
    }

    public static boolean isSpecial(@NotNull Term term) {
        return term instanceof GenericVariable;
    }

    static void writeCompound(@NotNull DataOutput out, @NotNull Compound c) throws IOException {

        //how many subterms to follow
        writeTermContainer(out, c.subterms());

        if (c.op().isImage() || c.op().temporal)
            out.writeInt(c.dt());
    }

    static void writeTermContainer(@NotNull DataOutput out, @NotNull TermContainer c) throws IOException {
        int siz = c.size();
        out.writeByte(siz);
        for (int i = 0; i < siz; i++) {
            writeTerm(out, c.term(i));
        }
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
     * TODO make a version which reads directlyinto TermIndex */
    @Nullable
    static Compound readCompound(@NotNull DataInput in, @NotNull Op o, @NotNull TermIndex t) throws IOException {

        Term[] v = readTermContainer(in, t);

        int dt = Tense.DTERNAL;
        if (o.isImage() || o.temporal) //TODO o.hasNumeric
            dt = in.readInt();

        return (Compound) t.builder().build(o, dt, v);
//        if (key == null)
//            throw new UnsupportedOperationException();
//        return (Compound) t.normalize(key, true);
    }

    public static byte[] asBytes(@NotNull Task t) {
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            IO.writeTask(new DataOutputStream(bs), t);
            byte[] tb = bs.toByteArray();
            return tb;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static Task taskFromBytes(@NotNull byte[] b, @NotNull TermIndex index) {
        try {
            return IO.readTask(new DataInputStream(new ByteArrayInputStream(b)), index);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /** serialization and deserialization of terms, tasks, etc. */
    public static class DefaultCodec extends FSTConfiguration {

        final TermIndex index;

        public DefaultCodec(TermIndex t) {
            super(null);

            this.index = t;

            createDefaultConfiguration();
            //setStreamCoderFactory(new FBinaryStreamCoderFactory(this));
            setForceSerializable(true);


            //setCrossPlatform(false);
            setShareReferences(false);
            setPreferSpeed(true);
            setCrossPlatform(false);




            registerClass(Atom.class, GenericCompound.class,
                    AbstractTask.class,
                    Term[].class,
                    TermContainer.class,
                    //long[].class, char.class,
                    Op.class);



            registerSerializer(AbstractTask.class, new FSTBasicObjectSerializer() {

                @Override
                public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
                }

                @NotNull
                @Override
                public Object instantiate(Class objectClass, @NotNull FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws IOException {
                    return readTask(in, index);
                }

                @Override
                public void writeObject(@NotNull FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
                    writeTask(out, (Task) toWrite);
                }
            }, true);


            registerSerializer(Atom.class, terms, true);
            registerSerializer(Atomic.class, terms, true);

            registerSerializer(GenericCompound.class, terms, true);

            registerSerializer(AtomConcept.class, terms, true);
            registerSerializer(CompoundConcept.class, terms, true);

        }

        @Nullable
        final FSTBasicObjectSerializer termContainers = new FSTBasicObjectSerializer() {

            @Override
            public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
            }

            @Nullable
            @Override
            public Object instantiate(Class objectClass, @NotNull FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws IOException {
                return readTermContainer(in, index);
            }

            @Override
            public void writeObject(@NotNull FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
                writeTermContainer(out, (TermContainer) toWrite);
            }
        };

        @Nullable
        final FSTBasicObjectSerializer terms = new FSTBasicObjectSerializer() {

            @Override
            public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
            }

            @Nullable
            @Override
            public Object instantiate(Class objectClass, @NotNull FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws IOException {
                return readTerm(in, index);
            }

            @Override
            public void writeObject(@NotNull FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
                writeTerm(out, (Term) toWrite);
            }
        };


    }


}
