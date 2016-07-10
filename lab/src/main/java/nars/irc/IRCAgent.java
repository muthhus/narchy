package nars.irc;

import nars.index.TermIndex;
import nars.inter.InterNAR;
import nars.nal.Tense;
import nars.nar.Terminal;
import nars.term.Compound;
import nars.time.RealtimeMSClock;
import nars.util.data.random.XorShift128PlusRandom;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 7/10/16.
 */
public class IRCAgent extends IRCBot {

    final Terminal terminal = new Terminal(16, new XorShift128PlusRandom(1), new RealtimeMSClock());

    static boolean running = true;

    private final InterNAR inter;

    public IRCAgent(String server, String nick, String channel, int udpPort) throws Exception {
        super(server, nick, channel);

        terminal.log();

        terminal.onExec(new TermProcedure("say") {
            @Override public @Nullable Object function(Compound arguments, TermIndex i) {
                send(channel, arguments.toString());
                return null;
            }
        });

        inter = new InterNAR(terminal, (short)udpPort );
        inter.broadcastPriorityThreshold = 0.5f; //lower threshold

        terminal.believe("connect(\"" + server + "\").", Tense.Present, 1f, 0.9f);
    }


    @Override
    protected void onMessage(String channel, String nick, String msg) {

        terminal.believe(
                "irc(\"" + channel + "\",\"" + nick + "\",\"" + msg + "\")",
                Tense.Present, 1f, 0.9f );

    }

    public static void main(String[] args) throws Exception {
        //while (running) {
            try {
                new IRCAgent(
                        "irc.freenode.net",
                        //"localhost",
                        "NARchy", "#netention", 11001);
            } catch (Exception e) {
                e.printStackTrace();

            }
        //}
    }
}
