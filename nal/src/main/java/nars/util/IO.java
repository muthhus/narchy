package nars.util;

import nars.$;
import nars.Op;
import nars.Symbols;
import nars.concept.AtomConcept;
import nars.concept.CompoundConcept;
import nars.index.TermIndex;
import nars.nal.Tense;
import nars.task.AbstractTask;
import nars.task.DerivedTask;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermSet;
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
    public static MutableTask readTask(@NotNull ObjectInput in, TermIndex t) throws IOException, ClassNotFoundException {
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

    public static void writeTask(@NotNull ObjectOutput out, @NotNull Task t) throws IOException {
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

    public static void writeAtomic(@NotNull ObjectOutput out, @NotNull Atomic a) throws IOException {
        out.writeUTF(a.toString());
    }


    @Nullable
    public static Atomic readAtomic(@NotNull ObjectInput in, Op o, @NotNull TermIndex t) throws IOException {
        String s = in.readUTF();
        return t.the(s);
    }



    static void writeTerm(@NotNull ObjectOutput out, @NotNull Term term) throws IOException {

        out.writeByte(term.op().ordinal());

        if (term instanceof Atomic)
            writeAtomic(out, (Atomic)term);
        else
            writeCompound(out, (Compound)term);
    }

    static void writeCompound(@NotNull ObjectOutput out, @NotNull Compound c) throws IOException {

        //how many subterms to follow
        writeTermContainer(out, c.subterms());

        if (c.op().isImage())
            out.writeByte(c.relation());
        else if (c.op().temporal)
            out.writeInt(c.dt());
    }

    static void writeTermContainer(@NotNull ObjectOutput out, @NotNull TermContainer c) throws IOException {
        int siz = c.size();
        out.writeByte(siz);
        for (int i = 0; i < siz; i++) {
            writeTerm(out, c.term(i));
        }
    }
    @Nullable
    public static TermContainer readTermContainer(@NotNull ObjectInput in, @NotNull TermIndex t) throws IOException {
        int siz = in.readByte();
        Term[] s = new Term[siz];
        for (int i = 0; i < siz; i++) {
            s[i] = readTerm(in, t);
        }

        return TermVector.the(s);
    }

    /**
     * called by readTerm after determining the op type
     * TODO make a version which reads directlyinto TermIndex */
    @Nullable
    static Compound readCompound(@NotNull ObjectInput in, @NotNull Op o, @NotNull TermIndex t) throws IOException {

        TermContainer v = readTermContainer(in, t);

        int relation = -1, dt = Tense.DTERNAL;
        if (o.isImage())
            relation = in.readByte();
        else if (o.temporal)
            dt = in.readInt();


        return (Compound) t.normalized(t.builder().build(o, relation, dt, v));
    }

    /**
     * called by readTerm after determining the op type */
    @Nullable
    static Term readTerm(@NotNull ObjectInput in, @NotNull TermIndex t) throws IOException {
        Op o = Op.values()[in.readByte()];
        if (o.isAtomic())
            return readAtomic(in, o, t);
        else
            return readCompound(in, o, t);
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
                public Object instantiate(Class objectClass, @NotNull FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
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
            public Object instantiate(Class objectClass, @NotNull FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
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
            public Object instantiate(Class objectClass, @NotNull FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
                return readTerm(in, index);
            }

            @Override
            public void writeObject(@NotNull FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
                writeTerm(out, (Term) toWrite);
            }
        };


    }


}
