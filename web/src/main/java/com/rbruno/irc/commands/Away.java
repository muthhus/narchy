package com.rbruno.irc.commands;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.reply.Reply;
import com.rbruno.irc.Request;

public class Away extends Command {

	public Away() {
		super("AWAY", 0);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		String message = "";
		if (request.getArgs().length >= 1) message = request.getArgs()[0];
		if (message.startsWith(":")) message = message.substring(1);
		request.getClient().setAwayMessage(message);
		if (request.getClient().getAwayMessage().isEmpty()) {
			request.connection.send(Reply.RPL_UNAWAY, request.getClient(), ":You are no longer marked as being away");
		} else {
			request.connection.send(Reply.RPL_NOWAWAY, request.getClient(), ":You have been marked as being away");
		}
	}
}
