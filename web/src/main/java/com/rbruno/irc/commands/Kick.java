package com.rbruno.irc.commands;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.reply.Error;
import com.rbruno.irc.Channel;
import com.rbruno.irc.Client;
import com.rbruno.irc.Request;

public class Kick extends Command {

	public Kick() {
		super("KICK", 2);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		Channel channel = server.getChannel(request.getArgs()[0]);
		if (channel == null) {
			request.connection.send(Error.ERR_NOSUCHCHANNEL, request.getClient(), request.getArgs()[1] + " :No such channel");
			return;
		}
		if (!channel.checkOP(request.getClient()) && !request.getClient().isServerOP()) {
			request.connection.send(Error.ERR_CHANOPRIVSNEEDED, request.getClient(), request.getArgs()[1] + " :You're not channel operator");
			return;
		}
		Client target = server.getClient(request.getArgs()[1]);
		if (target == null) {
			request.connection.send(Error.ERR_NOSUCHNICK, request.getClient(), request.getArgs()[1] + " :No such nick");
			return;
		}
		if (!channel.isUserOnChannel(target)) {
			request.connection.send(Error.ERR_USERNOTINCHANNEL, request.getClient(), target.id + ' ' + channel.id + " :User is not on that channel");
			return;
		}
		// TODO Send kick msg to target
		String message = ":You have been kicked from the channel";
		if (request.getArgs().length >= 3) message = request.getArgs()[2];
		for (Client client : channel.getClients())
			client.connection.send(':' + request.getClient().getAbsoluteName() + " KICK " + channel.id + ' ' + target.id + ' ' + message);

		channel.removeClient(target);
		target.removeChannel(channel);

	}

}
