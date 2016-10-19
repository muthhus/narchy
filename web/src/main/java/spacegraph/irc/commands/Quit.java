package spacegraph.irc.commands;

import spacegraph.irc.Channel;
import spacegraph.irc.Client;
import spacegraph.irc.Request;

public class Quit extends Command {

	public Quit() {
		super("QUIT", 0);
	}

	@Override
	public void run(Request request) throws java.io.IOException {

		if (request.connection.isClient()) {
			request.connection.close();

			String message = "Leaving";
            if (request.args.length != 0) message = request.args[0];

			for (Channel current : request.client.getChannels()) {
				current.removeClient(request.client);
				for (Client client : current.getClients())
					client.connection.send(':' + request.client.getAbsoluteName() + " QUIT " + message);
			}
			// TODO: Notify all clients and servers
		} else {
			// TODO: netsplits
		}
	}

}
