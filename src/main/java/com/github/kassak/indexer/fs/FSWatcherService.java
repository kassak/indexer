package com.github.kassak.indexer.fs;

import com.github.kassak.indexer.utils.ThreadService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
    Service which performs registration of root to events service in
    separate thread.
*/
public class FSWatcherService extends ThreadService implements IFSWatcherService {
    /**
        Creates new service

        @param processor filesystem events processor
        @param queueSize size of registration queue
    */
    public FSWatcherService(@NotNull IFSEventsProcessor processor, int queueSize) {
        queue = new ArrayBlockingQueue<WatcherRegistrationTask>(queueSize);
        eventsService = new FSEventsService(processor);
    }

    @Override
    public void startService() throws Exception {
        //TODO:
        eventsService.startService();
        super.startService();
    }

    @Override
    public void stopService() throws Exception {
        //TODO:
        super.stopService();
        eventsService.stopService();
    }

    @Override
    public boolean isRunning() {
        //TODO:
        return super.isRunning();
    }

    @Override
    public boolean waitFinished(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        //TODO:
        return super.waitFinished(timeout, unit);
    }

    @Override
    public void run() {
        while(!Thread.currentThread().isInterrupted()) {
            try {
                final WatcherRegistrationTask task = queue.take();
                if(task.register)
                    eventsService.registerRoot(task.path);
                else
                    eventsService.unregisterRoot(task.path);
            } catch (InterruptedException e) {
                if(log.isLoggable(Level.FINE))
                    log.fine("Interrupted while waiting for file");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.log(Level.WARNING, "Task processing failed", e);
            }
        }
    }

    @Override
    public void registerRoot(@NotNull Path path) throws IOException {
        if(log.isLoggable(Level.FINE))
            log.fine("Registering " + path);
        queue.add(new WatcherRegistrationTask(path, true));
    }

    @Override
    public void unregisterRoot(@NotNull Path path) throws IOException {
        if(log.isLoggable(Level.FINE))
            log.fine("Unregistering " + path);
        queue.add(new WatcherRegistrationTask(path, false));
    }

    private final BlockingQueue<WatcherRegistrationTask> queue;
    private final FSEventsService eventsService;
    private final Logger log = Logger.getLogger(FSWatcherService.class.getName());
}
