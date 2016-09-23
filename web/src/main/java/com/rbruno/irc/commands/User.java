package com.rbruno.irc.commands;

import java.io.IOException;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.Config;
import com.rbruno.irc.reply.Reply;
import com.rbruno.irc.Client;
import com.rbruno.irc.Request;

public class User extends Command {

	public User() {
		super("USER", 4);
	}

	@Override
	public void run(Request request) throws IOException {
		IRCServer server = request.server();

		switch (request.connection.getType()) {
		case LOGGIN_IN:
			request.connection.close();
			break;
		case CLIENT:
			Client client = request.getClient();
			client.setUsername(request.getArgs()[0]);
			client.setHostname(Config.getProperty("hostname"));
			client.setServername(Config.getProperty("hostname"));
			client.setRealName(request.getArgs()[3].substring(1));
			request.connection.setClient(client);
			request.connection.send(1, client.id, ":Welcome to the " + Config.getProperty("hostname") + " Internet Relay Chat Network " + client.id);
			request.connection.send(Reply.RPL_LUSERCLIENT, client, ":There are " + server.getClientCount() + " users and " + server.getInvisibleClientCount() + " invisible on 1 servers");
			request.connection.send(Reply.RPL_LUSEROP, client, server.getOps() + " :operator(s) online");
			//request.getConnection().send(Reply.RPL_LUSERUNKNOWN, client, "0 :unknown connection(s)");
			//request.connection.send(Reply.RPL_LUSERCHANNELS, client, server.getNonSecretChannels() + " :channels formed");
			request.connection.send(Reply.RPL_LUSERME, client, ":I have " + server.getClientCount() + " clients and 1 servers");
			server.sendMOTD(client);
			server.add(client);
			break;
		case SERVER:
			// TODO Server
			break;
		}

	}
}
