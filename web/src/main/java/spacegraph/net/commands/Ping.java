package spacegraph.net.commands;

import spacegraph.net.Config;
import spacegraph.net.IRCServer;
import spacegraph.net.Request;

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
