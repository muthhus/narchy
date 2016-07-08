package nars.inter;


import nars.$;
import nars.NAR;
import nars.nar.Default;
import nars.task.Task;
import nars.util.IO;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Arrays;

/**
 * Peer interface for an InterNARS mesh
 * https://github.com/addthis/meshy/blob/master/src/test/java/com/addthis/
 */
public class InterNAR  {


    final Logger logger;

    final NAR nar;


    public InterNAR(NAR n, int port) throws IOException {

        logger = LoggerFactory.getLogger(n.self + "," + getClass().getSimpleName());

        this.nar = n;



    }




    public static void main(String[] args) throws IOException, InterruptedException {
        //test

        NAR a = new Default();
        InterNAR ai = new InterNAR(a, 10001);

        NAR b = new Default();
        InterNAR bi = new InterNAR(a, 10001);
        //bi.connectPeer(ai);



        ai.send(a.task("(a-->b)."));

    }

    public void send(Task t) {


        DataOutput d = new DataOutputStream(null);
        try {
            IO.writeTask(d, t);
            //o.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }



    }

}
