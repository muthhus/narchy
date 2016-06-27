/*
 * This file is part of lanterna (http://code.google.com/p/lanterna/).
 *
 * lanterna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2010-2016 Martin
 */
package com.googlecode.lanterna.terminal;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.WriteInput;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author martin
 */
public class ShellTerminal {


//    public static void main(String[] args) throws InterruptedException, IOException {
//        //final Terminal rawTerminal = new TestTerminalFactory(args).createTerminal();
//        build(new DefaultVirtualTerminal());
//
//
//    }

    /** TODO make a wrapper which bundles the process with the virtual terminal
     * so that it can track execution state */
    public static DefaultVirtualTerminal build(int c, int r, String cmdPath) throws IOException {
        DefaultVirtualTerminal t = new DefaultVirtualTerminal(c, r);
        t.fore(TextColor.ANSI.WHITE);
        t.back(TextColor.ANSI.BLACK);
        build(t, cmdPath);
        return t;
    }


    public static <T extends Terminal> T build(T term, String cmdPath) throws IOException {
        //assume bash is available
        Process bashProcess = Runtime.getRuntime().exec(cmdPath, makeEnvironmentVariables());
        ProcessOutputReader stdout = new ProcessOutputReader(bashProcess.getInputStream(), term);
        ProcessOutputReader stderr = new ProcessOutputReader(bashProcess.getErrorStream(), term);
        WriteInput stdin = new WriteInput(bashProcess.getOutputStream(), term);
        stdin.start();
        stdout.start();
        stderr.start();



//        PrintStream ps = new PrintStream(bashProcess.getOutputStream());
//        ps.println("ls");
//        ps.flush();

//        new Thread(()->{
//            try {
//                int returnCode = bashProcess.waitFor();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        });



        //stdout.stop();
        //stderr.stop();
        //stdin.stop();
        //System.exit(returnCode);
        return term;
    }

    private static String[] makeEnvironmentVariables() {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("TERM", "xterm");
        return env.keySet().stream().map(key -> key + "=" + env.get(key)).toArray(String[]::new);
    }

    private static class ProcessOutputReader {

        private final InputStreamReader inputStreamReader;
        private final Terminal terminalEmulator;
        private boolean stop;
        final int updatePeriodMS = 50; //20hz

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
                                    terminalEmulator.put(buffer[i]);
                                }
                                terminalEmulator.flush();
                            }
                            else {
                                try {
                                    Thread.sleep(updatePeriodMS);
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

}
