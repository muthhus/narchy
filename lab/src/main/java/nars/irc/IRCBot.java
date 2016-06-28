package nars.irc;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public abstract class IRCBot {

    private final String server;
    public final String nick;
    private final String login;
    protected final String channel;
    boolean outputting = true;

    private static final Logger logger = LoggerFactory.getLogger(IRCBot.class);

    protected final BufferedWriter writer;

    public void setOutputting(boolean outputting) {
        this.outputting = outputting;
    }

    @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
    public IRCBot(String server, String nick, String channel) throws Exception {
        this(server, nick, nick.toLowerCase(), channel);
    }

    @SuppressWarnings("ConstructorNotProtectedInAbstractClass")
    public IRCBot(String server, String nick, String login, String channel) throws Exception {

        this.server = server;
        this.nick = nick;
        this.login = login;
        this.channel = channel;

        int port = 6667;

        logger.info("connecting {} {}", server, port);

        Socket socket = new Socket(server, port);

        writer = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream( )));
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream( )));

        // Log on to the server.
        logger.info("identifying {} {}", login, nick);

        writer.write("NICK " + nick + "\r\n");
        writer.write("USER " + login + " 8 * : " + nick + "\r\n");
        writer.flush();

        new Thread(() -> {

            // Join the channel.
            try {
                // Read lines from the server until it tells us we have connected.
                String line = null;
                while ((line = reader.readLine( )) != null) {
                    if (line.contains("004")) {
                        // We are now logged in.
                        break;
                    }
                    else if (line.contains("433")) {
                        System.out.println("Nickname is already in use.");
                        return;
                    }
                }

                logger.info("joining {}", channel);

                writer.write("JOIN " + channel + "\r\n");
                writer.flush();


                // Keep reading lines from the server.
                while ((line = reader.readLine( )) != null) {

                    try {
                        Message m = Message.parse(line);

                        switch (m.command) {
                            case "PING":
                                writer.write("PONG " + m.params.get(0) + "\r\n");
                                writer.flush();
                                break;
                            case "PRIVMSG":
                                //System.err.println(line);
                                logger.info("in: {}", line);
                                onMessage(m.params.get(0), m.nick, m.params.get(1));
                                break;
                            default:
                                logger.error("unparsed: {}", line);
                        }
                    }
                    catch (Exception e) {
                        logger.error("exception: {} from input: {}", e, line);
                    }
                }
            } catch (IOException e) {
                logger.error("connect: {}", e);
                e.printStackTrace();
            }

        }).start();
    }

    protected abstract void onMessage(String channel, String nick, String msg);

    protected /*synchronized */ boolean send(String channel, String message) {
        String x = "PRIVMSG " + channel + " :" + message + "\r\n";

        try {
            synchronized(writer) {
                writer.write(x);
                writer.flush();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    /** to default channel */
    public boolean send(String message) {
        return send(channel, message);
    }



    public static class Message extends HashMap<String,Object> {

        String prefix;
        String nick;
        String command;
        final ArrayList<String> params = new ArrayList<>();

        @Override
        public String toString() {
            return command + ", " + prefix + ", " + super.toString() + ", " + params;
        }

        public static Message parse(String line) {
            Message message = new Message();
            int position = 0;
            int nextspace = 0;
            // parsing!
            if (line.charAt(0) == '@') {
                String[] rawTags;

                nextspace = line.indexOf(' ');
                System.out.println(nextspace);
                if (nextspace == -1) {
                    return null;
                }

                rawTags = line.substring(1, nextspace).split(";");

                for (String tag : rawTags) {
                    String[] pair = tag.split("=");

                    if (pair.length == 2) {
                        message.put(pair[0], pair[1]);
                    } else {
                        message.put(pair[0], true);
                    }
                }
                position = nextspace + 1;
            }

            while (line.charAt(position) == ' ') {
                position++;
            }

            if (line.charAt(position) == ':') {
                nextspace = line.indexOf(' ', position);
                if (nextspace == -1) {
                    return null;
                }
                message.prefix = line.substring(position + 1, nextspace);
                position = nextspace + 1;

                while (line.charAt(position) == ' ') {
                    position++;
                }

                if ((message.prefix.length() > 1) && (message.prefix.indexOf('!')!=-1))
                    message.nick = message.prefix.substring(0, message.prefix.indexOf('!'));
            }

            nextspace = line.indexOf(' ', position);

            if (nextspace == -1) {
                if (line.length() > position) {
                    message.command = line.substring(position);
                }
                return message;
            }

            message.command = line.substring(position, nextspace);

            position = nextspace + 1;

            while (line.charAt(position) == ' ') {
                position++;
            }

            while (position < line.length()) {
                nextspace = line.indexOf(' ', position);

                if (line.charAt(position) == ':') {
                    String param = line.substring(position + 1);
                    message.params.add(param);
                    break;
                }

                if (nextspace != -1) {
                    String param = line.substring(position, nextspace);
                    message.params.add(param);
                    position = nextspace + 1;

                    while (line.charAt(position) == ' ') {
                        position++;
                    }
                    continue;
                }

                if (nextspace == -1) {
                    String param = line.substring(position);
                    message.params.add(param);
                    break;
                }
            }

            return message;
        }
    }
}
