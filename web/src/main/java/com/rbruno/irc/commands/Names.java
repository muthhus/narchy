package com.rbruno.irc.commands;

import com.rbruno.irc.Channel;
import com.rbruno.irc.Client;
import com.rbruno.irc.IRCServer;
import com.rbruno.irc.Request;
import com.rbruno.irc.reply.Reply;

import java.io.IOException;
import java.util.ArrayList;

public class Names extends Command {

	public Names() {
		super("NAMES", 0);
	}

	@Override
	public void run(Request request) throws IOException {
		IRCServer server = request.server();
        if (request.args.length == 0) {
			server.forEachChannel(current -> {
                String message = current.id + " :";
				ArrayList<Client> clients = current.getClients();
				for (Client client : clients) {
					message = message + (current.checkOP(client) || client.isServerOP() ? "@" : "+") + client.id + " ";
				}
				try {
					request.connection.send(Reply.RPL_NAMREPLY, request.client, message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} else {
            String[] stringChannels = request.args[0].split(",");
			for (String current : stringChannels) {
				Channel channel = server.getChannel(current);
                String message = channel.id + " :";
				ArrayList<Client> clients = channel.getClients();
				for (Client client : clients) {
					message = message + (channel.checkOP(client) || client.isServerOP() ? "@" : "+") + client.id + " ";
				}
				request.connection.send(Reply.RPL_NAMREPLY, request.client, message);
			}
		}
	}

}
