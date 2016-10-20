package nars;

import nars.nar.Default;
import nars.util.Util;
import org.fusesource.jansi.AnsiConsole;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.history.history.MemoryHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.io.PrintStream;

/**
 * TODO
 * add volume +/- hotkeys that throttle output
 * and throttle CPU
 */
public class Repl {

    public Repl(NAR nar) throws IOException {


        TerminalBuilder tb = TerminalBuilder.builder();

//        rightPrompt = new AttributedStringBuilder()
//                .style(AttributedStyle.DEFAULT.background(AttributedStyle.RED))
//                .append(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
//                .append("\n")
//                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED | AttributedStyle.BRIGHT))
//                .append(LocalTime.now().format(new DateTimeFormatterBuilder()
//                        .appendValue(HOUR_OF_DAY, 2)
//                        .appendLiteral(':')
//                        .appendValue(MINUTE_OF_HOUR, 2)
//                        .toFormatter()))
//                .toAnsi();

        AnsiConsole.systemInstall();

        Terminal terminal = tb
                //.system(true)
                .encoding("UTF-8")
                .build();

//        String p = new AttributedStringBuilder()
//                .style(AttributedStyle.DEFAULT.background(AttributedStyle.YELLOW))
//                .append("foo")
//                .style(AttributedStyle.DEFAULT)
//                .append("@bar")
//                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
//                .append("\nbaz")
//                .style(AttributedStyle.DEFAULT)
//                .append("> ").toAnsi(terminal);


        LineReader reader = LineReaderBuilder.builder()
                .terminal(terminal)
                //.completer(completer)
                .history(new MemoryHistory())
                .appName("NARchy")
                .build();

        nar.logSummaryGT(new PrintStream(reader.getTerminal().output()), 0.1f);
        nar.next();
        NARLoop loop = nar.loop(20f);


        reader.setOpt(LineReader.Option.AUTO_FRESH_LINE);
        reader.setOpt(LineReader.Option.AUTO_LIST);


        while (true) {

            terminal.flush();

            String line = null;
            try {

                line = reader.readLine("> ");
                if (line == null)
                    break;

            } catch (UserInterruptException e) {
                // Ignore
                break;
            } catch (EndOfFileException e) {
                break;
            }


            line = line.trim();
            if (!line.isEmpty()) {

                //terminal.writer().println("\u001B[33m======>\u001B[0m\"" + line + "\"");

                try {
                    nar.input(line);
                } catch (Exception e) {
                    terminal.writer().println(e.toString());
                }

            }

            // If we input the special word then we will mask
            // the next line.
            //ParsedLine pl = reader.getParser().parse(line, 0);
            //String line = pl.line();

        }

        loop.stop();


    }

    public static void main(String[] args) throws IOException {
        //Default e = new Default();
        //new InterNAR2(e, 15000);

        Default d = new Default();
        //InterNAR2 ii = new InterNAR2(d, 15001);
        //ii.connect("tcp://localhost:15000");

        //Util.sleep(1000);
        //ii.send("test");

        new Repl(d);
    }

    /**
     * https://github.com/zeromq/jeromq/blob/master/src/test/java/guide/psenvpub.java
     */
    private static class InterNAR2 implements Runnable {

        private final NAR nar;
        private final ZMQ.Context context;
        private final ZMQ.Socket publisher;
        private final Thread thread;
        private ZMQ.Socket subscriber;

        public InterNAR2(NAR n, int port) {
            //super("tcp://0.0.0.0:" + port);

            context = ZMQ.context(1);
            publisher = context.socket(ZMQ.PUB);
            subscriber = context.socket(ZMQ.SUB);

            //publisher.bind("tcp://*:5563");
            publisher.bind("tcp://*:" + port);

            new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    // Write two messages, each with an envelope and content
                    publisher.sendMore("A");
                    publisher.send("We don't want to see this");
                    Util.sleep(100);
                    publisher.sendMore("B");
                    publisher.send("We would like to see this");
                    Util.sleep(100);
                }
            }).start();


            this.nar = n;

            thread = new Thread(this);
            thread.start();

        }

        public void connect(String url) {
            subscriber.connect(url);
        }

        @Override
        public void run() {


            // Prepare our context and subscriber



            subscriber.subscribe("B".getBytes(ZMQ.CHARSET));
            while (!Thread.currentThread().isInterrupted()) {

                // Read envelope with address
                String address = subscriber.recvStr();
                // Read message contents
                String contents = subscriber.recvStr();
                System.out.println(this + " " + address + " : " + contents);
            }
        }

        public void stop() {
            publisher.close();
            subscriber.close();
            context.term();
        }

        public void send(String x) {
            publisher.send(x);
        }

//        @Override
//        protected String receive(ZMQ.Socket sub) {
//            return sub.recvStr();
//        }
    }

}
