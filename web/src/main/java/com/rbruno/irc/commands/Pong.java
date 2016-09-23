package com.rbruno.irc.commands;


import com.rbruno.irc.Request;

public class Pong extends Command {

	public Pong() {
		super("PONG", 0);
	}

	@Override
	public void run(Request request) throws Exception {

	}
}
