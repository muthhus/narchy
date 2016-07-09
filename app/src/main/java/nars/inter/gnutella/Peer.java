package nars.inter.gnutella;

import nars.util.Util;
import nars.util.data.map.CapacityLinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;


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
    private final short myPort;
    public final Server server;
    public final Client client;
    final ConcurrentHashMap<InetSocketAddress, PeerThread> neighbors;
    final ExecutorService exe =
            //Executors.newFixedThreadPool(8);
//            new ThreadPoolExecutor(0, Integer.MAX_VALUE,
//                                      1L, TimeUnit.SECONDS,
//                                      new SynchronousQueue<Runnable>());
            //Executors.newFixedThreadPool(4);
            Executors.newCachedThreadPool();

    final int maxConnections = 5;

    final Map<String, Message> messageCache = new CapacityLinkedHashMap(4096);
    private final byte[] id;
    public final InetSocketAddress address;


    /**
     * this instance must implement ClientModel
     */
    protected Peer() throws IOException {
        this(null);
    }

    public Peer(ClientModel model) throws IOException {
        this(newRandomPort(), model);
    }

    /**
     * Constructs a Servent that listens for upcoming connections in the
     * specified port, and shares files of the especified directoryPath
     *
     * @param pathName Directory Path for sharinf files
     * @throws IOException IO error when opening the socket in which this Servent
     *                     listens for upcoming connections
     */
    public Peer(short port, ClientModel model) throws IOException {

        this.myPort = port;
        if (model == null)
            model = (ClientModel) this;

        neighbors = new ConcurrentHashMap<>(maxConnections);

        host = InetAddress.getLocalHost();
        address = new InetSocketAddress(host, port);


        this.client = new Client(this, myPort, neighbors,
                host,
                id = IdGenerator.getIdServent(), model
        );

        exe.execute(() -> {
            while (client.running) {

                Util.pause(GnutellaConstants.DEAD_CONNECTION_REMOVAL_INTERVAL_MS);

                client.removeDeadConnections();

            }
        });

        this.server = new Server(this, myPort, neighbors, client);

        exe.execute(server);

        logger.info("started {}", server.socket);
    }

    public void connect(Peer p) {
        if (p == this)
            throw new RuntimeException("self connect");
        connect(p.host(), p.port());
    }


    /**
     * Connects to a Gnutella client with the specified ip and port
     *
     * @param ip   The ip in string format of the Gnutella client
     * @param port The port in which the Gnutella client listens
     * @return true if the connection is made succesfully, false otherwise
     */
    public boolean connect(String ip, short port) {
        if (neighbors.size() > client.getMaxNodes()) {
            client.setMaxNodes();
        }
        boolean connection = client.connect(ip, port);
        return connection;
    }

    /**
     * Used to actively discover hosts on the network
     */
    public void ping() {
        client.addPing();
    }

    /**
     * The primary mechanism for searching the distributed network.
     *
     * @param searchCriteria the name of file for the search
     */
    public void query(String searchCriteria) {
        client.query(GnutellaConstants.DFLTMIN_SPEED, searchCriteria);
    }

    public void query(byte[] searchCriteria) {
        client.query(GnutellaConstants.DFLTMIN_SPEED, searchCriteria);

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
        client.download(InetAddress.getByName(ip), port, file, size, 0);
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

        client.stop();

        logger.info("stop");

        Enumeration<InetSocketAddress> n = neighbors.keys();
        while (n.hasMoreElements()) {
            InetSocketAddress idN = n.nextElement();
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
        return myPort;
    }

    public String host() {
        return host.getHostName();
    }

    public boolean seen(Message messageP) {
        return messageCache.putIfAbsent(messageP.idString(), messageP) != null;
    }
}
