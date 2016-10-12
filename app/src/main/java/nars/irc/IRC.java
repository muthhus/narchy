package nars.irc;

import org.eclipse.collections.impl.factory.Iterables;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.pircbotx.output.OutputIRC;

import java.io.IOException;

/**
 * Created by me on 10/12/16.
 */
public class IRC extends ListenerAdapter {

    public final PircBotX irc;

//    @Override
//    public void onGenericMessage(GenericMessageEvent event) {
//        //When someone says ?helloworld respond with "Hello World"
//        if (event.getMessage().startsWith("?helloworld"))
//            event.respond("Hello world!");
//    }

    public IRC(String nick, String server, String... channels) throws IOException, IrcException {
        this(new Configuration.Builder()
                .setName(nick)
                .setRealName(nick)
                .setLogin("root")
                .setVersion("unknown")
                .addServer(server)
                .addAutoJoinChannels(Iterables.mList(channels))
                .setAutoReconnect(true)
                .setAutoNickChange(true)
        );
    }

    public IRC(Configuration.Builder cb)  {

        cb.addListener(this);

        this.irc = new PircBotX(cb.buildConfiguration());

    }

    public final IRC start() throws IOException, IrcException {
        irc.startBot();
        return this;
    }

    public final void stop() {
        irc.stopBotReconnect();
    }

    public void send(String[] channels, String message) {
        if (irc.isConnected()) {
            OutputIRC out = irc.send();
            for (String c : channels) {
                out.message(c, message);
            }
        } else {
            //.?
        }
    }

    public static void main(String[] args) throws Exception {

        IRC bot = new IRC("experiment1", "irc.freenode.net", "#123xyz");

    }
}
