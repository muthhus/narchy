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
        Channel channel = server.getChannel(request.args[0]);
		if (channel == null) {
			request.connection.send(Error.ERR_NOSUCHCHANNEL, request.client, request.args[0] + " :No such channel");
			return;
		}
		if (channel.getMode(ChannelMode.TOPIC) && !channel.checkOP(request.client)) {
			request.connection.send(Error.ERR_CHANOPRIVSNEEDED, request.client, channel.id + " :You're not channel operator");
			request.connection.send(Reply.RPL_TOPIC, request.client, channel.id + ' ' + channel.getTopic());
			return;
		}
        if (request.args.length == 1) {
			request.connection.send(Reply.RPL_TOPIC, request.client, channel.id + ' ' + channel.getTopic());
		} else {
            channel.setTopic(request.args[1]);
		}
	}

}
