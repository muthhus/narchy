package com.rbruno.irc.commands;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.reply.Error;
import com.rbruno.irc.reply.Reply;
import com.rbruno.irc.Channel;
import com.rbruno.irc.Channel.ChannelMode;
import com.rbruno.irc.Request;

public class Join extends Command {

	public Join() {
		super("JOIN", 1);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		// TODO: Check password
        String[] channels = request.args[0].split(",");
		for (String channelName : channels) {
			Channel channel = server.getChannel(channelName);
//			if (channel == null) {
//				if (!server.getConfig().getProperty("CreateOnJoin").equals("true")) {
//					request.connection.send(Error.ERR_NOSUCHCHANNEL, request.getClient(), channelName + " :No such channel");
//					continue;
//				}
//				if (!channelName.startsWith("#") && !channelName.startsWith("&")) continue;
//				String password = "";
//				if (request.getArgs().length >= 2) password = request.getArgs()[1];
//				channel = new Channel(request.getArgs()[0], password, false, this);
//				channel.addOP(request.getClient());
//				server.get(channel);
//			}
			if (channel!=null) { // && channel.getUserLimit() == -1 || channel.getUserLimit() > channel.getCurrentNumberOfUsers() || request.getClient().isServerOP()) {
                if (channel.checkPassword((request.args.length >= 2) ? request.args[1] : "")) {
					if (channel.getMode(ChannelMode.INVITE_ONLY) && !channel.isUserInvited(request.client) && !request.client.isServerOP()) {
						request.connection.send(Error.ERR_INVITEONLYCHAN, request.client, channel.id + " :Cannot join channel (+i)");
						continue;
					}
					channel.addClient(request.connection.getClient());
					request.connection.getClient().addChannel(channel);
					request.connection.send(Reply.RPL_TOPIC, request.client, channel.id + " :" + channel.getTopic());
				} else {
					request.connection.send(Error.ERR_BADCHANNELKEY, request.client, channel.id + " :Cannot join channel (+k)");
				}

			} else {
				request.connection.send(Error.ERR_CHANNELISFULL, request.client, channel.id + " :Cannot join channel (+l)");
			}

		}

	}
}
