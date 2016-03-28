package nars.task;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.nar.Default;
import nars.nar.Terminal;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import nars.term.index.PatternIndex;
import org.junit.Test;
import org.nustaq.serialization.*;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 3/28/16.
 */
public class TaskSerializationTest {

    //FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
    static final FSTConfiguration conf = FSTConfiguration.createUnsafeBinaryConfiguration();
    static {
        conf.registerSerializer(Atomic.class, new FSTBasicObjectSerializer() {

            @Override
            public boolean willHandleClass(Class cl) {
                return Atomic.class.isAssignableFrom(cl);
            }

            @Override public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception { }

            @Override
            public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
                //return new Atom(in.readStringUTF());
                String s = in.readStringUTF();
                if (s.startsWith("%")) {
                    //special case: pattern variables
                    System.out.println(s + " " + $.$(s));
                }
                return $.$(s);
            }

            @Override
            public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
                Atomic a = (Atomic)toWrite;
                out.writeStringUTF(a.toString());
            }
        }, true);

        conf.registerSerializer(GenericCompound.class, new FSTBasicObjectSerializer() {

            @Override
            public boolean willHandleClass(Class cl) {
                return GenericCompound.class.isAssignableFrom(cl);
            }

            @Override public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception { }

            @Override
            public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPosition) throws Exception {
                //GenericCompound c = $.$(in.readUTF());
                GenericCompound c = (GenericCompound) Narsese.the().term(in.readUTF(), null /* raw, unnormalized */);
                return c;
            }

            @Override
            public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
                GenericCompound a = (GenericCompound)toWrite;
                out.writeStringUTF(a.toString());
            }
        }, true);

        conf.setForceSerializable(true);

    }

    public static void testSerialize(Object orig) {
        byte barray[] = conf.asByteArray(orig);
        System.out.println(orig + "\n\tserialized: "+ barray.length + " bytes");

        Object copy = conf.asObject(barray);
        //System.out.println("\tbytes: " + Arrays.toString(barray));
        System.out.println("\tcopy: " + copy);

        assertTrue(copy!=orig);
        assertEquals(copy, orig);
        assertEquals(copy.getClass(), orig.getClass());
    }

    //    /* https://github.com/RuedigerMoeller/fast-serialization/wiki/Serialization*/
    @Test
    public void testRuleSerialization() {



        NAR nar = new Terminal(1024);

        testSerialize( nar.term("<a-->b>").term() /* term, not the concept */ );
        testSerialize( nar.task("<a-->b>.") );
        testSerialize( nar.task("<a-->(b==>c)>!") );


    }

}
