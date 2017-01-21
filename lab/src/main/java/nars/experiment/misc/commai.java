//package nars.experiment.misc;
//
//import nars.$;
//import nars.NAR;
//import nars.concept.SensorConcept;
//import nars.remote.NAgents;
//import org.zeromq.ZContext;
//import org.zeromq.ZMQ;
//
//import java.util.Arrays;
//
///**
// * https://github.com/facebookresearch/CommAI-env
// * python3 run.py tasks_config.json -l learners.base.RemoteLearner --learner-cmd "echo" -v ConsoleView
// * <p>
// * https://github.com/zeromq/jeromq
// */
//public class commai extends NAgents {
//
//
//    //static final byte[] periodChar = {0, 0, 1, 0, 1, 1, 1, 0};  // '.' utf-8 code
//    private final ZMQ.Socket client;
//    private final ZContext ctx;
//
//    boolean ready = false;
//
//    ///** next output bit */
//    //public boolean out = false;
//    //public boolean input0, input1, input2, input3, input4, input5, input6, input7;
//
//
//    boolean out[] = new boolean[8], in[] = new boolean[8];
//
//    /*
//    #### Defining a learner in programming language X
//
//    When the session is created, the environment and learner are initialized:
//    - The learner begins by sending a handshake 'hello' to the environment.
//    - Loop: accept reward, accept environment bit, send reply bit.
//    */
//    public commai(NAR nar) {
//        super("commai", nar, 1);
//
//        ctx = new ZContext();
//        client = ctx.createSocket(ZMQ.PAIR);
//        client.connect("tcp://localhost:5556");
//
//
//        nar.log();
//
////        new NObj("c", this, nar).read(
////                "input0",
////                "input1",
////                "input2",
////                "input3",
////                "input4",
////                "input5",
////                "input6",
////                "input7"
////        ).into(this);
//
//        for (int i = 0; i < 8; i++) {
//            int ii = i;
//
//            actionToggle("(o," + i + ")", () -> out[ii] = true, () -> out[ii] = false);
//            sense(new SensorConcept("(i," + i + ')', nar, () -> in[ii] ? 1f : 0f, (f) -> $.t(f, alpha)));
//        }
//
//
//        //handshake
//        client.send("hello");
//
//        ready = true;
////        boolean running = true;
////        while (running) {
////
////
////        }
////
////        client.close();
////        ctx.destroy();
//    }
//
//    @Override
//    protected float act() {
//
//        if (!ready)
//            return 0f;
//
//        int reward = 0;
//
//        char outC = 0;
//        for (int b = 0; b < 8; b++) {
//            outC |= out[b] ? 1 << b : 0;
//        }
//        if (outC < 32 || outC >= 127) {
//            outC = ' ';
//            for (int b = 0; b < 8; b++) {
//                out[b] = (outC & (1 << b)) > 0 ? true : false;
//            }
//        }
//
//        for (int b = 0; b < 8; b++) {
//            byte rewardB = client.recv()[0];
//            if (rewardB > 0)
//                reward++;
//            byte ii = (byte)(client.recv()[0] - '0');
//            in[7-b] = ii > 0 ? true : false;
//
//            client.send(new byte[]{out[7-b] ? (byte)'1' : (byte)'0'});
//        }
//
//        char inC = 0;
//        for (int b = 0; b < 8; b++) {
//            inC |= in[b] ? 1 << b : 0;
//        }
//        io(inC, outC);
//
//
//        System.out.println(inC + " "+  outC + "=" + Arrays.toString(out) + " " + reward);
//
//        return reward;
//    }
//
//    private void io(char inC, char outC) {
//        nar.input("i:\"" + inC + "\". :|:");
//        nar.input("o:\"" + outC + "\". :|:");
//    }
//
//
//    public static void main(String[] args) {
////        Default d = new Default();
////        new commai(d).run(50000, 100);
//
//        runRT(commai::new, 10);
//
//    }
//
//}
