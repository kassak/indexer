package com.github.kassak.indexer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

public class FSWatcher implements IFSWatcher {
    public FSWatcher(IFSProcessor fsProcessor) throws UnsupportedOperationException, IOException {
        watcher = FileSystems.getDefault().newWatchService();
        this.fsProcessor = fsProcessor;
        this.watchKeys = new HashMap<>();
        this.watchFilters = new ConcurrentHashMap<>();
    }

    public void registerRoot(Path path) throws IOException {
        synchronized (watchKeys) {
            if (Files.isDirectory(path)) {
                try {
                    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            if (registerDirectory(dir.toAbsolutePath()))
                                return FileVisitResult.CONTINUE;
                            return FileVisitResult.SKIP_SUBTREE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            if (Files.isRegularFile(file))
                                fsProcessor.onFileChanged(file.toAbsolutePath());
                            else if (log.isLoggable(Level.FINE))
                                log.fine("Skipping not a regular file " + file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            log.log(Level.WARNING, "Failed to visit " + file.toString(), exc);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (IOException e) {
                    log.log(Level.WARNING, "Exception while registering " + path, e);
                    throw e;
                }
            } else if (Files.isRegularFile(path)) {
                if (registerFile(path.toAbsolutePath()))
                    fsProcessor.onFileChanged(path.toAbsolutePath());
            } else
                log.warning("Attempt to register not file nor directory " + path);
        }
    }

    public void unregisterRoot(Path path) throws IOException {
        synchronized (watchKeys) {
            if (Files.isDirectory(path)) {
                if(watchKeys.containsKey(path.toAbsolutePath().getParent().toString())) {
                    log.warning("Ignoring attempt to unregister subdir of watchewd dir " + path.toAbsolutePath().toString());
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
                    throw e;
                } finally {
                    fsProcessor.onDirectoryRemoved(path);
                }
            } else {
                if (unregisterFile(path))
                    fsProcessor.onFileRemoved(path.toAbsolutePath());
            }
        }
    }

    private boolean registerDirectory(Path dir) throws IOException {
        String sdir = dir.toString();
        if(log.isLoggable(Level.FINE))
            log.fine("rigistering " + sdir);
        if(!watchKeys.containsKey(sdir)) {
            watchKeys.put(sdir, dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
            watchFilters.put(sdir, new ConcurrentSkipListSet<String>());
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
            log.fine("unrigistering " + path);
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
                return false; //ignore registration of file in watched dir
            }
            else {
                return f.add(fname);
            }
        }
    }

    private boolean unregisterDirectory(Path dir) {
        String sdir = dir.toString();
        if(log.isLoggable(Level.FINE))
            log.fine("unrigistering " + sdir);
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
            log.fine("unrigistering " + path);
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
        try {
            while (!Thread.interrupted()) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException x) {
                    break;
                }

                Path base = (Path) key.watchable();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == OVERFLOW) {
                        processOverflow(base);
                    } else if (kind == ENTRY_CREATE) {
                        Path p = base.resolve(asPathEvent(event).context()).toAbsolutePath();
                        if(filterEvent(p))
                            continue;
                        processNewEntry(p);
                    } else if (kind == ENTRY_DELETE) {
                        Path p = base.resolve(asPathEvent(event).context()).toAbsolutePath();
                        if(filterEvent(p))
                            continue;
                        processDeleteEntry(p);
                    } else if (kind == ENTRY_MODIFY) {
                        Path p = base.resolve(asPathEvent(event).context()).toAbsolutePath();
                        if(filterEvent(p))
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
            }
        }
        finally {
            try {
                watcher.close();
            } catch (IOException e) {
                log.warning("Failed to close");
            }
        }
    }

    private void processOverflow(Path path) {
        log.warning("processOverflow " + path.toString());
        synchronized (watchKeys) { //assure parent is still watched
            if(!watchKeys.containsKey(path.toString())) {
                if(log.isLoggable(Level.FINE)) {
                    log.fine("Ignoring overflow on unwatched directory " + path);
                    return;
                }
            }
            fsProcessor.onDirectoryChanged(path);
            final Set<String> filter = new HashSet<>(watchFilters.get(path.toString()));
            final Set<String> files = new HashSet<>();
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if(filter.isEmpty())
                            registerRoot(dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if(filter.contains(file.getFileName().toString()))
                            files.add(file.getFileName().toString());
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        log.log(Level.WARNING, "Failed to visit " + file.toString(), exc);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                log.log(Level.WARNING, "Exception while processing overflow " + path, e);
            }
            filter.removeAll(files);
            for(String f : filter)
                unregisterFile(path.resolve(f));
        }
    }

    private void processNewEntry(Path path) {
        if(log.isLoggable(Level.FINE))
            log.fine("processNewEntry " + path.toString());
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
            fsProcessor.onFileChanged(path);
        else if(log.isLoggable(Level.FINE))
            log.fine("Ignoring creation of not directory nor file " + path);
    }

    private void processDeleteEntry(Path path) {
        if (log.isLoggable(Level.FINE))
            log.fine("processDeleteEntry " + path.toString());
        synchronized (watchKeys) {
            unregisterFile(path);
        }
        fsProcessor.onFileRemoved(path);
    }

    private void processModifyEntry(Path path) {
        if(log.isLoggable(Level.FINE))
            log.fine("processModifyEntry " + path.toString());
        if (Files.isRegularFile(path))
            fsProcessor.onFileChanged(path);
    }

    @SuppressWarnings("unchecked")
    private static WatchEvent<Path> asPathEvent(WatchEvent<?> event) {
        return (WatchEvent<Path>)event;
    }
    private final WatchService watcher;
    private final IFSProcessor fsProcessor;
    private final Map<String, WatchKey> watchKeys;
    private final Map<String, Set<String>> watchFilters;
    private static final Logger log = Logger.getLogger(FSWatcher.class.getName());
}
