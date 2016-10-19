package spacegraph.irc.commands;

import spacegraph.irc.Channel;
import spacegraph.irc.Client;
import spacegraph.irc.IRCServer;
import spacegraph.irc.Request;
import spacegraph.irc.reply.Reply;

import java.io.IOException;

public class List extends Command {

	public List() {
		super("LIST", 0);
	}

	@Override
	public void run(Request request) throws IOException { IRCServer server = request.server();
		Client client = request.client;
        if (request.args.length == 0) {

			request.send(Reply.RPL_LISTSTART, client, "Channel :Users Name");
			server.forEachChannel(current -> {
				try {
					request.send(Reply.RPL_LIST, client, current.id + ' ' + current.getCurrentNumberOfUsers() + " :" + current.getTopic());
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			request.send(Reply.RPL_LISTEND, client, ":End of /LIST");
		} else {
            String[] stringChannels = request.args[0].split(",");
			request.send(Reply.RPL_LISTSTART, client, "Channel :Users  Name");
			for (String current : stringChannels) {
				Channel channel = server.getChannel(current);
				request.send(Reply.RPL_LIST, client, channel.id + ' ' + channel.getCurrentNumberOfUsers() + " :" + channel.getTopic());
			}
			request.send(Reply.RPL_LISTEND, client, " :End of /LIST");
		}
	}

}
