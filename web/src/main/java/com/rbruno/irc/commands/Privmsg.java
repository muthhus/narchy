package com.rbruno.irc.commands;

import com.rbruno.irc.Channel;
import com.rbruno.irc.Channel.ChannelMode;
import com.rbruno.irc.Client;
import com.rbruno.irc.IRCServer;
import com.rbruno.irc.Request;
import com.rbruno.irc.reply.Error;

public class Privmsg extends Command {

	public Privmsg() {
		super("PRIVMSG", 2);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
        for (String reciver : request.args[0].split(",")) {
			if (reciver.startsWith("$")) {
				// TODO: Server
			} else if (reciver.startsWith("#") || reciver.startsWith("&")) {
				Channel channel = server.getChannel(reciver);
				if (channel == null) {
					request.connection.send(Error.ERR_NOSUCHCHANNEL, request.client, reciver + " :No such channel");
					return;
				}
				if (!request.client.getChannels().contains(channel) && channel.getMode(ChannelMode.NO_MESSAGE_BY_OUTSIDE)) {
					request.connection.send(Error.ERR_CANNOTSENDTOCHAN, request.client, channel.id + " :Cannot send to channel");
					return;
				}
				if (!channel.hasVoice(request.client)) {
					request.connection.send(Error.ERR_CANNOTSENDTOCHAN, request.client, channel.id + " :Cannot send to channel");
					return;
				}
				channel.sendMessage(request.client, request.args[1]);

			} else if (reciver!=null) {
				Client client = server.getClient(reciver);
				if (client != null) {
					client.connection.send(':' + request.client.getAbsoluteName() + " PRIVMSG " + client.id + ' ' + request.args[1]);
				} else {
					request.client.connection.send(Error.ERR_NOSUCHNICK, client, reciver + " :No such nick");
				}
			}
		}
	}

}
