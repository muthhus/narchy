package com.rbruno.irc.commands;

import java.io.IOException;

import com.rbruno.irc.reply.Error;
import com.rbruno.irc.Client;
import com.rbruno.irc.Request;

public class Nick extends Command {

	public Nick() {
		super("NICK", 1);
	}

	@Override
	public void run(Request request) throws IOException {
        if (request.server().isNick(request.args[0])) {
            request.connection.send(Error.ERR_NICKNAMEINUSE, "*", request.args[0] + " :Nickname is already in use");
			request.connection.close();
			return;
		}
		if (request.connection.getClient()!=null) {
            request.connection.send(Error.ERR_NICKNAMEINUSE, "*", request.args[0] + " :Nickname changing not yet supported");
			request.connection.close();
			return;
		}

        request.connection.setClient(new Client(request.connection, request.args[0]));
	}

}
