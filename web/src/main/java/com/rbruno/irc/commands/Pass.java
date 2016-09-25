package com.rbruno.irc.commands;

import com.rbruno.irc.Request;

public class Pass extends Command {

	public Pass() {
		super("PASS", 1);
	}

	@Override
	public void run(Request request) {
        request.connection.setConnectionPassword(request.args[0]);
	}

}
