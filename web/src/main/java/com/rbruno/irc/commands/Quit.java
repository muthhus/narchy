package com.rbruno.irc.commands;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.Channel;
import com.rbruno.irc.Client;
import com.rbruno.irc.Request;

public class Quit extends Command {

	public Quit() {
		super("QUIT", 0);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		if (request.connection.isClient()) {
			request.connection.close();

			String message = "Leaving";
			if (request.getArgs().length != 0) message = request.getArgs()[0];

			for (Channel current : request.getClient().getChannels()) {
				current.removeClient(request.getClient());
				for (Client client : current.getClients())
                    client.connection.send(':' + request.getClient().getAbsoluteName() + " QUIT " + message);
			}
			// TODO: Notify all clients and servers
		} else {
			// TODO: netsplits
		}
	}

}
