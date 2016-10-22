package com.rbruno.irc.templates;

import com.rbruno.irc.Server;
import com.rbruno.irc.net.Connection;

/**
 * Phrases and stores information on a request made by a client.
 */
public class Request {

	private Connection connection;
	private String prefix;
	private String command;
	private String[] args = new String[0];
	private Client client;

	private boolean cancelled;

	/**
	 * Creates a new Request object. Phrases the line into prefix, command and
	 * arguments. KNOWN BUG: Everything after the last ':' will be put in one
	 * arguments but will keep it ':'.
	 * 
	 * @param connection
	 *            Connection the request came from.
	 * @param line
	 *            The line that was sent.
	 * @throws Exception
	 */
	public Request(Connection connection, String line) throws Exception {
		this.connection = connection;
		if (line.startsWith(":")) {
			this.prefix = line.split(" ")[0].substring(1);
			line = line.substring(prefix.length() + 1);
		}
		this.command = line.split(" ")[0];

		if (line.length() != command.length()) {
			line = line.substring(command.length() + 1);
		} else {
			line = line.substring(command.length());
		}
		String postDelimiter = line.substring(line.split(":")[0].length());
		line = line.substring(0, line.length() - postDelimiter.length());
		if (line.length() > 0) this.args = line.split(" ");
		if (postDelimiter.length() > 0) {
			String[] newArgs = new String[args.length + 1];
			for (int i = 0; i < args.length; i++) {
				newArgs[i] = args[i];
			}
			newArgs[newArgs.length - 1] = postDelimiter;
			this.args = newArgs;
		}
		if (prefix != null && connection.isServer()) {
			client = Server.getServer().getClientManager().getClient(prefix);
		} else if (connection.isClient()) {
			client = connection.getClient();
		} else {
			client = null;
		}
	}

	/**
	 * Returns the connection that the request was sent through.
	 * 
	 * @return The connection that the request was sent through.
	 */
	public Connection getConnection() {
		return connection;
	}

	/**
	 * Returns the prefix of the that was sent. The prefix should only be used
	 * when sent by server.
	 * 
	 * @return The prefix of the that was sent.
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Returns the command that was sent.
	 * 
	 * @return The command that was sent.
	 */
	public String getCommand() {
		return command;
	}

	/**
	 * Returns the arguments that were sent. If a ':' delimiter was used then
	 * the last argument will start with a ':'.
	 * 
	 * @return The array of arguments.
	 */
	public String[] getArgs() {
		return args;
	}

	/**
	 * Returns the client that sent the request.
	 * 
	 * @return The client that sent the request.
	 */
	public Client getClient() {
		return client;
	}

	/**
	 * Return weather or not this request is cancelled. When a request is
	 * cancelled it will not be run by the server.
	 * 
	 * @return Weather or not this request is cancelled.
	 */
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * Sets the cancelled status of this request. When a request is cancelled it
	 * will not be run by the server.
	 * 
	 * @param cancelled
	 *            Cancelled status.
	 */
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

}