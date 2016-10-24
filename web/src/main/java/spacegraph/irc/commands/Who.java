package spacegraph.irc.commands;

import spacegraph.irc.Channel;
import spacegraph.irc.Client;
import spacegraph.irc.IRCServer;
import spacegraph.irc.Request;
import spacegraph.irc.reply.Reply;

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