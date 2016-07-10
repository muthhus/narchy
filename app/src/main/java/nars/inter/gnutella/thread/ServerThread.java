package nars.inter.gnutella.thread;

import nars.inter.gnutella.GnutellaConstants;
import nars.inter.gnutella.message.Message;
import nars.inter.gnutella.Peer;
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

    public ServerThread(Socket socket, Peer peer) throws IOException {
        super(socket, peer);
    }

    @Override
    public void run() {

        InetSocketAddress remote = (InetSocketAddress) socket.getRemoteSocketAddress();

        logger.info("{} connect {}", peer, remote);

        peer.neighbors.putIfAbsent(remote, this);

        while (working) {

            Message m;
            try {

                m = Message.nextMessage(inStream, remote);
                //logger.info("{} recv {}", peer, m);
                if (m == null)
                    continue;

                //logger.trace("recv {}", m);

                flag = true;
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
                        pending(m);
                        //}
                        break;

                }

            } catch (IOException e) {
                logger.error("{} recv {}", remote, e);
                break;
            }




        /*} catch (IOException e) {
            System.err.println(getClass() + "run(): " + e.getClass()
                    + e.getMessage());
        }*/
        }

        stop();

    }


}
