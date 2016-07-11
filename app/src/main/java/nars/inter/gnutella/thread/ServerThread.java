package nars.inter.gnutella.thread;

import com.google.common.io.ByteStreams;
import nars.bag.impl.ArrayBag;
import nars.budget.Budget;
import nars.budget.UnitBudget;
import nars.budget.merge.BudgetMerge;
import nars.inter.gnutella.GnutellaConstants;
import nars.inter.gnutella.message.Message;
import nars.inter.gnutella.Peer;
import nars.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by me on 7/9/16.
 */
public class ServerThread extends PeerThread {

    public static final Logger logger = LoggerFactory.getLogger(ServerThread.class);
    private Thread sendThread;


    /** messages per second (hz) */
    float maxSendFrequency = 16;
    float maxRecvFrequency = 24;
    final int messageBagSize = 64;

    final ArrayBag<Message> outgoing = new ArrayBag(messageBagSize, BudgetMerge.max);



    public ServerThread(Socket socket, Peer peer) throws IOException {
        super(socket, peer);
    }

    @Override
    public void run() {

        InetSocketAddress remote = (InetSocketAddress) socket.getRemoteSocketAddress();

        logger.info("{} connect {}", peer, remote);

        peer.neighbors.putIfAbsent(remote, this);

        sendThread = new Thread(this::send);
        sendThread.start();


        while (working) {

            Message m;
            try {

                byte type = inStream.readByte();
                byte len1 = inStream.readByte(); //lower 8 bits
                byte len2 = inStream.readByte(); //upper 8 bits
                int len = (len2 << 8 ) | len1;
                byte[] buffer = new byte[len - 3];
                int s = in.read(buffer);
                if (s != buffer.length) {
                    logger.warn("socket underflow");
                    continue;
                }

                m = Message.nextMessage(type, ByteStreams.newDataInput(buffer), remote);
                logger.info("{} recv {}", peer, m);
                if (m == null)
                    continue;



                //logger.trace("recv {}", m);

                switch (m.type) {

                    case GnutellaConstants.PING:
                        if (!peer.seen(m)) {
                            pending(m);
                        }

                        break;

                    case GnutellaConstants.PONG:
                        if (!peer.seen(m)) {
                            pending(m);
                        }
                        break;

                    case GnutellaConstants.PUSH:
                        break;

                    case GnutellaConstants.QUERY:
                        if (!peer.seen(m)) {
                            pending(m);
                        }
                        break;

                    case GnutellaConstants.QUERY_HIT:
                        //if (/*mine(m) || */unseen(m)) {
                        //pending(m);
                        //}
                        break;

                    default:
                        logger.error("unknown message type {} ", m.type);
                        break;
                }

            } catch (IOException e) {
                logger.error("{} error {}", remote, e);
                break;
            }

            Util.pause((long)(1000f/(maxRecvFrequency)));


        }

        stop();

    }

    protected void send() {

        while (working) {
            try {
                if (!outgoing.isEmpty()) {
                    Message top;
                    outgoing.commit();
                    synchronized (outgoing) {
                        top = outgoing.removeItem(0).get();
                    }
                    _send(top);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            Util.pause((long) (1000f / (maxSendFrequency)));
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /* METHODS USED IF NOT DOWNLOADTHREAD */

    public void send(Message m, Budget b) {
        synchronized (outgoing) {
            outgoing.put(m, b);
        }
//        if (outgoing.isFull()) {
//            logger.warn("output ")
//        }
    }

    /**
     * Adds the Message to the queue of pending messages to send
     *
     * @param m the Message
     */
    public void send(Message m) {
        //logger.trace("send {}", m);
        outgoing.put(m, UnitBudget.Zero);
    }

    @Override
    public void stop() {
        super.stop();

        sendThread.interrupt();

        outgoing.clear();

    }
}
