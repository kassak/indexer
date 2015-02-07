package com.github.kassak.indexer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
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
        if(Files.isDirectory(path)) {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        fsProcessor.processFile(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        log.log(Level.WARNING, "Failed to visit " + file.toString(), exc);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        String sdir = dir.toAbsolutePath().toString();
                        if(log.isLoggable(Level.FINE))
                            log.fine("rigistering " + sdir);
                        synchronized(watchKeys) {
                            if(!watchKeys.containsKey(sdir)) {
                                watchKeys.put(sdir, dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
                                watchFilters.put(sdir, new ConcurrentSkipListSet<String>());
                                return FileVisitResult.CONTINUE;
                            }
                            else { //drop file filters on directory registration
                                Set<String> f = watchFilters.get(sdir);
                                boolean e = f.isEmpty();
                                f.clear();
                                return e ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                            }
                        }
                    }
                });
            } catch (IOException e) {
                log.log(Level.WARNING, "Exception while registering " + path, e);
            }
        }
        else {
            synchronized(watchKeys) {
                Path dir = path.toAbsolutePath().getParent();
                String sdir = dir.toString();
                String fname = path.getFileName().toString();
                if(!watchKeys.containsKey(dir)) {
                    watchKeys.put(sdir, dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
                    Set<String> s = new ConcurrentSkipListSet<String>();
                    s.add(fname);
                    watchFilters.put(sdir, s);
                }
                else {
                    //ignore registration of file in watched dir
                }
            }
            fsProcessor.processFile(path);
        }
    }

    public void unregisterRoot(Path path) throws IOException {
        if(Files.isDirectory(path)) {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        synchronized (watchKeys) {
                            String sdir = dir.toAbsolutePath().toString();
                            WatchKey key = watchKeys.get(sdir);
                            watchKeys.remove(sdir);
                            watchFilters.remove(sdir);
                            if (key != null)
                                key.cancel();
                        }
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
            }
        }
        else {
            synchronized(watchKeys) {
                Path dir = path.toAbsolutePath().getParent();
                String sdir = dir.toString();
                String fname = path.getFileName().toString();
                Set<String> s = watchFilters.get(sdir);
                if(s != null && !s.isEmpty()) {
                    s.remove(fname);
                    if(s.isEmpty()) {
                        WatchKey w = watchKeys.get(sdir);
                        if(w != null) {
                            w.cancel();
                            watchKeys.remove(sdir);
                        }
                        watchFilters.remove(sdir);
                    }
                }
            }
        }
    }

    boolean filterEvent(Path base, Path rel) {
        Set<String> s = watchFilters.get(base.toAbsolutePath().toString());
        return s != null && !s.isEmpty() && !s.contains(rel.getFileName().toString());
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
                        WatchEvent<Path> pe = asPathEvent(event);
                        if(filterEvent(base, pe.context()))
                            continue;
                        processNewEntry(base.resolve(pe.context()));
                    } else if (kind == ENTRY_DELETE) {
                        WatchEvent<Path> pe = asPathEvent(event);
                        if(filterEvent(base, pe.context()))
                            continue;
                        processDeleteEntry(base.resolve(pe.context()));
                    } else if (kind == ENTRY_MODIFY) {
                        WatchEvent<Path> pe = asPathEvent(event);
                        if(filterEvent(base, pe.context()))
                            continue;
                        processModifyEntry(base.resolve(pe.context()));
                    } else {
                        log.warning("Unknown event type");
                    }
                }

                boolean valid = key.reset();
                if (!valid) {
                    processDeleteEntry(base);
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
        //TODO: filters
        if(Files.isDirectory(path)) {
            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        registerRoot(dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        fsProcessor.processFile(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        log.log(Level.WARNING, "Failed to visit " + file.toString(), exc);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                log.log(Level.WARNING, "Exception while updating " + path.toString(), e);
            }
        }
        else
            fsProcessor.processFile(path);
    }

    private void processNewEntry(Path path) {
        if(log.isLoggable(Level.FINE))
            log.fine("processNewEntry " + path.toString());
        if(Files.isDirectory(path)) {
            try {
                registerRoot(path);
            } catch (IOException e) {
                log.log(Level.WARNING, "Failed to register root", e);
            }
        }
        else
            fsProcessor.processFile(path);
    }

    private void processDeleteEntry(Path path) {
        if(log.isLoggable(Level.FINE))
            log.fine("processDeleteEntry " + path.toString());
        //TODO: remove filter
        fsProcessor.processFile(path);
    }

    private void processModifyEntry(Path path) {
        if(log.isLoggable(Level.FINE))
            log.fine("processModifyEntry " + path.toString());
        fsProcessor.processFile(path);
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
