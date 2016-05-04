package nars.util;

import nars.$;
import nars.Op;
import nars.Symbols;
import nars.concept.AtomConcept;
import nars.task.DerivedTask;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nustaq.serialization.*;

import java.io.IOException;


/** serialization and deserialization of terms, tasks, etc. */
public class TermCodec extends FSTConfiguration {

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


//        registerSerializer(DefaultTruth.class, new FSTBasicObjectSerializer() {
//
////            @Override
////            public boolean willHandleClass(Class cl) {
////                return Task.class.isAssignableFrom(cl);
////            }
//
//            @Override
//            public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
//            }
//
//            @Override
//            public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
//                //return new Atom(in.readStringUTF());
//                float f = in.readFloat();
//                float c = in.readFloat();
//                return new DefaultTruth(f, c);
//            }
//
//            @Override
//            public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
//                Truth t = (Truth) toWrite;
//                out.writeFloat(t.freq());
//                out.writeFloat(t.conf());
//            }
//        }, false);


        registerSerializer(MutableTask.class, new FSTBasicObjectSerializer() {

//            @Override
//            public boolean willHandleClass(Class cl) {
//                return Task.class.isAssignableFrom(cl);
//            }

            @Override
            public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
            }

            @NotNull
            @Override
            public Object instantiate(Class objectClass, @NotNull FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
                //return new Atom(in.readStringUTF());

                Term term = (Term) in.readObject();
                char punc = (char) in.readByte();

                Truth truth;
                if (hasTruth(punc)) {
                    float[] fc = new float[2];
                    in.getCodec().readFPrimitiveArray(fc, float.class, 2);
                    truth = new DefaultTruth(fc);
                } else {
                    truth = null;
                }

                long occ = in.readLong();
                long cre = in.readLong();


                int eviLength = in.readByte(); //in.readByte(); //UnsignedByte();
                long[] evi = new long[eviLength];
                //for (int i = 0; i < eviLength; i++)
                    //evi[i] = in.readLong();
                in.getCodec().readFPrimitiveArray(evi, long.class, eviLength);

                float pri = in.readFloat();
                float dur = in.readFloat();
                float qua = in.readFloat();

                MutableTask mm = new MutableTask(term, punc, truth).time(cre, occ);
                mm.evidence(evi);
                mm.budget(pri, dur, qua);


                return mm;
            }

            public boolean hasTruth(char punc) {
                return punc == Symbols.BELIEF || punc == Symbols.GOAL;
            }

            @Override
            public void writeObject(@NotNull FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
                Task t = (Task) toWrite;
                out.writeObject(t.term());
                char p = t.punc();
                out.writeByte(p);
                if (hasTruth(p)) {
                    out.getCodec().writePrimitiveArray(new float[] { t.freq(), t.conf() }, 0, 2);
                    //out.writeFloat(t.freq()); out.writeFloat(t.conf());
                }
                out.writeLong(t.occurrence());
                out.writeLong(t.creation());


                long[] evi = t.evidence();
                int evil;
                evil = evi == null ? 0 : evi.length;

                out.writeByte(evil); //out.writeByte((byte) evil);
                //for (int i = 0; i < evil; i++)
                    //out.writeLong(evi[i]);
                out.getCodec().writePrimitiveArray(evi, 0, evil);

                out.writeFloat(t.pri());
                out.writeFloat(t.dur());
                out.writeFloat(t.qua());
            }
        }, true);


        registerSerializer(Atom.class, AtomicTermSerializer.the, true);
        registerSerializer(Atomic.class, AtomicTermSerializer.the, true);
        registerSerializer(AtomConcept.class, AtomicTermSerializer.the, true);


        registerSerializer(GenericCompound.class, new FSTBasicObjectSerializer() {

//            @Override
//            public boolean willHandleClass(Class cl) {
//                return GenericCompound.class.isAssignableFrom(cl);
//            }

            @Override
            public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
            }

            @Nullable
            @Override
            public Object instantiate(Class objectClass, @NotNull FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
                //GenericCompound c = (GenericCompound) Narsese.the().term(in.readUTF(), null /* raw, unnormalized */);

                Op o = Op.values()[in.readByte()];
                //int subs = in.readByte();
                Term[] s = (Term[]) in.readObject();
//                Term[] s = new Term[subs];
//
//                for (int i = 0; i < subs; i++) {
//                    s[i] = (Term) in.readObject();
//                }

                return $.the(o, s);
            }

            @Override
            public void writeObject(@NotNull FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
                GenericCompound a = (GenericCompound) toWrite;
                //out.writeStringUTF(a.toString());

                out.writeByte(a.op().ordinal());
                //int subs = a.size();
                //out.writeByte(subs); //how many subterms to follow

                //TODO include relation and dt if:
                //      --image
                //      --temporal (conj, equiv, impl)
                //  ...
                out.writeObject(a.terms());
                /*for (int i = 0; i < subs; i++) {
                    out.writeObject(a.term(i).term());
                }*/
            }
        }, true);


    }

    private static class AtomicTermSerializer extends FSTBasicObjectSerializer {
        public static final AtomicTermSerializer the = new AtomicTermSerializer();

        private AtomicTermSerializer() {

        }

        @Override
        public boolean willHandleClass(@NotNull Class cl) {

            return Atomic.class.isAssignableFrom(cl);
        }

        @Override
        public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
        }

        @Nullable
        @Override
        public Object instantiate(Class objectClass, @NotNull FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
            //return new Atom(in.readStringUTF());
            String s = in.readStringUTF();
//                if (s.startsWith("%")) {
//                    //special case: pattern variables
//                    System.out.println(s + " " + $.$(s));
//                }
            return $.$(s);
        }

        @Override
        public void writeObject(@NotNull FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
            Atomic a = (Atomic) toWrite;
            out.writeStringUTF(a.toString());
        }
    }
}
