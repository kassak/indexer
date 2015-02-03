package com.github.kassak.indexer;

import java.io.IOException;
import java.nio.file.*;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

public class FSWatcher implements Runnable {
    FSWatcher() throws UnsupportedOperationException, IOException {
        watcher = FileSystems.getDefault().newWatchService();
    }

    public void registerRoot(Path path) throws IOException {
        path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                break;
            }

            for (WatchEvent<?> event: key.pollEvents()) {
                WatchEvent<Path> ev = asPathEvent(event);
                WatchEvent.Kind<?> kind = ev.kind();

                Path filename = ((Path)key.watchable()).resolve(ev.context());
                if(kind == ENTRY_CREATE) {
                    processNewEntry(filename);
                }
                else if(kind == ENTRY_DELETE) {
                    processDeleteEntry(filename);
                }
                else if(kind == ENTRY_MODIFY) {
                    processModifyEntry(filename);
                }
                else if(kind == OVERFLOW) {
                    //TODO:
                    log.warning("Overflow occurred");
                }
                else {
                    log.warning("Unknown event type");
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                key.cancel();
            }
        }
        try {
            close();
        } catch (IOException e) {
            log.warning("Failed to close");
        }
    }

    public void close() throws IOException {
        watcher.close();
    }


    private void processNewEntry(Path path) {
        log.fine("processNewEntry " + path.toString());
        //TODO:
    }

    private void processDeleteEntry(Path path) {
        log.fine("processDeleteEntry " + path.toString());
        //TODO:
    }

    private void processModifyEntry(Path path) {
        log.fine("processModifyEntry " + path.toString());
        //TODO:
    }

    @SuppressWarnings("unchecked")
    private static WatchEvent<Path> asPathEvent(WatchEvent<?> event) {
        return (WatchEvent<Path>)event;
    }
    private final WatchService watcher;
    private static final Logger log = Logger.getLogger(FSWatcher.class.getName());
}
