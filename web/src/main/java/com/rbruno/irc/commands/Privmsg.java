package com.rbruno.irc.commands;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.reply.Error;
import com.rbruno.irc.Channel;
import com.rbruno.irc.Channel.ChannelMode;
import com.rbruno.irc.Client;
import com.rbruno.irc.Request;

public class Privmsg extends Command {

	public Privmsg() {
		super("PRIVMSG", 2);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		for (String reciver : request.getArgs()[0].split(",")) {
			if (reciver.startsWith("$")) {
				// TODO: Server
			} else if (reciver.startsWith("#") || reciver.startsWith("&")) {
				Channel channel = server.getChannel(reciver);
				if (channel == null) {
					request.connection.send(Error.ERR_NOSUCHCHANNEL, request.getClient(), reciver + " :No such channel");
					return;
				}
				if (!request.getClient().getChannels().contains(channel) && channel.getMode(ChannelMode.NO_MESSAGE_BY_OUTSIDE)) {
					request.connection.send(Error.ERR_CANNOTSENDTOCHAN, request.getClient(), channel.id + " :Cannot send to channel");
					return;
				}
				if (!channel.hasVoice(request.getClient())) {
					request.connection.send(Error.ERR_CANNOTSENDTOCHAN, request.getClient(), channel.id + " :Cannot send to channel");
					return;
				}
				channel.sendMessage(request.getClient(), request.getArgs()[1]);

			} else {
				Client client = server.getClient(reciver);
				if (client != null) {
					client.connection.send(':' + request.getClient().getAbsoluteName() + " PRIVMSG " + client.id + ' ' + request.getArgs()[1]);
				} else {
                    request.getClient().connection.send(Error.ERR_NOSUCHNICK, client, reciver + " :No such nick");
				}
			}
		}
	}

}
