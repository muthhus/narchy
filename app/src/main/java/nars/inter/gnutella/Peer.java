package nars.inter.gnutella;

import nars.util.data.map.CapacityLinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;


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


    private final InetAddress ipAddress;
    private final short myPort;
    public final Server server;
    public final Client client;
    final ConcurrentHashMap<InetSocketAddress, PeerThread> neighbors;
    private final IdGenerator myIdGenerator;
    private final File myDirectory;

    final int maxConnections = 5;

    final Map<String, Message> messageCache = new CapacityLinkedHashMap(4096);

    /**
     * Constructs a Servent that listens for upcoming connections in the
     * specified port, and shares files of the especified directoryPath
     *
     * @param pathName Directory Path for sharinf files
     * @throws IOException IO error when opening the socket in which this Servent
     *                     listens for upcoming connections
     */
    public Peer(String pathName, ClientModel model) throws IOException {

        this.myPort = newRandomPort();

        neighbors = new ConcurrentHashMap<>(maxConnections);

        ipAddress = InetAddress.getLocalHost();

        myIdGenerator = new IdGenerator();

        myDirectory = new File(pathName);

        this.client = new Client(this, myPort, neighbors,
                ipAddress,
                IdGenerator.getIdServent(), model
        );

        client.start();

        this.server = new Server(this, myPort, neighbors,
                myIdGenerator,
                myDirectory, client);


        new Thread(server).start();

        logger.info("started {}", server.socket);
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
        client.addQuery(GnutellaConstants.DFLTMIN_SPEED, searchCriteria);
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

        Enumeration<InetSocketAddress> n = neighbors.keys();
        while (n.hasMoreElements()) {
            InetSocketAddress idN = n.nextElement();
            neighbors.remove(idN).close();
        }


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
        return ipAddress.getHostName();
    }

    public boolean seen(Message messageP) {
        return messageCache.putIfAbsent(messageP.idString(), messageP) != null;
    }
}
