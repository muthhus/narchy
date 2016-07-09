package nars.inter.gnutella;

import nars.inter.gnutella.message.*;
import nars.inter.gnutella.thread.DownloadThread;
import nars.inter.gnutella.thread.PeerThread;
import nars.inter.gnutella.thread.ServerThread;
import nars.util.Util;
import nars.util.data.map.CapacityLinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Client of Gnutella network. Protocol v0.4 Features: +Connect to the network
 * +Share files +Answer to querys +Search and download. For downloads uses
 * protocol HTTP
 *
 * @author Ismael Fernandez
 * @author Miguel Vilchis
 * @version 2.0
 * @see http://www.stanford.edu/class/cs244b/gnutella_protocol_0.4.pdf
 * @see https
 * ://www.dropbox.com/sh/yyzy5e48qk7p5h0/EtzNfXlzAO/comp-dist-unam-proyecto
 * -diapos.pdf
 */

public class Peer {

    final static Logger logger = LoggerFactory.getLogger(Peer.class);


    public final InetAddress host;
    public final short port;
    public final Server server;
    final ExecutorService exe =
            //Executors.newFixedThreadPool(8);
//            new ThreadPoolExecutor(0, Integer.MAX_VALUE,
//                                      1L, TimeUnit.SECONDS,
//                                      new SynchronousQueue<Runnable>());
            //Executors.newFixedThreadPool(4);
            Executors.newCachedThreadPool();

    final int maxConnections = 5;

    public final Map<String, Message> messageCache = new CapacityLinkedHashMap(4096);
    public final InetSocketAddress address;
    private final ConcurrentHashMap<String, Collection<InetSocketAddress>> firstPongsFromNeighbors;
    public final PeerModel model;


    boolean running;
    public final ConcurrentHashMap<SocketAddress, PeerThread> neighbors;


    private final ExecutorService pendingMessages = Executors.newSingleThreadExecutor();


    @Deprecated private int numberFileShared;
    private int numberKbShared;

    private int maxNodes;
    private byte[] id = new byte[16]; //unused?

    /**
     * this instance must implement ClientModel
     */
    protected Peer() throws IOException {
        this(null);
    }

    public Peer(PeerModel model) throws IOException {
        this(newRandomPort(), model);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + address.toString() + ",port=" + port + "]";
    }

    /**
     * Constructs a Servent that listens for upcoming connections in the
     * specified port, and shares files of the especified directoryPath
     *
     * @param pathName Directory Path for sharinf files
     * @throws IOException IO error when opening the socket in which this Servent
     *                     listens for upcoming connections
     */
    public Peer(short port, PeerModel model) throws IOException {

        host = InetAddress.getLocalHost();
        address = new InetSocketAddress(host, port);
        this.port = port;
        if (model == null)
            this.model = (PeerModel) this;
        else
            this.model = model;

        neighbors = new ConcurrentHashMap<>(maxConnections);
        firstPongsFromNeighbors = new ConcurrentHashMap<>();




        running = true;

        numberFileShared = 0;
        numberKbShared = 0;

        maxNodes = 10;

        this.server = new Server(this, this.port);

        exe.execute(server);

        exe.execute(() -> {
            while (running) {

                Util.pause(GnutellaConstants.DEAD_CONNECTION_REMOVAL_INTERVAL_MS);

                removeDeadConnections();

            }
        });

        logger.info("started {}", server.socket);
    }

    public void connect(Peer p) {
        if (p == this)
            throw new RuntimeException("self connect");
        connect(p.host.getHostAddress(), p.port());
    }


    /**
     * Connects to a Gnutella client with the specified ip and port
     *
     * @param ip   The ip in string format of the Gnutella client
     * @param port The port in which the Gnutella client listens
     * @return true if the connection is made succesfully, false otherwise
     */
    public boolean connect(String ip, short port) {
        if (neighbors.size() > getMaxNodes()) {
            setMaxNodes();
        }
        if (neighbors.size() <= maxNodes) {
            PeerThread node;
            try {
                Socket sktTmp = new Socket(InetAddress.getByName(ip), port);
                InetSocketAddress inetSocketA = new InetSocketAddress(
                        sktTmp.getInetAddress(), sktTmp.getLocalPort());
                node = new ServerThread(sktTmp, this);

                if (node.connexionRequest() == GnutellaConstants.ACCEPTED) {

                    neighbors.computeIfAbsent(inetSocketA, (i) -> {
                        exe.execute(node);
                        return node;
                    });

                    return true;
                }
            } catch (IOException e) {

                e.printStackTrace();
            }
            System.err.println("DENIED CONNECCTION");

            return false;
        }

        return false;
    }

