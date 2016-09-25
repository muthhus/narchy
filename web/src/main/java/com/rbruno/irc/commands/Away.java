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
        if (request.args.length >= 1) message = request.args[0];
		if (message.startsWith(":")) message = message.substring(1);
		request.client.setAwayMessage(message);
		if (request.client.getAwayMessage().isEmpty()) {
			request.connection.send(Reply.RPL_UNAWAY, request.client, ":You are no longer marked as being away");
		} else {
			request.connection.send(Reply.RPL_NOWAWAY, request.client, ":You have been marked as being away");
		}
	}
}
