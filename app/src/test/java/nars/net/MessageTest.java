//package nars.net;
//
//import nars.IO;
//import nars.Narsese;
//import nars.Task;
//import nars.nar.Terminal;
//import nars.net.gnutella.message.QueryMessage;
//import org.junit.jupiter.api.Test;
//
//import java.io.IOException;
//
//
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Created by me on 7/10/16.
// */
//public class MessageTest {
//
//    static final Terminal t = new Terminal();
//    static InterNAR i;
//    static {
//        try {
//            InterNAR ii = new InterNAR(t);
//            i = ii;
//        } catch (IOException e) {
//            e.printStackTrace();
//            i = null;
//        }
//    }
//
//    @Test
//    public void testTaskSerializationInQuery() {
//
//        try {
//            testTaskRoundtrip("(a --> (b,c,d,e)).");
//            testTaskRoundtrip("(a --> b).");
//            testTaskRoundtrip("(a --> (b,c)).");
//        } catch (IOException e) {
//            assertTrue(false);
//        } catch (Narsese.NarseseException e) {
//            e.printStackTrace();
//        }
//
//    }
//
//    public void testTaskRoundtrip(String x) throws IOException, Narsese.NarseseException {
//        testTaskRoundtrip(t.task(x));
//    }
//    public void testTaskRoundtrip(Task x) throws IOException {
//
//        //test message as generated by sender
//        QueryMessage m = i.createQuery(IO.asBytes(x));
//        assertEquals(x, IO.taskFromBytes(m.query, t.concepts));
//
//        System.out.println(m);
//
////        //test message as generated by receiver
////        byte[] b = m.asBytes();
////        QueryMessage n = (QueryMessage)Message.nextMessage(
////                new DataInputStream(new ByteArrayInputStream(b)),
////                i.address);
////        assertEquals(x, IO.taskFromBytes(n.query, t.index));
//
//    }
//}
