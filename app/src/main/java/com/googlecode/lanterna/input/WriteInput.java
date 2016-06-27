package com.googlecode.lanterna.input;

import com.googlecode.lanterna.terminal.Terminal;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by me on 4/1/16.
 */
public class WriteInput {

    private final OutputStream outputStream;
    private final Terminal terminalEmulator;
    private boolean stop;

    public WriteInput(OutputStream outputStream, Terminal terminalEmulator) {
        this.outputStream = outputStream;
        this.terminalEmulator = terminalEmulator;
        this.stop = false;
    }

    public void start() {
        new Thread("InputWriter") {
            @Override
            public void run() {
                try {
                    while (!stop) {
                        KeyStroke keyStroke = terminalEmulator.pollInput();
                        if (keyStroke == null) {
                            Thread.sleep(1);
                        } else {
                            switch (keyStroke.getKeyType()) {
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
                } catch (IOException | InterruptedException e) {
                } finally {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                    }
                }
            }
        }.start();
    }

    private void writeCharacter(char character) throws IOException {
        outputStream.write(character);
        terminalEmulator.put(character);
    }

    private void flush() throws IOException {
        outputStream.flush();
        terminalEmulator.flush();
    }

    public void stop() {
        stop = true;
    }
}
