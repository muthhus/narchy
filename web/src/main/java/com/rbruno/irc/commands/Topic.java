package com.rbruno.irc.commands;

import com.rbruno.irc.reply.Error;
import com.rbruno.irc.reply.Reply;
import com.rbruno.irc.Channel;
import com.rbruno.irc.Request;
import com.rbruno.irc.Channel.ChannelMode;
import com.rbruno.irc.IRCServer;

public class Topic extends Command {

	public Topic() {
		super("TOPIC", 1);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		Channel channel = server.getChannel(request.getArgs()[0]);
		if (channel == null) {
			request.connection.send(Error.ERR_NOSUCHCHANNEL, request.getClient(), request.getArgs()[0] + " :No such channel");
			return;
		}
		if (channel.getMode(ChannelMode.TOPIC) && !channel.checkOP(request.getClient())) {
			request.connection.send(Error.ERR_CHANOPRIVSNEEDED, request.getClient(), channel.id + " :You're not channel operator");
			request.connection.send(Reply.RPL_TOPIC, request.getClient(), channel.id + ' ' + channel.getTopic());
			return;
		}
		if (request.getArgs().length == 1) {
			request.connection.send(Reply.RPL_TOPIC, request.getClient(), channel.id + ' ' + channel.getTopic());
		} else {
			channel.setTopic(request.getArgs()[1]);
		}
	}

}
