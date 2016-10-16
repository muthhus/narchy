package com.rbruno.irc.commands;

import com.rbruno.irc.Channel;
import com.rbruno.irc.Client;
import com.rbruno.irc.IRCServer;
import com.rbruno.irc.Request;
import com.rbruno.irc.reply.Error;
import com.rbruno.irc.reply.Reply;

public class Invite extends Command {

	public Invite() {
		super("INVITE", 2);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
        Channel channel = server.getChannel(request.args[1]);
		if (channel == null) {
			request.connection.send(Error.ERR_NOSUCHCHANNEL, request.client, request.args[1] + " :No such channel");
			return;
		}
		if (!channel.checkOP(request.client) && !request.client.isServerOP()) {
			request.connection.send(Error.ERR_CHANOPRIVSNEEDED, request.client, request.args[1] + " :You're not channel operator");
			return;
		}
        Client target = server.getClient(request.args[0]);
		if (target == null) {
			request.connection.send(Error.ERR_NOSUCHNICK, request.client, request.args[1] + " :No such nick");
			return;
		}
		channel.inviteUser(target);
		request.connection.send(Reply.RPL_INVITING, request.client, target.id + ' ' + channel.id);
		target.connection.send(':' + request.client.getAbsoluteName() + " INVITE " + target.id + ' ' + channel.id);
	}

}
