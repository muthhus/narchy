package com.rbruno.irc.commands;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.reply.Error;
import com.rbruno.irc.reply.Reply;
import com.rbruno.irc.Channel;
import com.rbruno.irc.Client;
import com.rbruno.irc.Request;

public class Invite extends Command {

	public Invite() {
		super("INVITE", 2);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		Channel channel = server.getChannel(request.getArgs()[1]);
		if (channel == null) {
			request.connection.send(Error.ERR_NOSUCHCHANNEL, request.getClient(), request.getArgs()[1] + " :No such channel");
			return;
		}
		if (!channel.checkOP(request.getClient()) && !request.getClient().isServerOP()) {
			request.connection.send(Error.ERR_CHANOPRIVSNEEDED, request.getClient(), request.getArgs()[1] + " :You're not channel operator");
			return;
		}
		Client target = server.getClient(request.getArgs()[0]);
		if (target == null) {
			request.connection.send(Error.ERR_NOSUCHNICK, request.getClient(), request.getArgs()[1] + " :No such nick");
			return;
		}
		channel.inviteUser(target);
		request.connection.send(Reply.RPL_INVITING, request.getClient(), target.id + ' ' + channel.id);
		target.connection.send(':' + request.getClient().getAbsoluteName() + " INVITE " + target.id + ' ' + channel.id);
	}

}
