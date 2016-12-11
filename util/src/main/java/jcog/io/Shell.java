package jcog.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Created by me on 5/22/16.
 */
public class Shell {

    private static final Logger logger = LoggerFactory.getLogger(Shell.class);

    public final Process proc;
    public final PrintWriter writer;
    private final StreamGobbler reader;
    //private final StreamGobbler errReader;
    public long lastOutput;


    static final Executor exe = Executors.newSingleThreadExecutor();

    public Shell(String... cmd) throws IOException {
        this.proc = new ProcessBuilder(cmd).redirectErrorStream(true).start();

        this.writer = new PrintWriter(new OutputStreamWriter(this.proc.getOutputStream(), "UTF-8"), true);




//        this.errReader = new StreamGobbler(proc.getErrorStream(), (e) -> {
//            logger.info(e);
//        });
//        errReader.setName("errr");


        this.reader = new StreamGobbler(proc.getInputStream(), s -> {
            if (!s.isEmpty())
                exe.execute(() -> readln(s));
        });
        reader.setName("read");


        //errReader.start();
        reader.start();

        logger.info("started"/*, proc.getPid()*/);

    }

    protected void readln(String line) {
        logger.info("OUT: {}", line);
        lastOutput = System.currentTimeMillis();
    }

    public void println(String line) {

        logger.trace(" IN: {}", line);

        writer.println(line);


    }

    public void stop() {

        if (writer != null) {
            writer.close();

            if (proc != null) {
                proc.destroy();
            }

            reader.stop();
        }


    }

//    public static void exec(String command) {
//        Shell.get().println(command);
//    }
//
//    public static Shell get() {
//        if (Shell.rootShell == null) {
//            while (Shell.rootShell == null) {
//                try {
//                    Shell.rootShell = new Shell("su"); //Open with Root Privileges
//                } catch (IOException e) {
//                }
//            }
//        }
//        return Shell.rootShell;
//    }
    //private static Shell rootShell = null;

    static class StreamGobbler extends Thread {
        private final Consumer<String> eachLine;
        InputStream is;

        private StreamGobbler(InputStream is, Consumer<String> eachLine) {
            this.is = is;
            this.eachLine = eachLine;
        }

        @Override
        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line = null;
                while ((line = br.readLine()) != null) {
                    eachLine.accept(line);
                    //System.out.println(type + "> " + line);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }
}
