package com.rbruno.irc.commands;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.Config;
import com.rbruno.irc.reply.Reply;
import com.rbruno.irc.Channel;
import com.rbruno.irc.Client;
import com.rbruno.irc.Request;

public class Whois extends Command {

	public Whois() {
		super("WHOIS", 2);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		for (String current : request.getArgs()[1].split(",")) {
			Client target = server.getClient(current);
			request.connection.send(Reply.RPL_WHOISUSER, request.getClient(), target.id + ' ' + target.getUsername() + ' ' + Config.getProperty("hostname") + " * :" + target.getRealName());
			// TODO Whois server
			if (target.isServerOP()) request.connection.send(Reply.RPL_WHOISOPERATOR, request.getClient(), target.id + " :is an IRC operator");
			if (!target.getAwayMessage().isEmpty()) request.connection.send(Reply.RPL_AWAY, request.getClient(), target.id + " :" + target.getAwayMessage());
			//request.getConnection().send(Reply.RPL_WHOISIDLE, request.getClient(), target.getNickname() + " 0 :seconds idle");
			request.connection.send(Reply.RPL_ENDOFWHOIS, request.getClient(), target.id + " :End of /WHOIS list");
			String channels = "";
			for (Channel channel : target.getChannels()) {
				if (channel.checkOP(target) || target.isServerOP()) {
                    channels = channels + '@' + channel.id + ' ';
				} else if (channel.hasVoice(target)) {
                    channels = channels + '+' + channel.id + ' ';
				} else {
                    channels = channels + channel.id + ' ';
				}
			}
			request.connection.send(Reply.RPL_WHOISCHANNELS, request.getClient(), target.id + " :" + channels);
		}

	}

}
