package com.github.kassak.indexer.fs;

import com.github.kassak.indexer.utils.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
    Service which processes events from FSEventsService.
*/
public class FSWatcherService implements Runnable, IService, FSEventsService.IRawFSEventsProcessor {
    /**
        Creates new service

        @param processor filesystem events processor
        @param queueSize size of registration queue
    */
    public FSWatcherService(@NotNull IFSEventsProcessor processor, int queueSize) {
        eventsProcessor = processor;
        currentService = new ThreadService(this);
        queue = new ArrayBlockingQueue<>(queueSize*2);
        fsSemaphore = new Semaphore(queueSize);
        userSemaphore = new Semaphore(queueSize);
        eventsService = new FSEventsService(this);
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
            updateActivity();
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

    public Future<Void> registerRoot(@NotNull final Path path) throws InterruptedException {
        if(log.isLoggable(Level.FINER))
            log.finer("Registering " + path);
        userSemaphore.acquire();
        FutureTask <Void> res = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    registerRootImpl(path, false);
                } finally {
                    userSemaphore.release();
                }
                return null;
            }
        });
        queue.put(res);
        return res;
    }

    public Future<Void> unregisterRoot(@NotNull final Path path) throws InterruptedException {
        if(log.isLoggable(Level.FINER))
            log.finer("Unregistering " + path);
        userSemaphore.acquire();
        FutureTask <Void> res = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    unregisterRootImpl(path);
                } finally {
                    userSemaphore.release();
                }
                return null;
            }
        });
        queue.put(res);
        return res;
    }

    private boolean registerRootImpl(@NotNull Path path, boolean auto) throws IOException {
        boolean finished = false;
        path = path.toAbsolutePath();
        boolean wasBlacklisted = eventsService.isBlacklisted(path);

        try {
            if (Files.isDirectory(path)) {
                if(eventsService.isRegistered(path)) {
                    log.log(auto ? Level.FINER : Level.WARNING, "Already registered " + path);
                    finished = true;
                    return false;
                }
                if(auto) {
                    if(eventsService.isRegistered(path.getParent())) {
                        log.finer("Outdated registration query " + path);
                        finished = true;
                        return false;
                    }
                    if(wasBlacklisted) {
                        log.finer("Skipping blacklisted " + path);
                        finished = true;
                        return false;
                    }
                } else {
                    eventsService.setBlacklisted(path, false);
                }
                try {
                    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            updateActivity();
                            if (Thread.currentThread().isInterrupted())
                                return FileVisitResult.SKIP_SIBLINGS;
                            if(eventsService.isBlacklisted(dir)) {
                                if(log.isLoggable(Level.FINER))
                                    log.finer("Skipping blacklisted " + dir);
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            if (eventsService.registerDirectory(dir))
                                return FileVisitResult.CONTINUE;
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            updateActivity();
                            if (Thread.currentThread().isInterrupted())
                                return FileVisitResult.SKIP_SIBLINGS;
                            if(eventsService.isBlacklisted(file)) {
                                if(log.isLoggable(Level.FINER))
                                    log.finer("Skipping blacklisted " + file);
                                return FileVisitResult.SKIP_SUBTREE;
                            }
                            if (Files.isRegularFile(file))
                                try {
                                    eventsProcessor.onFileChanged(file);
                                } catch (InterruptedException e) {
                                    log.fine("Interrupted while reporting change " + file);
                                    Thread.currentThread().interrupt();
                                    return FileVisitResult.SKIP_SIBLINGS;
                                }
                            else if (log.isLoggable(Level.FINER))
                                log.finer("Skipping not a regular file " + file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            log.log(Level.WARNING, "Failed to visit " + file, exc);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    if (!Thread.currentThread().isInterrupted())
                        finished = true;
                } catch (IOException e) {
                    log.log(Level.WARNING, "Exception while registering " + path, e);
                    throw e;
                }
            } else if (Files.isRegularFile(path)) {
                if(auto) {
                    if(log.isLoggable(Level.FINER))
                        log.finer("Ignoring auto registration of file " + path);
                    finished = true;
                    return false;
                }
                eventsService.setBlacklisted(path, false);
                if (eventsService.registerFile(path)) {
                    try {
                        eventsProcessor.onFileChanged(path);
                        finished = true;
                    } catch (InterruptedException e) {
                        log.fine("Interrupted while reporting change  " + path);
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                log.log(auto ? Level.FINER : Level.FINE, "Attempt to register not file nor directory " + path);
                eventsService.setBlacklisted(path, false);
                finished = true;
            }
        } finally {
            if(!finished) {
                log.fine("Registration not finished. Recovering " + path);
                boolean interrupted = Thread.currentThread().isInterrupted();
                unregisterRootImpl(path);
                eventsService.setBlacklisted(path, wasBlacklisted);
                if(interrupted)
                    Thread.currentThread().interrupt();
            }
        }
        return finished;
    }

    private void unregisterRootImpl(@NotNull Path rpath) throws IOException {
        final Path path = rpath.toAbsolutePath();
        eventsService.setBlacklisted(path, true);
        if (Files.isDirectory(path)) {
            if(!eventsService.isRegistered(path)) {
                log.fine("Already unregistered " + path);
                return;
            }
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        updateActivity();
                        eventsService.unregisterDirectory(dir);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        log.log(Level.WARNING, "Failed to visit " + file, exc);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                log.log(Level.WARNING, "Exception while unregistering " + path, e);
                //TODO: consistency?
                throw e;
            } finally {
                Uninterruptible.performUninterruptibly(new InterruptibleCallable() {
                    @Override
                    public void call() throws InterruptedException {
                        eventsProcessor.onDirectoryRemoved(path);
                    }
                }, 10);
            }
        } else {
            if (eventsService.unregisterFile(path)) {
                Uninterruptible.performUninterruptibly(new InterruptibleCallable() {
                    @Override
                    public void call() throws InterruptedException {
                        eventsProcessor.onFileRemoved(path);
                    }
                }, 10);
            }
        }
    }

    private void processOverflowImpl(@NotNull Path path) {
        if(log.isLoggable(Level.FINER))
            log.finer("processOverflowImpl " + path);
        if(!eventsService.isRegistered(path)) {
            log.fine("Ignoring overflow on unwatched directory " + path);
            return;
        }
        try {
            eventsProcessor.onDirectoryChanged(path);
        } catch (InterruptedException e) {
            log.fine("Interrupted while reporting overflow " + path);
            Thread.currentThread().interrupt();
            return;
        }
        final Set<String> filter = new HashSet<>(eventsService.watchedFiles(path));
        final Set<String> files = new HashSet<>();
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    updateActivity();
                    if(Thread.currentThread().isInterrupted())
                        return FileVisitResult.SKIP_SIBLINGS;
                    if(filter.isEmpty())
                        registerRootImpl(dir, true);
                    return FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    updateActivity();
                    if(Thread.currentThread().isInterrupted())
                        return FileVisitResult.SKIP_SIBLINGS;
                    if(filter.contains(file.getFileName().toString()))
                        files.add(file.getFileName().toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    log.log(Level.WARNING, "Failed to visit " + file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.log(Level.WARNING, "Exception while processing overflow " + path, e);
            //TODO: happens? consistency?
        }
        if(Thread.currentThread().isInterrupted())
            return;
        filter.removeAll(files);
        for(String f : filter)
            eventsService.unregisterFile(path.resolve(f));
    }

    public void processNewEntryImpl(@NotNull Path path) {
        if(log.isLoggable(Level.FINER))
            log.finer("processNewEntryImpl " + path);
        if(Files.isDirectory(path)) {
            try {
                registerRootImpl(path, true);
            } catch (IOException e) {
                log.log(Level.WARNING, "Failed to register root", e);
            }
        }
        else if(Files.isRegularFile(path))
            try {
                if(eventsService.isFileRegistered(path))
                    eventsProcessor.onFileChanged(path);
            } catch (InterruptedException e) {
                log.warning("Interrupted while reporting change " + path);
                Thread.currentThread().interrupt();
            }
        else if(log.isLoggable(Level.FINER))
            log.finer("Ignoring creation of not directory nor file " + path);
    }

    public void processDeleteEntryImpl(@NotNull Path path) {
        if (log.isLoggable(Level.FINER))
            log.finer("processDeleteEntryImpl " + path);
        eventsService.unregisterFile(path);
        try {
            eventsProcessor.onFileRemoved(path);
        } catch (InterruptedException e) {
            log.warning("Interrupted while reporting remove " + path);
            Thread.currentThread().interrupt();
        }
    }

    public void processModifyEntryImpl(@NotNull Path path) {
        if(log.isLoggable(Level.FINER))
            log.finer("processModifyEntryImpl " + path);
        if (Files.isRegularFile(path))
            try {
                if(eventsService.isFileRegistered(path))
                    eventsProcessor.onFileChanged(path);
            } catch (InterruptedException e) {
                log.warning("Interrupted while reporting change " + path);
                Thread.currentThread().interrupt();
            }
    }

    @Override
    public void processOverflow(final @NotNull Path path) throws InterruptedException {
        if(log.isLoggable(Level.FINER))
            log.finer("processOverflow " + path);
        fsSemaphore.acquire();
        FutureTask <Void> res = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    processOverflowImpl(path);
                } finally {
                    fsSemaphore.release();
                }
                return null;
            }
        });
        queue.put(res);
    }

    @Override
    public void processNewEntry(final @NotNull Path path) throws InterruptedException {
        if(log.isLoggable(Level.FINER))
            log.finer("processNewEntry " + path);
        fsSemaphore.acquire();
        FutureTask <Void> res = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    processNewEntryImpl(path);
                } finally {
                    fsSemaphore.release();
                }
                return null;
            }
        });
        queue.put(res);
    }

    @Override
    public void processDeleteEntry(final @NotNull Path path) throws InterruptedException {
        if(log.isLoggable(Level.FINER))
            log.finer("processDeleteEntry " + path);
        fsSemaphore.acquire();
        FutureTask <Void> res = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    processDeleteEntryImpl(path);
                } finally {
                    fsSemaphore.release();
                }
                return null;
            }
        });
        queue.put(res);
    }

    @Override
    public void processModifyEntry(final @NotNull Path path) throws InterruptedException {
        if(log.isLoggable(Level.FINER))
            log.finer("processModifyEntry " + path);
        fsSemaphore.acquire();
        FutureTask <Void> res = new FutureTask<>(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    processModifyEntryImpl(path);
                } finally {
                    fsSemaphore.release();
                }
                return null;
            }
        });
        queue.put(res);
    }

    @TestOnly
    public boolean isIdle() {
        return queue.isEmpty() && eventsService.isIdle();
    }

    @TestOnly
    public long getLastActivity() {
        return isIdle() ? lastActivity : eventsService.getLastActivity();
    }

    private void updateActivity() {
        lastActivity = System.currentTimeMillis();
    }

    private final Semaphore userSemaphore;
    private final Semaphore fsSemaphore;
    private final BlockingQueue<FutureTask<Void>> queue;
    private final FSEventsService eventsService;
    private final IFSEventsProcessor eventsProcessor;
    private final ThreadService currentService;
    private volatile boolean running;
    private volatile long lastActivity;
    private final Logger log = Logger.getLogger(FSWatcherService.class.getName());
}
