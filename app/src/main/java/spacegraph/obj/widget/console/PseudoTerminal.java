package spacegraph.obj.widget.console;

import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.ansi.ANSITerminal;
import com.googlecode.lanterna.terminal.ansi.UnixTerminal;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.jcraft.jsch.JSchException;
import com.jogamp.opengl.GL2;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.io.output.TeeOutputStream;
import spacegraph.SpaceGraph;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author martin
 */
public class PseudoTerminal {


    private static String[] makeEnvironmentVariables() {
        List<String> environment = new ArrayList<String>();
        Map<String, String> env = new TreeMap<String, String>(System.getenv());
        env.put("TERM", "xterm");   //Will this make bash detect us as a proper terminal??
        for(String key : env.keySet()) {
            environment.add(key + "=" + env.get(key));
        }
        return environment.toArray(new String[environment.size()]);
    }

    private static class ProcessOutputReader {

        private final InputStreamReader inputStreamReader;
        private final Terminal terminalEmulator;
        private boolean stop;

        public ProcessOutputReader(InputStream inputStream, Terminal terminalEmulator) {
            this.inputStreamReader = new InputStreamReader(inputStream, Charset.defaultCharset());
            this.terminalEmulator = terminalEmulator;
            this.stop = false;
        }

        private void start() {
            new Thread("OutputReader") {
                @Override
                public void run() {
                    try {
                        char[] buffer = new char[1024];
                        int readCharacters = inputStreamReader.read(buffer);
                        while(readCharacters != -1 && !stop) {
                            if(readCharacters > 0) {
                                for(int i = 0; i < readCharacters; i++) {
                                    terminalEmulator.putCharacter(buffer[i]);
                                }
                                terminalEmulator.flush();
                            }
                            else {
                                try {
                                    Thread.sleep(1);
                                }
                                catch(InterruptedException e) {
                                }
                            }
                            readCharacters = inputStreamReader.read(buffer);
                        }
                    }
                    catch(IOException e) {
                        e.printStackTrace();
                    }
                    finally {
                        try {
                            inputStreamReader.close();
                        }
                        catch(IOException e) {
                        }
                    }
                }
            }.start();
        }

        private void stop() {
            stop = true;
        }
    }

    private static class ProcessInputWriter {

        private final OutputStream outputStream;
        private final Terminal terminalEmulator;
        private boolean stop;

        public ProcessInputWriter(OutputStream outputStream, Terminal terminalEmulator) {
            this.outputStream = outputStream;
            this.terminalEmulator = terminalEmulator;
            this.stop = false;
        }

        private void start() {
            new Thread("InputWriter") {
                @Override
                public void run() {
                    try {
                        while(!stop) {
                            KeyStroke keyStroke = terminalEmulator.pollInput();
                            if(keyStroke == null) {
                                Thread.sleep(1);
                            }
                            else {
                                switch(keyStroke.getKeyType()) {
                                    case Character:
                                        writeCharacter(keyStroke.getCharacter());
                                        break;
                                    case Enter:
                                        writeCharacter('\n');
                                        break;
                                    case Backspace:
                                        writeCharacter('\b');
                                        break;
                                    case Tab:
                                        writeCharacter('\t');
                                        break;
                                    default:
                                }
                                flush();
                            }
                        }
                    }
                    catch(IOException e) {
                    }
                    catch(InterruptedException e) {
                    }
                    finally {
                        try {
                            outputStream.close();
                        }
                        catch(IOException e) {
                        }
                    }
                }
            }.start();
        }

        private void writeCharacter(char character) throws IOException {
            outputStream.write(character);
            terminalEmulator.putCharacter(character);
        }

        private void flush() throws IOException {
            outputStream.flush();
            terminalEmulator.flush();
        }

