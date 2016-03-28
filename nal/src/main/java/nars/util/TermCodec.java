package nars.util;

import com.sun.tools.classfile.ConstantPool;
import nars.$;
import nars.Narsese;
import nars.Symbols;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.nustaq.serialization.*;

import java.io.IOException;


/** serialization and deserialization of terms, tasks, etc. */
public class TermCodec extends FSTConfiguration {

    public static final TermCodec the = new TermCodec();

    TermCodec() {
        super(null);

        createDefaultConfiguration();
        //setStreamCoderFactory(new FBinaryStreamCoderFactory(this));



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

            @Override
            public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
                //return new Atom(in.readStringUTF());

                Term term = (Term) in.readObject();
                char punc = in.readChar();

                Truth truth;
                if (punc != Symbols.QUESTION && punc != Symbols.QUEST) {
                    truth = new DefaultTruth(in.readFloat() /* f */, in.readFloat() /* c */);
                } else {
                    truth = null;
                }

                long occ = in.readLong();
                long cre = in.readLong();


                int eviLength = in.readInt(); //in.readByte(); //UnsignedByte();
                long[] evi = new long[eviLength];
                for (int i = 0; i < eviLength; i++)
                    evi[i] = in.readLong();

                float pri = in.readFloat();
                float dur = in.readFloat();
                float qua = in.readFloat();

                MutableTask mm = new MutableTask(term, punc).truth(truth).time(cre, occ);
                mm.evidence(evi);
                mm.budget(pri, dur, qua);


                return mm;
            }

            @Override
            public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
                Task t = (Task) toWrite;
                out.writeObject(t.term());
                out.writeChar(t.punc());
                if (!t.isQuestOrQuestion()) {
                    //out.writeObject(t.truth());
                    out.writeFloat(t.freq());
                    out.writeFloat(t.conf());
                }
                out.writeLong(t.occurrence());
                out.writeLong(t.creation());


                long[] evi = t.evidence();
                int evil;
                evil = evi == null ? 0 : evi.length;

                out.writeInt(evil); //out.writeByte((byte) evil);
                for (int i = 0; i < evil; i++)
                    out.writeLong(evi[i]);


                out.writeFloat(t.pri());
                out.writeFloat(t.dur());
                out.writeFloat(t.qua());
            }
        }, true);

        registerSerializer(Atomic.class, new FSTBasicObjectSerializer() {

            @Override
            public boolean willHandleClass(Class cl) {
                return Atomic.class.isAssignableFrom(cl);
            }

            @Override
            public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
            }

            @Override
            public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
                //return new Atom(in.readStringUTF());
                String s = in.readStringUTF();
//                if (s.startsWith("%")) {
//                    //special case: pattern variables
//                    System.out.println(s + " " + $.$(s));
//                }
                return $.$(s);
            }

            @Override
            public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
                Atomic a = (Atomic) toWrite;
                out.writeStringUTF(a.toString());
            }
        }, true);

        registerSerializer(GenericCompound.class, new FSTBasicObjectSerializer() {

//            @Override
//            public boolean willHandleClass(Class cl) {
//                return GenericCompound.class.isAssignableFrom(cl);
//            }

            @Override
            public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
            }

            @Override
            public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
                //GenericCompound c = $.$(in.readUTF());
                GenericCompound c = (GenericCompound) Narsese.the().term(in.readUTF(), null /* raw, unnormalized */);
                return c;
            }

            @Override
            public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
                GenericCompound a = (GenericCompound) toWrite;
                out.writeStringUTF(a.toString());
            }
        }, true);

        //setForceSerializable(true);


    }
}
