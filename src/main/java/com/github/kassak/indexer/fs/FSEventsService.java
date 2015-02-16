package com.github.kassak.indexer.fs;

import com.github.kassak.indexer.utils.IService;
import com.github.kassak.indexer.utils.ThreadService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

/**
    Filesystem watcher. Producer of events.
*/
public class FSEventsService implements Runnable, IService {
    public static interface IRawFSEventsProcessor {
        public void processOverflow(@NotNull Path path);
        public void processNewEntry(@NotNull Path path);
        public void processDeleteEntry(@NotNull Path path);
        public void processModifyEntry(@NotNull Path path);
    }

    /**
        Creates new events service with specified processor

        @param fsProcessor processor of filesystem events
    */
    public FSEventsService(@NotNull IRawFSEventsProcessor fsProcessor) {
        currentService = new ThreadService(this);
        this.fsProcessor = fsProcessor;
        this.watchKeys = new HashMap<>();
        this.watchWhitelists = new ConcurrentHashMap<>();
        this.watchBlacklists = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        waitingForEvents = false;
    }

    @Override
    public void startService() throws FailureException {
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch(IOException e) {
            log.log(Level.WARNING, "Failed to start watcher", e);
            throw new FailureException();
        }
        try {
            currentService.startService();
        } catch(FailureException e) {
            log.warning("Failed to start, closing watcher");
            try {
                watcher.close();
            } catch (IOException we) {
                log.log(Level.WARNING, "Failed to close watcher", we);
            }
            throw e;
        }
    }

    @Override
    public void stopService() {
        try {
            currentService.stopService();
        } finally {
            try {
                watcher.close();
            } catch (IOException e) {
                log.log(Level.WARNING, "Failed to close watcher", e);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return currentService.isRunning();
    }

    @Override
    public boolean waitFinished(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return currentService.waitFinished(timeout, unit);
    }

    @TestOnly
    public boolean isIdle() {
        return waitingForEvents;
    }

    @TestOnly
    public long getLastActivity() {
        return isIdle() ? lastActivity : System.currentTimeMillis();
    }

    public boolean isFileRegistered(@NotNull Path path) {
        Set<String> res = watchWhitelists.get(path.getParent().toString());
        return res == null || res.contains(path.getFileName().toString());
    }

    public boolean isBlacklisted(@NotNull Path dir) {
        return watchBlacklists.contains(dir.toString());
    }

    public void setBlacklisted(@NotNull Path path, boolean b) {
        if(b)
            watchBlacklists.add(path.toString());
        else
            watchBlacklists.remove(path.toString());
    }

    public boolean isRegistered(@NotNull Path path) {
        return watchKeys.containsKey(path.toString());
    }

    public Set<String> watchedFiles(@NotNull Path path) {
        Set<String> res = watchWhitelists.get(path.toString());
        if(res == null)
            res = Collections.emptySet();
        return res;
    }

    public boolean registerDirectory(@NotNull Path dir) throws IOException {
        if(log.isLoggable(Level.FINER))
            log.finer("Registering " + dir);
        String sdir = dir.toString();
        if(!watchKeys.containsKey(sdir)) {
            watchKeys.put(sdir, dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
            return true;
        } else {
            Set<String> f = watchWhitelists.get(sdir);
            if(f == null || f.isEmpty())
                return false;
            else {
                log.fine("Directory registration overrides files registration " + dir);
                f.clear();
                return true;
            }
        }
    }

    public boolean registerFile(@NotNull Path path) throws IOException {
        Path dir = path.getParent();
        String sdir = dir.toString();
        String fname = path.getFileName().toString();
        if(log.isLoggable(Level.FINER))
            log.finer("Unregistering " + path);
        if(!watchKeys.containsKey(sdir)) {
            watchKeys.put(sdir, dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY));
            Set<String> s = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(1));
            s.add(fname);
            watchWhitelists.put(sdir, s);
            return true;
        }
        else {
            Set<String> f = watchWhitelists.get(sdir);
            if(f == null || f.isEmpty()) {
                log.fine("Ignoring attempt to watch file in watched directory " + path);
                return false;
            }
            else
                return f.add(fname);
        }
    }

    public boolean unregisterDirectory(@NotNull Path dir) {
        String sdir = dir.toString();
        if(log.isLoggable(Level.FINER))
            log.finer("Unregistering " + sdir);
        if(!watchKeys.containsKey(sdir)) {
            return false;
        } else {
            WatchKey w = watchKeys.remove(sdir);
            if(w.isValid())
                w.cancel();
            Set<String> s = watchWhitelists.remove(sdir);
            if(s != null && !s.isEmpty())
                log.fine("Cancelled  file registrations while unregistering " + sdir);
            return true;
        }
    }

    public boolean unregisterFile(@NotNull Path path) {
        Path dir = path.getParent();
        String sdir = dir.toString();
        String fname = path.getFileName().toString();
        if(log.isLoggable(Level.FINER))
            log.finer("Unregistering " + path);
        if(!watchKeys.containsKey(sdir)) {
            return false;
        } else {
            Set<String> f = watchWhitelists.get(sdir);
            if(f == null || f.isEmpty()) {
                log.fine("Ignoring attempt to unwatch file in watched directory " + path);
                return false;
            }
            else {
                boolean ex = f.remove(fname);
                if(f.isEmpty()) {
                    WatchKey w = watchKeys.remove(sdir);
                    if(w.isValid())
                        w.cancel();
                    watchWhitelists.remove(sdir);
                }
                return ex;
            }
        }
    }

    boolean filterEvent(@NotNull Path path) {
        if(isBlacklisted(path))
            return false;
        Set<String> s = watchWhitelists.get(path.getParent().toString());
        return s != null && !s.isEmpty() && !s.contains(path.getFileName().toString());
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            try {
                lastActivity = System.currentTimeMillis();
                WatchKey key;
                try {
                    waitingForEvents = true;
                    key = watcher.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ClosedWatchServiceException e) {
                    log.warning("Watch service closed. breaking");
                    break;
                } finally {
                    waitingForEvents = false;
                }

                Path base = (Path) key.watchable();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (kind == OVERFLOW) {
                        fsProcessor.processOverflow(base);
                    } else if (kind == ENTRY_CREATE || kind == ENTRY_DELETE || kind == ENTRY_MODIFY) {
                        Path p = base.resolve(asPathEvent(event).context()).toAbsolutePath();
                        if (filterEvent(p))
                            continue;
                        if (kind == ENTRY_CREATE)
                            fsProcessor.processNewEntry(p);
                        else if (kind == ENTRY_DELETE)
                            fsProcessor.processDeleteEntry(p);
                        else if (kind == ENTRY_MODIFY)
                            fsProcessor.processModifyEntry(p);
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

    @NotNull
    @SuppressWarnings("unchecked")
    private static WatchEvent<Path> asPathEvent(@NotNull WatchEvent<?> event) {
        return (WatchEvent<Path>)event;
    }
    private WatchService watcher;
    private final IRawFSEventsProcessor fsProcessor;
    private final Map<String, WatchKey> watchKeys;
    private final Map<String, Set<String>> watchWhitelists;
    private final Set<String> watchBlacklists;
    private final ThreadService currentService;
    private volatile boolean waitingForEvents;
    private volatile long lastActivity;
    private static final Logger log = Logger.getLogger(FSEventsService.class.getName());
}
