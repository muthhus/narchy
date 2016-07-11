package nars.inter;


import nars.$;
import nars.NAR;
import nars.bag.impl.ArrayBag;
import nars.budget.merge.BudgetMerge;
import nars.inter.gnutella.Peer;
import nars.inter.gnutella.PeerModel;
import nars.inter.gnutella.message.Message;
import nars.inter.gnutella.message.QueryMessage;
import nars.link.BLink;
import nars.nal.Tense;
import nars.op.mental.Inperience;
import nars.task.Task;
import nars.term.Term;
import nars.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * Peer interface for an InterNARS mesh
 * https://github.com/addthis/meshy/blob/master/src/test/java/com/addthis/
 */
public class InterNAR extends Peer implements PeerModel {

    final Logger logger;
    final NAR nar;

    public float broadcastPriorityThreshold = 0.5f;
    public float broadcastConfidenceThreshold = 0.9f;

    final ArrayBag<Term> asked = new ArrayBag(64, BudgetMerge.plusDQBlend);
    private boolean paranoid = false;

    public InterNAR(NAR n) throws IOException {
        this(n, (short)-1);
    }

    public InterNAR(NAR n, short port) throws IOException {
        super(port);

        logger = LoggerFactory.getLogger(n.self + "," + getClass().getSimpleName());

        this.nar = n;

        nar.onTask(t -> {
            if (!wasReceived(t)) {
//                if (t.isQuestion()) {
//                    BLink<Term> existingBudget = asked.get(t.term());
//                    if (existingBudget == null /* || or below a decayed threshold */) {
//                        nar.runLater(() -> {
//                            //broadcast question as internar query
//                            asked.put(t.term(), t.budget());
//
//                            logger.info("{} asks \"{}\"", address, t);
//                            query(t);
//
//                        });
//                    }
//                } else if (t.isBeliefOrGoal()) {
                    if (t.pri() >= broadcastPriorityThreshold && (t.isBeliefOrGoal() && t.conf() >= broadcastConfidenceThreshold) || (t.isQuestOrQuestion())) {
                        query(t);
                    }
//                }
            }
        });
    }

    @Override
    public boolean onQuery(QueryMessage q) {
        if (super.onQuery(q)) {

            try {
                Task t = IO.taskFromBytes(q.query, nar.index);
                //logger.info("recv query {} \t {}", q, t);
                consider(q, t);
            } catch (Exception e) {
                logger.error("Malformed task: bytes={}", q.queryString());
                e.printStackTrace();
            }

            return true;
        }

        return false;

    }

    public void query(Task t) {
        nar.runLater(()->{
            query(IO.asBytes(t), t.budget());
        });
    }


//    @Override
//    public void onQueryHit(Peer client, QueryHitMessage q) {
//
//        Task t = IO.taskFromBytes(q.result, nar.index);
//        logger.info("{} told \"{}\" by {}", nar.self, t, q.responder);
//
//
//        //if (q.id.equals(id)) //TODO dont reify if it's a message originating from this peer
//        //TODO dont reify an already reified belief?
//
//        consider(q, t);
//
//
//    }

    public boolean wasReceived(Task x) {
        List l = x.log();
        if (l == null)
            return false;
        for (Object o : l) {
            if (o instanceof Message)
                return true;
        }
        return false;
    }

    public void consider(Message q, Task t) {

        t.log(q);

        if (paranoid) {
            nar.believe(
                    Inperience.reify(t, $.quote(q.idString()), 0.75f), Tense.Present
            );
        } else {
            nar.input(t);
        }
    }

    @Override
    public void data(Peer client, String file, ByteBuffer b, int rangeByte) {

    }

//    @Override
//    public void search(Peer client, QueryMessage q, Consumer<QueryHitMessage> eachResult) {
//
//        Task t = IO.taskFromBytes(q.query, nar.index);
//        logger.info("{} asked \"{}\" from {}", address, t, q.origin);
//
//        if (t.isQuestion()) {
//            final int[] count = {3};
//            nar.ask(t.term(), Tense.ETERNAL, a -> {
//                logger.info("{} answering \"{}\" to {}", address, a, q.origin);
//                eachResult.accept(client.createQueryHit(q.origin, q.idBytes(), 1, IO.asBytes(a)));
//                return (count[0]--) > 0;
//            });
//        } else if (t.isBelief()) {
//            consider(q, t);
//        }
//
//    }

    @Override
    public byte[] data(Peer client, String file, int rangePosition) {
        return null;
    }

}
