package com.rbruno.irc.commands;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.Config;
import com.rbruno.irc.reply.Error;
import com.rbruno.irc.reply.Reply;
import com.rbruno.irc.Client.ClientMode;
import com.rbruno.irc.Request;

public class Oper extends Command {

	public Oper() {
		super("OPER", 2);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		if (Config.getProperty("DisableOps").equals("true")) {
			request.connection.send(Error.ERR_NOOPERHOST, request.connection.getClient(), ":No O-lines for your host");
			return;
		}
		if (server.getConfig().checkOpPassword(request.getArgs()[0], request.getArgs()[1])) {
			request.getClient().setMode(ClientMode.OPERATOR, true, request.getClient());
			request.connection.send(Reply.RPL_YOUREOPER, request.connection.getClient(), ":You are now an IRC operator");
		} else {
			request.connection.send(Error.ERR_PASSWDMISMATCH, request.connection.getClient(), ":Password incorrect");
		}
	}
}
