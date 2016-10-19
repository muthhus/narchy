package spacegraph.irc;

import spacegraph.irc.reply.Reply;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;

/**
 * An object that stores all the information on a Channel including its clients.
 */
public class Channel {

	public final String id;
	private final IRCServer server;
	private String password;
	private final ArrayList<Client> clients = new ArrayList<>();
	private final ArrayList<Client> ops = new ArrayList<>();
	private final EnumMap<ChannelMode, Boolean> modes = new EnumMap<>(ChannelMode.class);
	private final ArrayList<Client> voiceList = new ArrayList<>();
	private final ArrayList<Client> invitedUsers = new ArrayList<>();
	//private int userLimit = 100;
	private String topic = "";
	private final boolean temporary;

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Channel) {
			return ((Channel)obj).id.equals(id);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}


	/**
	 * Creates a new a new Channels object. Will not add to ChannelManger.
	 * 
	 * @param id
	 *            Name of channel should start with '#' or '&'.
	 * @param password
	 *            Password of channel. If blank then no password is needed.
	 * @param server
	 * @see ChannelManger.addChannel(Channel)
	 */
	public Channel(String id, String password, boolean temporary, IRCServer server) {
		this.id = id;
		this.password = password;
		this.temporary = temporary;
		this.server = server;
	}

	public void setUserLimit(int i) {
		//
	}

	public enum ChannelMode {
		PRIVATE("p"), SECRET("s"), INVITE_ONLY("i"), TOPIC("t"), NO_MESSAGE_BY_OUTSIDE("n"), MODERATED_CHANNEL("m");
		private final String symbol;

		ChannelMode(String symbol) {
			this.symbol = symbol;
		}

		public String getSymbol() {
			return symbol;
		}
	}

	/**
	 * Returns weather or not a mode is set.
	 * 
	 * @param mode
	 * @return True if mode is set false if not.
	 */
	public boolean getMode(ChannelMode mode) {
		//if (!modes.containsKey(mode)) return false;
		return modes.get(mode)==Boolean.TRUE;
	}

	/**
	 * Sets mode to add. Also tells all users of the new change.
	 * 
	 * @param mode
	 *            ChannelMode to be set.
	 * @param add
	 *            What mode should be set to.
	 * @param sender
	 *            Who requested this mode change. To be used when sending to all
	 *            Clients.
	 * @throws IOException
	 */
	public void setMode(ChannelMode mode, boolean add, Client sender) throws IOException {
		send(Reply.RPL_CHANNELMODEIS, sender.getAbsoluteName() + " sets mode " + (add ? "+" : "-") + mode.getSymbol() + " on " + id);
		modes.put(mode, add);
	}

	/**
	 * Sets mode to add.
	 * 
	 * @param mode
	 *            ChannelMode to be set.
	 * @param add
	 *            What mode should be set to.
	 * @throws IOException
	 */
	public void setMode(ChannelMode mode, boolean add) {
		modes.put(mode, add);
	}

	/**
	 * Sends a reply to all clients on channel.
	 * 
	 * @param reply
	 *            Reply code to use.
	 * @param message
	 *            Message to send.
	 * @throws IOException
	 */
	public void send(Reply reply, String message) throws IOException {
		for (Client current : clients)
			current.connection.send(reply, current, message);
	}

	/**
	 * Sends a PRIVMSG to all clients on channel.
	 * 
	 * @param sender
	 *            Who sent the message.
	 * @param message
	 *            Message to be sent.
	 * @throws IOException
	 */
	public void sendMessage(Client sender, String message) throws IOException {
		String msg = ':' + sender.getAbsoluteName() + " PRIVMSG " + id + ' ' + message;
		for (Client current : clients)
			if (current != sender) {
				send(current, msg);
			}
	}

	private static void send(Client target, String message) throws IOException {
		target.connection.send(message);
	}

	private void sendToAll(String message) throws IOException {
		for (Client current : clients)
			current.connection.send(message);
	}

	/**
	 * Adds client to channel. Sends the client a NAMES command and sends a JOIN
	 * message to all clients.
	 * 
	 * @param client
	 *            Client to be added to the channel.
	 * @throws IOException
	 */
	public void addClient(Client client) throws IOException {
		clients.add(client);
		if (!this.getMode(ChannelMode.MODERATED_CHANNEL) || client.isServerOP()) this.voiceList.add(client);

		this.sendToAll(':' + client.id + '!' + client.getUsername() + '@' + client.getHostname() + " JOIN " + id);
		if (this.checkOP(client)) {
			this.send(Reply.RPL_CHANNELMODEIS, id + " +o " + client.id);
		} else if (this.hasVoice(client)) {
			this.send(Reply.RPL_CHANNELMODEIS, id + " +v " + client.id);
		}

		String message = "@ " + id + " :";
		ArrayList<Client> clients = this.getClients();
		for (Client current : clients) {
			if (this.checkOP(current)) {
				message = message + '@' + current.id + ' ';
			} else if (this.hasVoice(current)) {
				message = message + '+' + current.id + ' ';
			} else {
				message = message + current.id + ' ';
			}
		}
		client.connection.send(Reply.RPL_NAMREPLY, client, message);
		client.connection.send(Reply.RPL_ENDOFNAMES, client, id + " :End of /NAMES list.");
	}

	public void removeClient(Client client) {
		voiceList.remove(client);
		clients.remove(client);
		if (this.clients.isEmpty() && this.isTemporary()) {
			server.removeChannel(this);
		}
	}

	public ArrayList<Client> getClients() {
		return clients;
	}

	public boolean checkPassword(String password) {
		if (this.password == null || this.password.isEmpty()) return true;
		return this.password.equals(password);
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean checkOP(Client client) {
		return ops.contains(client);
	}

	public void addOP(Client client) {
		ops.add(client);
	}

	public void takeOP(Client client) {
		ops.remove(client);
	}

	public int getCurrentNumberOfUsers() {
		return clients.size();
	}

	public void giveVoice(Client voicee) {
		voiceList.add(voicee);
	}

	public void takeVoice(Client voicee) {
		voiceList.remove(voicee);
	}

	public boolean hasVoice(Client voicee) {
		return voiceList.contains(voicee);
	}

	public String getTopic() {
		if (topic.isEmpty() || topic == null) return "Default Topic";
		return topic;
	}

	/**
	 * Sets topic and tell all clients on channel of the change.
	 * 
	 * @param topic
	 *            New topic.
	 * @throws IOException
	 */
	public void setTopic(String topic) throws IOException {
		this.topic = topic;
		for (Client current : clients)
			current.connection.send(Reply.RPL_TOPIC, current, id + ' ' + this.getTopic());
	}

	public void inviteUser(Client client) {
		this.invitedUsers.add(client);
	}

	public void unInviteUser(Client client) {
		this.invitedUsers.remove(client);
	}

	public boolean isUserInvited(Client client) {
		return invitedUsers.contains(client);
	}

	public boolean isUserOnChannel(Client client) {
		return clients.contains(client);
	}

	public EnumMap<ChannelMode, Boolean> getModeMap() {
		return modes;
	}

	public boolean isTemporary() {
		return temporary;
	}

}
