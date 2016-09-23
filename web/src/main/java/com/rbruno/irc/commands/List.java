package com.rbruno.irc.commands;

import java.io.IOException;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.reply.Reply;
import com.rbruno.irc.Channel;
import com.rbruno.irc.Client;
import com.rbruno.irc.Request;

public class List extends Command {

	public List() {
		super("LIST", 0);
	}

	@Override
	public void run(Request request) throws IOException { IRCServer server = request.server();
		Client client = request.getClient();
		if (request.getArgs().length == 0) {

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
			String[] stringChannels = request.getArgs()[0].split(",");
			request.send(Reply.RPL_LISTSTART, client, "Channel :Users  Name");
			for (String current : stringChannels) {
				Channel channel = server.getChannel(current);
				request.send(Reply.RPL_LIST, client, channel.id + ' ' + channel.getCurrentNumberOfUsers() + " :" + channel.getTopic());
			}
			request.send(Reply.RPL_LISTEND, client, " :End of /LIST");
		}
	}

}
