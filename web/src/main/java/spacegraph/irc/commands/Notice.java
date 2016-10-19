package spacegraph.irc.commands;

import spacegraph.irc.Client;
import spacegraph.irc.IRCServer;
import spacegraph.irc.Request;
import spacegraph.irc.reply.Error;

public class Notice extends Command {

	public Notice() {
		super("NOTICE", 2);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
        Client client = server.getClient(request.args[0]);
		if (client != null) {
			client.connection.send(':' + request.client.getAbsoluteName() + " NOTICE " + client.id + ' ' + request.args[1]);
		} else {
			request.client.connection.send(Error.ERR_NOSUCHNICK, client, request.args[0] + " :No such nick");
		}
	}
}
