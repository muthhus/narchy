package nars.util;

import nars.$;
import nars.Op;
import nars.Symbols;
import nars.concept.AtomConcept;
import nars.nal.Tense;
import nars.task.DerivedTask;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import nars.term.container.TermVector;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nustaq.serialization.*;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Created by me on 5/29/16.
 */
public class IO {


    static boolean hasTruth(char punc) {
        return punc == Symbols.BELIEF || punc == Symbols.GOAL;
    }


    @NotNull
    public static MutableTask readTask(@NotNull ObjectInput in) throws IOException, ClassNotFoundException {
        Term term = (Term) in.readObject();

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
    public static Truth readTruth(@NotNull ObjectInput in) throws IOException {
        Truth truth;
        float f = in.readFloat();
        float c = in.readFloat();
        truth = new DefaultTruth(f, c);
        return truth;
    }

    public static void writeTask(@NotNull ObjectOutput out, Task t) throws IOException {
        out.writeObject(t.term());

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

    public static void writeTruth(@NotNull ObjectOutput out, float freq, float conf) throws IOException {
        out.writeFloat(freq);
        out.writeFloat(conf);
    }

    public static void writeAtomic(@NotNull ObjectOutput out, Atomic a) throws IOException {
        out.writeUTF(a.toString());
    }


    @Nullable
    public static Atomic readAtomic(@NotNull ObjectInput in, Op o) throws IOException {
        String s = in.readUTF();
        return $.$(s);
    }



    static void writeTerm(ObjectOutput out, Term term) throws IOException {

        out.writeByte(term.op().ordinal());

        if (term instanceof Atomic)
            writeAtomic(out, (Atomic)term);
        else
            writeCompound(out, (Compound)term);
    }

    static void writeCompound(@NotNull ObjectOutput out, Compound a) throws IOException {
        //TODO include relation and dt if:
        //      --image
        //      --temporal (conj, equiv, impl)
        //  ...

        //how many subterms to follow
        int siz = a.size();
        out.writeByte(siz);
        for (int i = 0; i < siz; i++) {
            writeTerm(out, a.term(i));
        }

        if (a.op().isImage())
            out.writeByte(a.relation());
        else if (a.op().temporal)
            out.writeInt(a.dt());
    }

    /** TODO make a version which reads directlyinto TermIndex */
    @Nullable
    public static Compound readCompound(@NotNull ObjectInput in, Op o) throws IOException {


        int siz = in.readByte();
        Term[] s = new Term[siz];
        for (int i = 0; i < siz; i++) {
            s[i] = readTerm(in);
        }

        int relation = -1, dt = Tense.DTERNAL;
        if (o.isImage())
            relation = in.readByte();
        else if (o.temporal)
            dt = in.readInt();

        return (Compound) $.the(o, relation, dt, TermVector.the(s));
    }

    static Term readTerm(ObjectInput in) throws IOException {
        Op o = Op.values()[in.readByte()];
        if (o.isAtomic())
            return readAtomic(in, o);
        else
            return readCompound(in, o);
    }


    /** serialization and deserialization of terms, tasks, etc. */
    public static class TermCodec extends FSTConfiguration {

        public static final TermCodec the = new TermCodec();

        TermCodec() {
            super(null);

            createDefaultConfiguration();
            //setStreamCoderFactory(new FBinaryStreamCoderFactory(this));
            setForceSerializable(true);

            //setCrossPlatform(false);
            setShareReferences(true);



            registerClass(Atom.class, GenericCompound.class,
                    MutableTask.class, DerivedTask.class,
                    Term[].class,
                    //long[].class, char.class,
                    Op.class);



            registerSerializer(MutableTask.class, new FSTBasicObjectSerializer() {

                @Override
                public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
                }

                @NotNull
                @Override
                public Object instantiate(Class objectClass, @NotNull FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
                    return readTask(in);
                }

                @Override
                public void writeObject(@NotNull FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
                    writeTask(out, (Task) toWrite);
                }
            }, true);


            registerSerializer(Atom.class, TermSerializer.the, true);
            registerSerializer(Atomic.class, TermSerializer.the, true);
            registerSerializer(AtomConcept.class, TermSerializer.the, true);
            registerSerializer(GenericCompound.class, TermSerializer.the, true);


        }

        private static class TermSerializer extends FSTBasicObjectSerializer {

            public static FSTObjectSerializer the = new TermSerializer();

            private TermSerializer() {

            }

            @Override
            public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
            }

            @Nullable
            @Override
            public Object instantiate(Class objectClass, @NotNull FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
                return readTerm(in);
            }

            @Override
            public void writeObject(@NotNull FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
                writeTerm(out, (Term) toWrite);
            }
        }


    }


}