        private void stop() {
            stop = true;
        }
    }


//    /**
//     * Created by me on 4/1/16.
//     */
//    public static class WriteInput {
//
//        private final OutputStream outputStream;
//        private final Terminal terminalEmulator;
//        private boolean stop;
//
//        public WriteInput(OutputStream outputStream, Terminal terminalEmulator) {
//            this.outputStream = outputStream;
//            this.terminalEmulator = terminalEmulator;
//            this.stop = false;
//        }
//
//        public void start() {
//            new Thread("InputWriter") {
//                @Override
//                public void run() {
//                    try {
//                        while (!stop) {
//                            KeyStroke keyStroke = terminalEmulator.pollInput();
//                            if (keyStroke == null) {
//                                Thread.sleep(1);
//                            } else {
//                                switch (keyStroke.getKeyType()) {
//                                    case Character:
//                                        writeCharacter(keyStroke.getCharacter());
//                                        break;
//                                    case Enter:
//                                        writeCharacter('\n');
//                                        break;
//                                    case Backspace:
//                                        writeCharacter('\b');
//                                        break;
//                                    case Tab:
//                                        writeCharacter('\t');
//                                        break;
//                                    default:
//                                }
//                                flush();
//                            }
//                        }
//                    } catch (IOException | InterruptedException e) {
//                    } finally {
//                        try {
//                            outputStream.close();
//                        } catch (IOException e) {
//                        }
//                    }
//                }
//            }.start();
//        }
//
//        private void writeCharacter(char character) throws IOException {
//            outputStream.write(character);
//            terminalEmulator.putCharacter(character);
//        }
//
//        private void flush() throws IOException {
//            outputStream.flush();
//            terminalEmulator.flush();
//        }
//
//        public void stop() {
//            stop = true;
//        }
//    }
//
//    /**
//     *
//     * @author martin
//     */
//    public static class ShellTerminal {
//
//
////    public static void main(String[] args) throws InterruptedException, IOException {
////        //final Terminal rawTerminal = new TestTerminalFactory(args).createTerminal();
////        build(new DefaultVirtualTerminal());
////
////
////    }
//
////        /** TODO make a wrapper which bundles the process with the virtual terminal
////         * so that it can track execution state */
////        public static DefaultVirtualTerminal build(int c, int r, String cmdPath) throws IOException {
////            DefaultVirtualTerminal t = new DefaultVirtualTerminal(c, r);
////            t.fore(TextColor.ANSI.WHITE);
////            t.back(TextColor.ANSI.BLACK);
////            build(t, cmdPath);
////            return t;
////        }
//
//
//        public static <T extends Terminal> T build(T term, String cmdPath) throws IOException {
//            //assume bash is available
//            Process bashProcess = Runtime.getRuntime().exec(cmdPath, makeEnvironmentVariables());
//            ProcessOutputReader stdout = new ProcessOutputReader(bashProcess.getInputStream(), term);
//            ProcessOutputReader stderr = new ProcessOutputReader(bashProcess.getErrorStream(), term);
//            WriteInput stdin = new WriteInput(bashProcess.getOutputStream(), term);
//            stdin.start();
//            stdout.start();
//            stderr.start();
//
//
//
////        PrintStream ps = new PrintStream(bashProcess.getOutputStream());
////        ps.println("ls");
////        ps.flush();
//
////        new Thread(()->{
////            try {
////                int returnCode = bashProcess.waitFor();
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
////        });
//
//
//
//            //stdout.stop();
//            //stderr.stop();
//            //stdin.stop();
//            //System.exit(returnCode);
//            return term;
//        }
//
//        private static String[] makeEnvironmentVariables() {
//            Map<String, String> env = new HashMap<>(System.getenv());
//            env.put("TERM", "xterm");
//            return env.keySet().stream().map(key -> key + "=" + env.get(key)).toArray(String[]::new);
//        }
//
//        private static class ProcessOutputReader {
//
//            private final InputStreamReader inputStreamReader;
//            private final Terminal terminalEmulator;
//            private boolean stop;
//            final int updatePeriodMS = 50; //20hz
//
//            public ProcessOutputReader(InputStream inputStream, Terminal terminalEmulator) {
//                this.inputStreamReader = new InputStreamReader(inputStream, Charset.defaultCharset());
//                this.terminalEmulator = terminalEmulator;
//                this.stop = false;
//            }
//
//            private void start() {
//                new Thread("OutputReader") {
//                    @Override
//                    public void run() {
//                        try {
//                            char[] buffer = new char[1024];
//                            int readCharacters = inputStreamReader.read(buffer);
//                            while(readCharacters != -1 && !stop) {
//                                if(readCharacters > 0) {
//                                    for(int i = 0; i < readCharacters; i++) {
//                                        terminalEmulator.putCharacter(buffer[i]);
//                                    }
//                                    terminalEmulator.flush();
//                                }
//                                else {
//                                    try {
//                                        Thread.sleep(updatePeriodMS);
//                                    }
//                                    catch(InterruptedException e) {
//                                    }
//                                }
//                                readCharacters = inputStreamReader.read(buffer);
//                            }
//                        }
//                        catch(IOException e) {
//                            e.printStackTrace();
//                        }
//                        finally {
//                            try {
//                                inputStreamReader.close();
//                            }
//                            catch(IOException e) {
//                            }
//                        }
//                    }
//                }.start();
//            }
//
//            private void stop() {
//                stop = true;
//            }
//        }
//
//    }

}