package nars.inter;


import nars.$;
import nars.NAR;
import nars.bag.impl.ArrayBag;
import nars.budget.merge.BudgetMerge;
import nars.inter.gnutella.*;
import nars.inter.gnutella.message.Message;
import nars.inter.gnutella.message.QueryHitMessage;
import nars.inter.gnutella.message.QueryMessage;
import nars.link.BLink;
import nars.nal.Tense;
import nars.op.mental.Inperience;
import nars.task.Task;
import nars.term.Term;
import nars.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * Peer interface for an InterNARS mesh
 * https://github.com/addthis/meshy/blob/master/src/test/java/com/addthis/
 */
public class InterNAR extends Peer implements PeerModel {

    final Logger logger;
    final NAR nar;

    float broadcastPriorityThreshold = 0.75f;
    float broadcastConfidenceThreshold = 0.9f;

    final ArrayBag<Term> asked = new ArrayBag(64, BudgetMerge.plusDQBlend);

    public InterNAR(NAR n) throws IOException {
        super();

        logger = LoggerFactory.getLogger(n.self + "," + getClass().getSimpleName());

        this.nar = n;

        nar.onTask(t -> {
            if (t.isQuestion()) {
                BLink<Term> existingBudget = asked.get(t.term());
                if (existingBudget == null /* || or below a decayed threshold */ ) {
                    nar.runLater(()->{
                        //broadcast question as internar query
                        asked.put(t.term(), t.budget());

                        logger.info("{} asks \"{}\"", address, t);
                        query(IO.asBytes(t));

                    });
                }
            } else if (t.isBelief()) {
                if (t.pri() >= broadcastPriorityThreshold && t.conf() >= broadcastConfidenceThreshold) {
                    query(IO.asBytes(t));
                }
            }
        });
    }



    @Override
    public void onQueryHit(Peer client, QueryHitMessage q) {

        Task t = IO.taskFromBytes(q.result, nar.index);
        logger.info("{} told \"{}\" by {}", nar.self, t, q.responder);


        //if (q.id.equals(id)) //TODO dont reify if it's a message originating from this peer
        //TODO dont reify an already reified belief?

        consider(q, t);


    }

    public void consider(Message q, Task t) {
        nar.believe(
            Inperience.reify(t, $.quote(q.idString()), 0.75f), Tense.Present
        );
    }

    @Override
    public void data(Peer client, String file, ByteBuffer b, int rangeByte) {

    }

    @Override
    public void search(Peer client, QueryMessage q, Consumer<QueryHitMessage> eachResult) {

        Task t = IO.taskFromBytes(q.query, nar.index);
        logger.info("{} asked \"{}\" from {}", address, t, q.recipient);

        if (t.isQuestion()) {
            final int[] count = {3};
            nar.ask(t.term(), Tense.ETERNAL, a -> {
                logger.info("{} answering \"{}\" to {}", address, a, q.recipient);
                eachResult.accept(client.createQueryHit(q.recipient, q.idBytes(), 1, IO.asBytes(a)));
                return (count[0]--) > 0;
            });
        } else if (t.isBelief()) {
            consider(q, t);
        }

    }

    @Override
    public byte[] data(Peer client, String file, int rangePosition) {
        return null;
    }

}