    /**
     * Used to actively discover hosts on the network
     */
    public void ping() {
        addPing();
    }

    /**
     * The primary mechanism for searching the distributed network.
     *
     * @param searchCriteria the name of file for the search
     */
    public void query(String searchCriteria) {
        query(GnutellaConstants.DFLTMIN_SPEED, searchCriteria);
    }

    public void query(byte[] searchCriteria) {
        query(GnutellaConstants.DFLTMIN_SPEED, searchCriteria);

    }

    /**
     * Establishes a connection to download the specified file from a specified
     * Server
     *
     * @param ip   The ip in string format of the Server
     * @param port The port in which the Server listens for upcoming connections
     * @param file Name of the file that for searching
     * @param size Size of the file for the search
     */
    public void download(String ip, short port, String file, int size) throws UnknownHostException {
        download(InetAddress.getByName(ip), port, file, size, 0);
    }


//	/**
//	 * List the current neighbors
//	 */
//	public void neighbors() {
//
////		Enumeration<InetSocketAddress> e = neighbors.keys();
////		System.out.println("\n NEIGHBORS AT THE MOMENT:  \n");
////		System.out.print("( ");
////		while (e.hasMoreElements()) {
////			InetSocketAddress idNodeNext = e.nextElement();
////			System.out.print(idNodeNext.getAddress() + "-"
////					+ idNodeNext.getPort() + ", ");
////		}
////		System.out.println(")");
//
//	}

