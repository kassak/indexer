package com.github.kassak.indexer.fs;

import com.github.kassak.indexer.utils.IService;
import com.github.kassak.indexer.utils.Services;
import com.github.kassak.indexer.utils.ThreadService;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
    Service which performs registration of root to events service in
    separate thread.
*/
public class FSWatcherService implements Runnable, IService {
    /**
        Creates new service

        @param processor filesystem events processor
        @param queueSize size of registration queue
    */
    public FSWatcherService(@NotNull IFSEventsProcessor processor, int queueSize) {
        currentService = new ThreadService(this);
        queue = new ArrayBlockingQueue<FutureTask<Void>>(queueSize);
        eventsService = new FSEventsService(processor);
    }

    @Override
    public void startService() throws FailureException {
        Services.startServices(eventsService, currentService);
        running = true;
    }

    @Override
    public void stopService() {
        running = false;
        Services.stopServices(currentService, eventsService);
    }

    @Override
    public boolean isRunning() {
        return Services.isServicesRunning(eventsService, currentService);
    }

    @Override
    public boolean waitFinished(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return Services.waitServicesFinished(timeout, unit, eventsService, currentService);
    }

    @Override
    public void run() {
        while(running) {
            FutureTask<Void> task;
            try {
                task = queue.take();
            } catch (InterruptedException e) {
                if(log.isLoggable(Level.FINE))
                    log.fine("Interrupted while waiting for file");
                Thread.currentThread().interrupt();
                break;
            }
            task.run();
            Thread.interrupted(); //reset interruption state
        }
    }

    public Future<Void> registerRoot(@NotNull final Path path) {
        if(log.isLoggable(Level.FINE))
            log.fine("Registering " + path);
        FutureTask <Void> res = new FutureTask<Void>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                eventsService.registerRoot(path);
                return null;
            }
        });
        queue.add(res);
        return res;
    }

    public Future<Void> unregisterRoot(@NotNull final Path path) {
        if(log.isLoggable(Level.FINE))
            log.fine("Unregistering " + path);
        FutureTask <Void> res = new FutureTask<Void>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                eventsService.unregisterRoot(path);
                return null;
            }
        });
        queue.add(res);
        return res;
    }

    private final BlockingQueue<FutureTask<Void>> queue;
    private final IFSWatcherService eventsService;
    private final ThreadService currentService;
    private volatile boolean running;
    private final Logger log = Logger.getLogger(FSWatcherService.class.getName());
}
