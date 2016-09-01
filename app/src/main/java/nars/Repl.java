package nars;

import nars.nar.Default;
import org.fusesource.jansi.AnsiConsole;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.history.history.MemoryHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

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
        NARLoop loop = nar.loop(20);


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



//ConsoleReader reader = new ConsoleReader();


    }

    public static void main(String[] args) throws IOException {
        new Repl(new Default());
    }
}
