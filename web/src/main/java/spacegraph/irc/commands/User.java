package spacegraph.irc.commands;

import spacegraph.irc.Client;
import spacegraph.irc.Config;
import spacegraph.irc.IRCServer;
import spacegraph.irc.Request;
import spacegraph.irc.reply.Reply;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class User extends Command {

    final static AtomicInteger serial = new AtomicInteger(0);

    public User() {
        super("USER", 4);
    }

    @Override
    public void run(Request request) throws IOException {
        IRCServer server = request.server();

        switch (request.connection.getType()) {
            case LOGGIN_IN:
                //request.connection.close();
                break;
            case CLIENT:
                Client client = request.client;
                if (client == null) {
                    client = new Client(request.connection, "Anoynmous" + serial.incrementAndGet());
                }
                client.setUsername(request.args[0]);
                String hostname = request.server().hostname;
                client.setHostname(hostname);
                client.setServername(hostname);
                client.setRealName(request.args[3].substring(1));
                request.connection.setClient(client);
                request.connection.send(1, client.id, ":Connected to " + Config.getProperty("hostname") + " on " + client.id);
                request.connection.send(Reply.RPL_LUSERCLIENT, client, ":Online " + server.getClientCount() + " users, " + server.getInvisibleClientCount() + " invisible");
                request.connection.send(Reply.RPL_LUSEROP, client, server.getOps() + " :operator(s) online");
                //request.getConnection().send(Reply.RPL_LUSERUNKNOWN, client, "0 :unknown connection(s)");
                //request.connection.send(Reply.RPL_LUSERCHANNELS, client, server.getNonSecretChannels() + " :channels formed");
                request.connection.send(Reply.RPL_LUSERME, client, ":" + server.getClientCount() + " clients and 1 servers");
                server.sendMOTD(client);
                server.addChannel(client);
                break;
            case SERVER:
                // TODO Server
                break;
        }

    }
}
