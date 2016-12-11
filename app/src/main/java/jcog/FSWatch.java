package jcog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.OVERFLOW;


public class FSWatch implements Runnable {

    static final Logger logger = LoggerFactory.getLogger(FSWatch.class);

    final WatchService watchService;
    private final WatchKey watchKey;
    private final Path path;
    private final Consumer<Path> onEvent;
    private Thread thread;

    public FSWatch(String path, Executor exe, Consumer<Path> onEvent) throws IOException {
        this(path, (t) -> exe.execute( () -> onEvent.accept(t)) );
    }

    public FSWatch(String path, Consumer<Path> onEvent) throws IOException {

        watchService = FileSystems.getDefault().newWatchService();

        this.path = Paths.get(path);

        watchKey = this.path.register(watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE
        );

        logger.info("start: {}", this.path);

        this.onEvent = onEvent;

        this.thread = new Thread(this, "FSWatch:" + path);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    boolean running;

    public void stop() {
        running = false;
        thread.stop();
        thread = null;
        logger.info("stop: {}", path);
    }

    @Override
    public void run() {

        if (running)
            throw new UnsupportedOperationException("already running");

        running = true;

        while (running) {

            WatchKey key;

            try {
                //System.out.println("Waiting for key to be signalled...");
                key = watchService.take();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
                continue;
            }

            List<WatchEvent<?>> eventList = key.pollEvents();
            //System.out.println("Process the pending events for the key: " + eventList.size());

            for (WatchEvent<?> genericEvent : eventList) {

                WatchEvent.Kind<?> eventKind = genericEvent.kind();
                //System.out.println("Event kind: " + eventKind);

                if (eventKind == OVERFLOW) {

                    continue; // pending events for loop
                }

                WatchEvent pathEvent = genericEvent;
                Path file = (Path) pathEvent.context();
                this.onEvent.accept(file);
            }

            boolean validKey = key.reset();
            //System.out.println("Key reset");
            //System.out.println("");

            if (!validKey) {
                //System.out.println("Invalid key");
                break; // infinite for loop
            }

        }
    }
}
