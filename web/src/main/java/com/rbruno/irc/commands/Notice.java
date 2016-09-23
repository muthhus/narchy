package com.rbruno.irc.commands;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.reply.Error;
import com.rbruno.irc.Client;
import com.rbruno.irc.Request;

public class Notice extends Command {

	public Notice() {
		super("NOTICE", 2);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		Client client = server.getClient(request.getArgs()[0]);
		if (client != null) {
			client.connection.send(':' + request.getClient().getAbsoluteName() + " NOTICE " + client.id + ' ' + request.getArgs()[1]);
		} else {
            request.getClient().connection.send(Error.ERR_NOSUCHNICK, client, request.getArgs()[0] + " :No such nick");
		}
	}
}
