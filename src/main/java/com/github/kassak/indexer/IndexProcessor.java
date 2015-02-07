package com.github.kassak.indexer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IndexProcessor {
    private static class IndexedFile {
        static public final int VALID = 0;
        static public final int PROCESSING = 1;
        static public final int INVALID = 2;

        public IndexedFile(Path path, int state, long stamp) {
            this.path = path;
            this.state = state;
            this.stamp = stamp;
            this.processingStamp = stamp;
        }

        public Path getPath() {
            return path;
        }

        public void setPath(Path path) {
            this.path = path;
        }

        public int getState() {
            return state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public long getStamp() {
            return stamp;
        }

        public void setStamp(long stamp) {
            this.stamp = stamp;
        }

        public long getProcessingStamp() {
            return processingStamp;
        }

        public void setProcessingStamp(long processingStamp) {
            this.processingStamp = processingStamp;
        }

        private Path path;
        private int state;
        private long stamp, processingStamp;
    }

    public IndexProcessor(FilesProcessor proc) {
        files = new ConcurrentSkipListMap<>();
        filesProcessor = proc;
    }
    public void syncFile(long stamp, Path file) {
        if(log.isLoggable(Level.FINE))
            log.fine("syncing file " + file);
        IndexedFile f = files.get(file.toString());
        if(f == null) {
            f = new IndexedFile(file, IndexedFile.INVALID, stamp);
            files.put(file.toString(), f);
        }
        assert(f.stamp <= stamp); //this is guaranteed
        f.stamp = stamp;
        if(f.state == IndexedFile.PROCESSING) //already processing
            return;
        f.state = IndexedFile.PROCESSING;
        f.processingStamp = stamp;
        processFile(file);
    }

    public void syncDirectory(final long stamp, Path file) {
        if(log.isLoggable(Level.FINE))
            log.fine("syncing directory " + file);
        try {
            Files.walkFileTree(file, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    //TODO: check file modification
                    syncFile(stamp, file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    log.log(Level.WARNING, "Error while visiting file " + file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.log(Level.WARNING, "Error while syncing dir " + file, e);
        }
        String sfile = file.toString() + FileSystems.getDefault().getSeparator();
        Set<String> dir = files.subMap(sfile, sfile + Character.MAX_VALUE).keySet();
        Iterator<String> it = dir.iterator();
        while (it.hasNext()) {
            String f = it.next();
            if(!Files.exists(FileSystems.getDefault().getPath(f))) {
                it.remove();
            }
        }
    }

    public void removeFile(long stamp, Path file) {
        if(log.isLoggable(Level.FINE))
            log.fine("removing file " + file);
        files.remove(file.toString());
    }

    public void removeDirectory(long stamp, Path file) {
        String sfile = file.toString();
        if(log.isLoggable(Level.FINE))
            log.fine("removing directory " + sfile);
        files.remove(sfile);
        sfile += FileSystems.getDefault().getSeparator();
        files.subMap(sfile, sfile + Character.MAX_VALUE).clear();
    }

    private void processFile(Path file) {
        try {
            filesProcessor.processFile(file);
        } catch (InterruptedException e) {
            log.warning("Interrupted while queueing of processing of " + file);
            Thread.currentThread().interrupt();
        }
    }

    public void fileFinished(long stamp, Path file, boolean b) {
        String sfile = file.toString();
        if(log.isLoggable(Level.FINE))
            log.fine("File finished " + sfile + " with result " + b);
        IndexedFile f = files.get(file.toString());
        if(f == null) { //file removed
            if(log.isLoggable(Level.FINE))
                log.fine("Finished processing removed file " + sfile);
            return;
        }
        assert(f.state == IndexedFile.PROCESSING && f.processingStamp <= stamp); //NOTE: how couldn't it be?
        if(f.stamp > stamp) { //modified while processing
            if(log.isLoggable(Level.FINE))
                log.fine("File was modified while processing. Doing it again " + sfile);
            f.processingStamp = f.stamp;
            f.state = IndexedFile.PROCESSING;
            processFile(file);
        }
        else {
            f.stamp = stamp;
            f.processingStamp = stamp;
            f.state = b ? IndexedFile.VALID : IndexedFile.INVALID; //TODO: do we need to retry on invalid?
        }
    }

    public void removeWords(long stamp, Path file) {
    }

    public void addWord(long stamp, Path file, String word) {
    }


    public Collection<String> getFiles() {
        return files.keySet();
    }

    private final SortedMap<String, IndexedFile> files;
    private final FilesProcessor filesProcessor;
    private static final Logger log = Logger.getLogger(IndexProcessor.class.getName());
}
