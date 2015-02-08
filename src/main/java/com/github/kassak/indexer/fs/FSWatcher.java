package com.github.kassak.indexer.fs;

import com.github.kassak.indexer.utils.InterruptibleCallable;
import com.github.kassak.indexer.utils.ThreadService;
import com.github.kassak.indexer.utils.Uninterruptible;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

public class FSWatcher extends ThreadService implements IFSWatcher {
    public FSWatcher(IFSProcessor fsProcessor) throws UnsupportedOperationException, IOException {
        this.fsProcessor = fsProcessor;
        this.watchKeys = new HashMap<>();
        this.watchFilters = new ConcurrentHashMap<>();
    }

    @Override
    public void startService() throws Exception {
        watcher = FileSystems.getDefault().newWatchService();
        super.startService();
    }

    @Override
    public void stopService() throws Exception {
        try {
            super.stopService();
        }
        finally {
            try {
                watcher.close();
            } catch (IOException e) {
                log.log(Level.WARNING, "Failed to close watcher", e);
                throw e;
            }
        }
    }

    public void registerRoot(Path path) throws IOException {
        synchronized (watchKeys) {
            boolean finished = false;
            try {
                if (Files.isDirectory(path)) {
                    try {
                        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                                if (Thread.currentThread().isInterrupted())
                                    return FileVisitResult.SKIP_SIBLINGS;
                                if (registerDirectory(dir.toAbsolutePath()))
                                    return FileVisitResult.CONTINUE;
                                return FileVisitResult.SKIP_SUBTREE;
                            }

                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                if (Thread.currentThread().isInterrupted())
                                    return FileVisitResult.SKIP_SIBLINGS;
                                if (Files.isRegularFile(file))
                                    try {
                                        fsProcessor.onFileChanged(file.toAbsolutePath());
                                    } catch (InterruptedException e) {
                                        log.warning("Interrupted while reporting change " + file);
                                        Thread.currentThread().interrupt();
                                        return FileVisitResult.SKIP_SIBLINGS;
                                    }
                                else if (log.isLoggable(Level.FINE))
                                    log.fine("Skipping not a regular file " + file);
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
                    if (registerFile(path.toAbsolutePath()))
                        try {
                            fsProcessor.onFileChanged(path.toAbsolutePath());
                            finished = true;
                        } catch (InterruptedException e) {
                            log.warning("Interrupted while reporting change  " + path);
                            Thread.currentThread().interrupt();
                        }
                } else {
                    log.warning("Attempt to register not file nor directory " + path);
                    finished = true;
                }
            }
            finally {
                if(!finished) {
                    log.warning("Registration not finished. Recovering " + path);
                    boolean interrupted = Thread.currentThread().isInterrupted();
                    unregisterRoot(path);
                    if(interrupted)
                        Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void unregisterRoot(final Path path) throws IOException {
        synchronized (watchKeys) {
            if (Files.isDirectory(path)) {
                if(watchKeys.containsKey(path.toAbsolutePath().getParent().toString())) {
                    log.warning("Ignoring attempt to unregister subdir of watched dir " + path.toAbsolutePath());
                    return;
                }
                try {
                    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            unregisterDirectory(dir.toAbsolutePath());
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
                            fsProcessor.onDirectoryRemoved(path.toAbsolutePath());
                        }
                    }, 10);
                }
            } else {
                if (unregisterFile(path)) {
                    Uninterruptible.performUninterruptibly(new InterruptibleCallable() {
                        @Override
                        public void call() throws InterruptedException {
                            fsProcessor.onFileRemoved(path.toAbsolutePath());
                        }
                    }, 10);
                }
            }
        }
    }

    private boolean registerDirectory(Path dir) throws IOException {
        String sdir = dir.toString();
        if(log.isLoggable(Level.FINE))
            log.fine("rigistering " + sdir);
        if(!watchKeys.containsKey(sdir)) {
            watchKeys.put(sdir, dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
            watchFilters.put(sdir, Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>()));
            return true;
        } else {
            Set<String> f = watchFilters.get(sdir);
            if(f.isEmpty())
                return false;
            else { //drop file filters on directory registration
                f.clear();
                return true;
            }
        }
    }

    private boolean registerFile(Path path) throws IOException {
        Path dir = path.getParent();
        String sdir = dir.toString();
        String fname = path.getFileName().toString();
        if(log.isLoggable(Level.FINE))
            log.fine("Unregistering " + path);
        if(!watchKeys.containsKey(sdir)) {
            watchKeys.put(sdir, dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
            Set<String> s = new ConcurrentSkipListSet<>();
            s.add(fname);
            watchFilters.put(sdir, s);
            return true;
        }
        else {
            Set<String> f = watchFilters.get(sdir);
            if(f.isEmpty()) {
                log.warning("Ignoring attempt to watch file in watched directory " + path);
                return false;
            }
            else {
                return f.add(fname);
            }
        }
    }

    private boolean unregisterDirectory(Path dir) {
        String sdir = dir.toString();
        if(log.isLoggable(Level.FINE))
            log.fine("Unregistering " + sdir);
        if(!watchKeys.containsKey(sdir)) {
            return false;
        }
        else { //ignore filters
            WatchKey w = watchKeys.remove(sdir);
            if(w.isValid())
                w.cancel();
            watchFilters.remove(sdir);
            return true;
        }
    }

    private boolean unregisterFile(Path path) {
        Path dir = path.getParent();
        String sdir = dir.toString();
        String fname = path.getFileName().toString();
        if(log.isLoggable(Level.FINE))
            log.fine("Unregistering " + path);
        if(!watchKeys.containsKey(sdir)) {
            return false;
        }
        else {
            Set<String> f = watchFilters.get(sdir);
            if(f.isEmpty()) {
                log.warning("Ignoring attempt to unwatch file in watched directory " + path);
                return false; //ignore unregistration of file in watched dir
            }
            else {
                boolean ex = f.remove(fname);
                if(f.isEmpty()) {
                    WatchKey w = watchKeys.remove(sdir);
                    if(w.isValid())
                        w.cancel();
                    watchFilters.remove(sdir);
                }
                return ex;
            }
        }
    }

    boolean filterEvent(Path path) {
        Set<String> s = watchFilters.get(path.getParent().toString());
        return s != null && !s.isEmpty() && !s.contains(path.getFileName().toString());
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ClosedWatchServiceException e) {
                    log.warning("Watch service closed. breaking");
                    break;
                }

                Path base = (Path) key.watchable();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == OVERFLOW) {
                        processOverflow(base);
                    } else if (kind == ENTRY_CREATE) {
                        Path p = base.resolve(asPathEvent(event).context()).toAbsolutePath();
                        if (filterEvent(p))
                            continue;
                        processNewEntry(p);
                    } else if (kind == ENTRY_DELETE) {
                        Path p = base.resolve(asPathEvent(event).context()).toAbsolutePath();
                        if (filterEvent(p))
                            continue;
                        processDeleteEntry(p);
                    } else if (kind == ENTRY_MODIFY) {
                        Path p = base.resolve(asPathEvent(event).context()).toAbsolutePath();
                        if (filterEvent(p))
                            continue;
                        processModifyEntry(p);
                    } else {
                        log.warning("Unknown event type");
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    synchronized (watchKeys) {
                        unregisterDirectory(base);
                    }
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Exception in FSWatcher loop. ignoring", e);
            }
        }
    }

    private void processOverflow(Path path) {
        log.warning("processOverflow " + path);
        synchronized (watchKeys) { //assure parent is still watched
            if(!watchKeys.containsKey(path.toString())) {
                if(log.isLoggable(Level.FINE)) {
                    log.fine("Ignoring overflow on unwatched directory " + path);
                    return;
                }
            }
            try {
                fsProcessor.onDirectoryChanged(path);
            } catch (InterruptedException e) {
                log.warning("Interrupted while reporting overflow " + path);
                Thread.currentThread().interrupt();
                return;
            }
            final Set<String> filter = new HashSet<>(watchFilters.get(path.toString()));
            final Set<String> files = new HashSet<>();
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if(Thread.currentThread().isInterrupted())
                            return FileVisitResult.SKIP_SIBLINGS;
                        if(filter.isEmpty())
                            registerRoot(dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
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
                unregisterFile(path.resolve(f));
        }
    }

    private void processNewEntry(Path path) {
        if(log.isLoggable(Level.FINE))
            log.fine("processNewEntry " + path);
        if(Files.isDirectory(path)) {
            try {
                synchronized (watchKeys) { //assure parent is still watched
                    if(watchKeys.containsKey(path.getParent().toString()))
                        registerRoot(path);
                    else if(log.isLoggable(Level.FINE))
                        log.fine("Ignoring auto registration of subdirectory or unwatched path " + path);
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "Failed to register root", e);
            }
        }
        else if(Files.isRegularFile(path))
            try {
                fsProcessor.onFileChanged(path);
            } catch (InterruptedException e) {
                log.warning("Interrupted while reporting change " + path);
                Thread.currentThread().interrupt();
                return;
            }
        else if(log.isLoggable(Level.FINE))
            log.fine("Ignoring creation of not directory nor file " + path);
    }

    private void processDeleteEntry(Path path) {
        if (log.isLoggable(Level.FINE))
            log.fine("processDeleteEntry " + path.toString());
        synchronized (watchKeys) {
            unregisterFile(path);
        }
        try {
            fsProcessor.onFileRemoved(path);
        } catch (InterruptedException e) {
            log.warning("Interrupted while reporting remove " + path);
            Thread.currentThread().interrupt();
            return;
        }
    }

    private void processModifyEntry(Path path) {
        if(log.isLoggable(Level.FINE))
            log.fine("processModifyEntry " + path.toString());
        if (Files.isRegularFile(path))
            try {
                fsProcessor.onFileChanged(path);
            } catch (InterruptedException e) {
                log.warning("Interrupted while reporting change " + path);
                Thread.currentThread().interrupt();
                return;
            }
    }

    @SuppressWarnings("unchecked")
    private static WatchEvent<Path> asPathEvent(WatchEvent<?> event) {
        return (WatchEvent<Path>)event;
    }
    private WatchService watcher;
    private final IFSProcessor fsProcessor;
    private final Map<String, WatchKey> watchKeys;
    private final Map<String, Set<String>> watchFilters;
    private static final Logger log = Logger.getLogger(FSWatcher.class.getName());
}