    /**
     * Close all connections
     */
    public void stop() {

        pendingMessages.shutdownNow();
        running = false;


        logger.info("stop");

        Enumeration<SocketAddress> n = neighbors.keys();
        while (n.hasMoreElements()) {
            SocketAddress idN = n.nextElement();
            neighbors.remove(idN).stop();
        }

        try {
            server.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


//        try {
//            exe.awaitTermination(100, TimeUnit.MILLISECONDS);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        List<Runnable> r = exe.shutdownNow();


    }

    private static short newRandomPort() {
        Random r = new Random();
        short port = (short) (r.nextInt(Short.MAX_VALUE)
                % (Short.MAX_VALUE - GnutellaConstants.MIN_PORT) + GnutellaConstants.MIN_PORT);
        return port;
    }

    /**
     * Returns the port in which this Servent is listening
     *
     * @return the port
     */
    public short port() {
        return port;
    }



    public boolean seen(Message messageP) {
        return messageCache.putIfAbsent(messageP.idString(), messageP) != null;
    }


    synchronized void removeDeadConnections() {
        Iterator<Map.Entry<SocketAddress, PeerThread>> ii = neighbors.entrySet().iterator();
        while (ii.hasNext()) {
            if (!ii.next().getValue().connected()) {
                ii.remove();
            }
        }
    }


    public synchronized void setMaxNodes() {
        maxNodes++;
    }

    public int getMaxNodes() {
        return maxNodes;
    }


    /**
     * Returns a PingMessage. MessageID is randomly generated. Ttl value is set
     * by default to 7, hop to 0
     *
     * @return the PingMessage
     */
    public PingMessage createPing() {
        PingMessage m = new PingMessage(GnutellaConstants.DEFAULT_TTL,
                GnutellaConstants.INITIAL_HOP, address);
        return m;
    }

    private PongMessage newPong(Message sent) {
        return newPong(sent.id.toByteArray());
    }

    private PongMessage newPong(byte[] messageId) {
        InetAddress result;
        synchronized (this) {
            result = host;
        }
        PongMessage pong = new PongMessage(messageId,
                GnutellaConstants.DEFAULT_TTL, (byte) 0, address,
                (short) address.getPort(), result,
                getNumberFileShared(), getNumberKbShared());
        return pong;
    }

    private Message createQuery(short minSpeed, String searchCriteria) {
        QueryMessage query = new QueryMessage(GnutellaConstants.DEFAULT_TTL,
                GnutellaConstants.INITIAL_HOP, 2 + searchCriteria.length(),
                address, minSpeed, searchCriteria);
        return query;
    }

    private Message createQuery(short minSpeed, byte[] searchCriteria) {
        QueryMessage query = new QueryMessage(GnutellaConstants.DEFAULT_TTL,
                GnutellaConstants.INITIAL_HOP, 2 + searchCriteria.length,
                address, minSpeed, searchCriteria);
        return query;
    }

    public QueryHitMessage createQueryHit(InetSocketAddress recipient, byte[] idMessage, int pL, byte[] result) {
        return createQueryHit(recipient, idMessage, pL, port, result, id);
    }

    public QueryHitMessage createQueryHit(InetSocketAddress recipient, byte[] idMessage, int pL,
                                          short port,
                                          byte[] result, byte[] idServent) {

        QueryHitMessage queryHit = new QueryHitMessage(idMessage,
                GnutellaConstants.DEFAULT_TTL, (byte) 0, (byte) pL,
                recipient, port, host,
                GnutellaConstants.DFLT_SPEED, result,
                idServent);
        return queryHit;
    }

    /**
     * Adds a PingMessage to the pending Message queue
     */
    public void addPing() {
        pending(createPing());
    }

    public final void pending(Message m) {
        pendingMessages.execute(() -> handle(m));
    }


    /**
     * Adds a QuerMessage to the pending Message queue
     *
     * @param minSpeed       The minimum speed (in kB/second) of servents that should
     *                       respond to this message.
     * @param searchCriteria A nul (i.e. 0x00) terminated search string. The maximum length
     *                       of this string is bounded by the Payload_Length field of the
     *                       descriptor header.
     */
    public void query(short minSpeed, String searchCriteria) {
        broadcast(createQuery(minSpeed, searchCriteria));
    }

    public void query(short minSpeed, byte[] searchCriteria) {
        broadcast(createQuery(minSpeed, searchCriteria));
    }

    final AtomicInteger messageCount = new AtomicInteger();


    public final void handle(Message message) {
        boolean fordward = message.refreshMessage();

        switch (message.getPayloadD()) {
            case GnutellaConstants.PING:
                onPing(message, fordward);
                break;

            case GnutellaConstants.PONG:
                onPong((PongMessage) message);
                break;

            case GnutellaConstants.QUERY:
                onQuery((QueryMessage) message);
                break;

            case GnutellaConstants.QUERY_HIT:
                onHit((QueryHitMessage) message);
                break;

        }

        if (messageCount.incrementAndGet() % GnutellaConstants.AUTO_PING_PER_N_MESSAGES == 0) {
            addPing();
        }
    }

    public void onPing(Message message, boolean forward) {
        if (seen(message))
            return;

        PeerThread n = neighbors.get(message.recipient);

        if (forward) {
            // Si yo lo cree
            if (address.equals(message.recipient)) {
                broadcast(message);
            } else {
                if (n != null) {
                    // contesto al que lo envio
                    n.send(
                            newPong(message)
                    );
                    broadcast(message, message.recipient);
                }
            }

        } else {
            // no tiene vida solo contesto
            if (n != null)
                n.send(newPong(message));
        }
    }

    public void onPong(PongMessage messageP) {
        // 3 casos es nuestro, no es nuestro, no debio de haber
        // llegado

        if (seen(messageP)) {

            // // Primer caso es nuestro

            InetSocketAddress ownerPing = messageP.recipient;
            if (address.equals(ownerPing)) {
                //for this node

                Collection<InetSocketAddress> firstPing = firstPongsFromNeighbors.get(messageP.idString());
                if (firstPing.contains(messageP.recipient)) {

                    connect(messageP.getIpAddressString(), messageP.getPort());

                } else { // Es el primer pong que recibe este nodo

                    firstPing.add(messageP.recipient);

                }
            } else {
                //forward

                neighbors.get(ownerPing).send(messageP);
            }

        } else {
            // EL PROTOCOLO DICE QUE SI ME LLEGA UN PONG DE UN PING
            // QUE NO CONOZCO DEBO SACAR EL PONG DE LA RED
            // PERO ESO YA LO HACEMOS AL HACER PULL SOBRE LA LISTA
            // DE MENSAJES

        }
    }

    public void onHit(QueryHitMessage m) {


        InetSocketAddress owner = m.recipient;
        //if (!address.equals(owner)) {
        model.onQueryHit(this, m);
        //} else {

        if (!address.equals(owner)) {
            if (!seen(m))
                neighbors.get(owner).send(m);
        }
    }


    public void onQuery(QueryMessage q) {


        if (address.equals(q.recipient)) {

        } else {
            model.search(this, q, (mQueryH) -> {
                neighbors.get(q.recipient).send(mQueryH);
            });
            broadcast(q, q.recipient);
        }

        //TODO decide to propagate query

//            Enumeration<InetSocketAddress> e = neighbors.keys();
//
//            // propago el query
//            while (e.hasMoreElements()) {
//
//                InetSocketAddress idNodeNext = e.nextElement();
//                // a todos los que no son el que lo recibio
//                if (!idNodeNext.equals(message.receptorNode)) {
//
//                    neighbors.get(idNodeNext).send(
//                            message);
//                }
//
//            }


    }

//    public QueryHitMessage searchFiles(QueryMessage message, String directory) {
//        File myFiles[] = new File(directory).listFiles();
//        sortFilesDesc(myFiles);
//
//        List<Triple<String, Integer, Integer>> ll = Global.newArrayList(0);
//
//        int payloadL = GnutellaConstants.QUERYHIT_PART_L + GnutellaConstants.SERVER_ID_L;
//
//        CharSequence qs = message.queryString();
//
//        for (int i = 0; i < myFiles.length; i++) {
//
//            File ff = myFiles[i];
//            if (ff.isDirectory())
//                continue;
//
//            String fn = ff.getName();
//
//            if (fn.contains(qs)) {
//                long len = ff.length();
//                ll.add(
//                    Triple.of(fn.trim(), (int) len, i)
//                );
//                payloadL += len;
//
//            }
//        }
//
//        return !ll.isEmpty() ? createQueryHit(message.idBytes(), payloadL, port,
//                null, //TODO encode 'll' as bytes,
//                id
//        ) : null;
//    }


    public void broadcast(Message message, InetSocketAddress except) {
        //logger.trace("{} broadcast: {} exceptTo {}", server.socket, message, except);
        seen(message);
        neighbors.forEach((k, v) -> {

            if (!k.equals(except))
                v.send(message);
        });
    }

    public void broadcast(Message message) {
        //logger.trace("broadcast: {}", message);
        seen(message);
        neighbors.forEach((k, v) -> v.send(message));
    }

    /**
     * Returns the number of files that the servent is sharing on the network
     *
     * @return number of files shared
     */
    public synchronized int getNumberFileShared() {
        return numberFileShared;
    }

    /**
     * Returns the number of kilobytes of data that the servent is sharing on
     * the network
     *
     * @return number of kilobytes shared
     */
    public synchronized int getNumberKbShared() {
        return numberKbShared;
    }

    /**
     * Creates a download connection with a Server with the specified data
     *
     * @param ip    The IP address of the responding host.
     * @param port  The port number on which the responding host can accept
     *              incoming connections.
     * @param file  The double-nul (i.e. 0x0000) terminated name of the file whose
     *              index is File_Index.
     * @param size  The size (in bytes) of the file whose index is File_Index
     * @param range A number, assigned by the responding host, which is used to
     *              uniquely identify the file matching the corresponding query.
     */
    public synchronized void download(InetAddress i, short port, String file, int size, int range) {

        try {

            DownloadThread thread = new DownloadThread(new Socket(i, port), this, false);
            if (thread.start(file, size, range)) {
                exe.execute(thread);
            } else {
                System.out.println("DENIED  DOWNLOAD CONNECTION");
            }
        } catch (IOException e) {

            e.printStackTrace();
        }


    }


//    private static void sortFilesDesc(File[] files) {
//        Comparator<File> comparator = (o1, o2) -> Long.valueOf(o1.lastModified()).compareTo(
//                o2.lastModified());
//        Arrays.sort(files, comparator);
//
//    }


}
