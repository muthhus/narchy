package com.rbruno.irc.commands;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.Channel;
import com.rbruno.irc.Client;
import com.rbruno.irc.Request;

public class Part extends Command {

	public Part() {
		super("PART", 1);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
        String[] channels = request.args[0].split(",");
		for (String channelName : channels) {
			Channel channel = server.getChannel(channelName);
			String message = "Leaving";
            if (request.args.length != 0)
                message = request.args[0];
			for (Client client : channel.getClients())
				client.connection.send(':' + request.client.getAbsoluteName() + " PART " + message);
			channel.removeClient(request.connection.getClient());
			request.connection.getClient().removeChannel(channel);


		}

	}

}