package spacegraph.net.commands;

import spacegraph.net.Channel;
import spacegraph.net.Client;
import spacegraph.net.IRCServer;
import spacegraph.net.Request;
import spacegraph.net.reply.Reply;

public class Who extends Command {

	public Who() {
		super("WHO", 1);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
        String target = request.args[0];
		if (target.startsWith("#") || target.startsWith("&")) {
			Channel channel = server.getChannel(target);
			for (Client client : channel.getClients())
				request.connection.send(Reply.RPL_WHOREPLY, request.client, channel.id + ' ' + client.getUsername() + " * " + client.getHostname() + ' ' + client.id + " H+ :" + Client.getHopCount() + ' ' + client.getRealName());
			request.connection.send(Reply.RPL_ENDOFWHO, request.client, target + " :End of /WHO list");
		}
	}

}
