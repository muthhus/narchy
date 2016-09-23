package com.rbruno.irc.commands;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.reply.Reply;
import com.rbruno.irc.Channel;
import com.rbruno.irc.Client;
import com.rbruno.irc.Request;

public class Who extends Command {

	public Who() {
		super("WHO", 1);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		String target = request.getArgs()[0];
		if (target.startsWith("#") || target.startsWith("&")) {
			Channel channel = server.getChannel(target);
			for (Client client : channel.getClients())
				request.connection.send(Reply.RPL_WHOREPLY, request.getClient(), channel.id + ' ' + client.getUsername() + " * " + client.getHostname() + ' ' + client.id + " H+ :" + Client.getHopCount() + ' ' + client.getRealName());
			request.connection.send(Reply.RPL_ENDOFWHO, request.getClient(), target + " :End of /WHO list");
		}
	}

}
