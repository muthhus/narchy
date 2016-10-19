package spacegraph.irc.commands;

import spacegraph.irc.Config;
import spacegraph.irc.IRCServer;
import spacegraph.irc.Request;

public class Ping extends Command {

	public Ping() {
		super("PING", 1);
	}
	
	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		switch (request.connection.getType()){
		case CLIENT:
            request.connection.send(':' + Config.getProperty("hostname") + " PONG " + request.client.id);
			break;
		case LOGGIN_IN:
			break;
		case SERVER:
			//TODO Server
			break;
		}
	}

}
