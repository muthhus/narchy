package spacegraph.irc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.irc.commands.Command;
import spacegraph.irc.reply.Reply;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Contains main method. The main class creates a new Server object which starts
 * everything up.
 */
public class IRCServer implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(IRCServer.class);

	private static final String VERSION = "v1.0-RELEASE";
	public final String hostname;
	private final boolean running;
	private final ServerSocket serverSocket;
	private final Config config;
	private final Map<String,Channel> channels = new ConcurrentHashMap();
	private final Map<String,Client> clients = new ConcurrentHashMap<>();

	/**
	 * Server constructor. Starts all managers, opens the socket and starts the
	 * running thread.
	 *
	 * @throws Exception
	 */
	public IRCServer(String hostname, int port) throws Exception {

		config = new Config();

		Command.init();

		serverSocket = new ServerSocket(port);

		this.hostname = hostname;

		running = true;

		logger.info("Started Server on port: " + serverSocket.getLocalPort());

		new Thread(this, "Running Thread").start();
	}

	public static String[] motd() {
		return new String[] { "Connected" };
	}

	public static void main(String args[]) throws Exception {
		new IRCServer("localhost", 6667);
	}

	/**
	 * Returns current server version.
	 *
	 * @return Current server version.
	 */
	public static String getVersion() {
		return VERSION;
	}

	/**
	 * Main running thread. Waits for sockets then creates a new Connection
	 * object on a new thread.
	 */
	@Override
	public void run() {
		while (running) {
			try {
				Socket socket = serverSocket.accept();
				IRCConnection c = new IRCConnection(this, socket);
				new Thread(c).start();
				//exe.submit(c);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sends MOTD to client. Reads from motd.txt. If not found will create one.
	 *
	 * @param client
	 *            Client to send MOTD.
	 * @throws IOException
	 */
	public void sendMOTD(Client client) throws IOException {
		client.connection.send(Reply.RPL_MOTDSTART, client, ":- " + Config.getProperty("hostname") + " Message of the day - ");
//		File motd = new File("motd.txt");
//		if (!motd.exists()) Utilities.makeFile("motd.txt");
		for (String line : motd())
			client.connection.send(Reply.RPL_MOTD, client, ":- " + line);
		client.connection.send(Reply.RPL_ENDOFMOTD, client, ":End of /MOTD command");
	}

//	/**
//	 * Creates a new ClientManager object. Reads and process channels.txt. If
//	 * channels.txt does not exist it will create one.
//	 *
//	 * @throws IOException
//	 */
//	public ChannelManager(String... channelAdds) throws IOException {
////		File channels = new File("channels.txt");
////		if (!channels.exists()) Utilities.makeFile("channels.txt");
////		BufferedReader reader = new BufferedReader(new FileReader(channels));
////		while (reader.ready()) {
////			String line = reader.readLine();
////			add(line);
////		}
////		reader.close();
//
//		for (String c : channelAdds)
//			add(c);
//	}

	/**
	 * Gets Config object.
	 *
	 * @return Config.
	 */
	public Config getConfig() {
		return config;
	}

	public void forEachChannel(Consumer<Channel> each) {
		channels.forEach((k,v)-> each.accept(v));
	}



//	/**
//	 * Adds a channel to the Array.
//	 *
//	 * @param channel
//	 *            Channel to be added.
//	 */
//	public void addChannel(Channel channel) {
//		channels.add(channel);
//	}

	public void addChannel(String line) throws IOException {
		if (line.startsWith("//")) return;
		String[] lineArray = line.split(":");
		if (lineArray.length < 5) return;
		Channel channel = new Channel(lineArray[0], lineArray[1], false, this);
		channel.setUserLimit(Integer.parseInt(lineArray[2]));
		for (char c : lineArray[3].toCharArray()) {
			switch (c) {
				case 'p':
					channel.setMode(Channel.ChannelMode.PRIVATE, true);
					break;
				case 's':
					channel.setMode(Channel.ChannelMode.SECRET, true);
					break;
				case 'i':
					channel.setMode(Channel.ChannelMode.INVITE_ONLY, true);
					break;
				case 't':
					channel.setMode(Channel.ChannelMode.TOPIC, true);
					break;
				case 'n':
					channel.setMode(Channel.ChannelMode.NO_MESSAGE_BY_OUTSIDE, true);
					break;
				case 'm':
					channel.setMode(Channel.ChannelMode.MODERATED_CHANNEL, true);
					break;
			}
		}
		channel.setTopic(lineArray[4]);
		this.channels.put(channel.id, channel);
	}

	/**
	 * Removes a channel from the Array.
	 *
	 * @param channel
	 *            Channel to be removed.
	 */
	public void removeChannel(Channel channel) {
		channels.remove(channel);
	}

//	/**
//	 * Returns the number of channels that do not have the secret flag.
//	 *
//	 * @return The number of channels that do not have the secret flag.
//	 */
//	public int getNonSecretChannels() {
//		int channels = 0;
//		for (Channel channel : this.getChannels())
//			if (!channel.getMode(ChannelMode.SECRET)) channels++;
//		return channels;
//	}

	/**
	 * Returns channel with the given name.
	 *
	 * @param name
	 *            Name of channel that is returned.
	 * @return Returns Channel with the given name or null if channel does not
	 *         exist.
	 */
	public Channel getChannel(String name) {
		return channels.computeIfAbsent(name, (n) -> new Channel(n, "", true, this));
	}

	/**
	 * Sends a message to all clients connected that are directly connected to
	 * this server.
	 *
	 * @param reply
	 * @param args
	 * @throws IOException
	 */
	public void broadcastLocal(Reply reply, String args) throws IOException {
		for (Client client : this.clients.values())
			if (client.connection.isClient())
				client.connection.send(reply, client, args);
	}

	/**
	 * Sends a message to all clients connected that are directly connected to
	 * this server.
	 *
	 * @param prefix
	 *            The prefix with out a preceding :
	 * @param command
	 * @param args
	 * @throws IOException
	 */
	public void broadcastLocal(String prefix, String command, String args) throws IOException {
		for (Client client : this.clients.values())
			if (client.connection.isClient())
				client.connection.send(prefix, command, args);
	}

	//	/**
//	 * Adds client to array. Will not check for nick names.
//	 *
//	 * @param client
//	 *            Client to add.
//	 * @throws Exception
//	 */
	public void addChannel(Client client) {
		clients.put(client.getUsername(), client);
	}

	/**
	 * Returns Client object the matches the given nickname. Returns null is no
	 * client has the given nickname.
	 *
	 * @param nickname
	 *            Nickname of client that is being requested.
	 * @return Client that matches given nickname. Returns null if no client has
	 *         that nickname.
	 */
	public Client getClient(String nickname) {
		return clients.get(nickname);
	}

	/**
	 * Removes client from clients array.
	 *
	 * @param client
	 *            Client to be removed.
	 */
	public void removeClient(Client client) {
		clients.remove(client);
	}

	/**
	 * Removes Client that has given nickname from array.
	 *
	 * @param nickname
	 *            Nickname of client that will be removed.
	 */
	public void removeClient(String nickname) {
		clients.remove(nickname);
	}

	/**
	 * Returns weather or not a Client with given nickname exists.
	 *
	 * @param nickname
	 * @return True if client exists false if not.
	 */
	public boolean isNick(String nickname) {
		return clients.containsKey(nickname);
	}

	/**
	 * Returns the amount of clients that are not invisible.
	 *
	 * @return The amount of Clients that are not invisible.
	 */
	public int getClientCount() {
		int users = 0;
		for (Client current : this.clients.values())
			if (!current.hasMode(Client.ClientMode.INVISIBLE)) users++;
		return users;
	}

	/**
	 * Returns the amount of clients that are invisible.
	 *
	 * @return The amount of Clients that are invisible.
	 */
	public int getInvisibleClientCount() {
		int users = 0;
		for (Client current : this.clients.values())
			if (current.hasMode(Client.ClientMode.INVISIBLE)) users++;
		return users;
	}

	/**
	 * Returns the amount of Server Operators online.
	 *
	 * @return The amount of Server Operators online.
	 */
	public int getOps() {
		int ops = 0;
		for (Client current : this.clients.values())
			if (current.isServerOP()) ops++;
		return ops;
	}

	/**
     * Manages all channels. Also adds channels from channels.txt file.
     */
    public interface ChannelManager {

    }

	/**
     * Manages all clients local and remote.
     */
    public interface ClientManager {


    }
}