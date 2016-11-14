package spacegraph.net;

import spacegraph.net.reply.Reply;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Stores information on a client local and remote.
 */
public class Client {

	public final IRCConnection connection;
	public final String id;
	private String username;
	private String hostname;
	private String servername;
	private String realName;

	private long lastCheckin;

	private final ArrayList<Channel> channels = new ArrayList<>();
	private final HashMap<ClientMode, Boolean> modes = new HashMap<>();
	private String awayMessage = "";

	/**
	 * Creates a new Client object. Will not add to ClientManager.
	 * 
	 * @param connection
	 *            The client's connection.
	 * @param nickname
	 *            Nickname of the client.
	 */
	public Client(IRCConnection connection, String nickname) {
		this.connection = connection;
		this.id = nickname;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Client)) return false;

		Client client = (Client) o;

		return id.equals(client.id);

	}

	/**
	 * 
	 * Creates a new Client object. Will not add to ClientManager.
	 * 
	 * @param connection
	 *            The client's connection.
	 * @param nickname
	 *            Nickname of the client.
	 * @param username
	 *            User Name of client.
	 * @param hostname
	 *            Host Name of client.
	 * @param servername
	 *            Server Name of client.
	 * @param realName
	 *            Real Name of client.
	 */
	public Client(IRCConnection connection, String nickname, String username, String hostname, String servername, String realName) {
		this.connection = connection;
		this.id = nickname;
		this.username = username;
		this.hostname = hostname;
		this.servername = servername;
		this.realName = realName;

	}

	public enum ClientMode {
		INVISIBLE("i"), SERVER_NOTICES("s"), WALLOPS("w"), OPERATOR("o");

		private final String symbol;

		ClientMode(String letter) {
			this.symbol = letter;
		}

		public String getSymbol() {
			return symbol;
		}
	}

	/**
	 * Gets mode of user.
	 * 
	 * @param mode
	 * @return True if Mode is set to user False if not.
	 */
	public boolean getMode(ClientMode mode) {
		return modes.get(mode);
	}

	/**
	 * Sets mode to add and send the change to the user.
	 * 
	 * @param mode
	 *            Client mode to be set.
	 * @param add
	 *            What to set mode to.
	 * @param sender
	 *            Who requested the mode change.
	 * @throws IOException
	 */
	public void setMode(ClientMode mode, boolean add, Client sender) throws IOException {
		connection.send(Reply.RPL_UMODEIS, this, sender.getAbsoluteName() + " sets mode " + (add ? "+" : "-") + mode.getSymbol() + " on " + id);

		modes.put(mode, add);
	}

	public String getRealName() {
		return realName;
	}

	public String getUsername() {
		return username;
	}

	/*public void setId(String id) {
		this.id = id;
	}*/

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public String getServername() {
		return servername;
	}

	public void setServername(String servername) {
		this.servername = servername;
	}

	public ArrayList<Channel> getChannels() {
		return channels;
	}

	public void addChannel(Channel channel) {
		channels.add(channel);
	}

	public void removeChannel(Channel channel) {
		channels.remove(channel);
	}

	public boolean hasMode(ClientMode mode) {
		//if (!modes.containsKey(mode)) return false;
		return modes.get(mode)!=null;
	}

	public boolean isServerOP() {
		//if (!modes.containsKey(ClientMode.OPERATOR)) return false;
		return modes.get(ClientMode.OPERATOR)!=null;
	}

	public long getLastCheckin() {
		return lastCheckin;
	}

	public void setLastCheckin(long lastCheckin) {
		this.lastCheckin = lastCheckin;
	}

	public static int getHopCount() {
		return 0;
	}

	/**
	 * Returns Nickname!Username@Hostname
	 * 
	 * @return Nickname!Username@Hostname
	 */
	public String getAbsoluteName() {
		return id + '!' + this.getUsername() + '@' + this.getHostname();
	}

	/**
	 * Sets away message. If away message is "" then the user is not away.
	 * 
	 * @param message
	 *            The away message.
	 */
	public void setAwayMessage(String message) {
		this.awayMessage = message;
	}

	/**
	 * Returns away message. If away message is "" then the user is not away.
	 * 
	 * @return Away message.
	 */
	public String getAwayMessage() {
		return awayMessage;
	}

}
