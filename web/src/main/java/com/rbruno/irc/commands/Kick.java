package com.rbruno.irc.commands;

import com.rbruno.irc.Channel;
import com.rbruno.irc.Client;
import com.rbruno.irc.IRCServer;
import com.rbruno.irc.Request;
import com.rbruno.irc.reply.Error;

public class Kick extends Command {

	public Kick() {
		super("KICK", 2);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
        Channel channel = server.getChannel(request.args[0]);
		if (channel == null) {
			request.connection.send(Error.ERR_NOSUCHCHANNEL, request.client, request.args[1] + " :No such channel");
			return;
		}
		if (!channel.checkOP(request.client) && !request.client.isServerOP()) {
			request.connection.send(Error.ERR_CHANOPRIVSNEEDED, request.client, request.args[1] + " :You're not channel operator");
			return;
		}
        Client target = server.getClient(request.args[1]);
		if (target == null) {
			request.connection.send(Error.ERR_NOSUCHNICK, request.client, request.args[1] + " :No such nick");
			return;
		}
		if (!channel.isUserOnChannel(target)) {
			request.connection.send(Error.ERR_USERNOTINCHANNEL, request.client, target.id + ' ' + channel.id + " :User is not on that channel");
			return;
		}
		// TODO Send kick msg to target
		String message = ":You have been kicked from the channel";
        if (request.args.length >= 3) message = request.args[2];
		for (Client client : channel.getClients())
			client.connection.send(':' + request.client.getAbsoluteName() + " KICK " + channel.id + ' ' + target.id + ' ' + message);

		channel.removeClient(target);
		target.removeChannel(channel);

	}

}
