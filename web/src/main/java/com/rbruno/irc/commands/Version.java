package com.rbruno.irc.commands;

import com.rbruno.irc.Config;
import com.rbruno.irc.IRCServer;
import com.rbruno.irc.Request;
import com.rbruno.irc.reply.Reply;

public class Version extends Command {

	public Version() {
		super("VERSION", 0);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
        if (request.args.length == 0) {
			request.connection.send(Reply.RPL_VERSION, request.client, IRCServer.getVersion() + ' ' + Config.getProperty("hostname"));
		} else {
			//TODO: Server
		}
	}

}
