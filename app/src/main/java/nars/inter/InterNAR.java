package nars.inter;


import com.google.common.collect.Lists;
import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.inter.gnutella.*;
import nars.nal.Tense;
import nars.nar.Default;
import nars.op.mental.Inperience;
import nars.task.MutableTask;
import nars.task.Task;
import nars.util.IO;
import org.apache.commons.lang3.tuple.Triple;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static nars.$.$;

/**
 * Peer interface for an InterNARS mesh
 * https://github.com/addthis/meshy/blob/master/src/test/java/com/addthis/
 */
public class InterNAR extends Peer implements ClientModel {

    final Logger logger;
    final NAR nar;

    public InterNAR(NAR n) throws IOException {
        super();

        logger = LoggerFactory.getLogger(n.self + "," + getClass().getSimpleName());

        this.nar = n;
    }







    @Override
    public void onQueryHit(Client client, QueryHitMessage q) {

        Task t = IO.taskFromBytes(q.result, nar.index);
        logger.info("{} told \"{}\" by {}", nar.self, t, q.responder);


        //if (q.id.equals(id)) //TODO dont reify if it's a message originating from this peer
        //TODO dont reify an already reified belief?

        nar.believe(
            Inperience.reify(t, $.quote(q.idString()), 0.75f), Tense.Present
        );


    }

    @Override
    public void data(Client client, String file, ByteBuffer b, int rangeByte) {

    }

    @Override
    public void search(Client client, QueryMessage message, Consumer<QueryHitMessage> eachResult) {

        Task t = IO.taskFromBytes(message.query, nar.index);
        logger.info("{} asked \"{}\" by {}", nar.self, t, message.recipient);

        if (t.isQuestion()) {
            final int[] count = {3};
            nar.ask(t.term(), Tense.ETERNAL, a -> {
                eachResult.accept(client.createQueryHit(message.idBytes(), 1, IO.asBytes(a)));
                return (count[0]--) > 0;
            });
        }

    }

    @Override
    public byte[] data(Client client, String file, int rangePosition) {
        return null;
    }

}
