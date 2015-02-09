package com.github.kassak.indexer.storage;

import com.github.kassak.indexer.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IndexProcessor {

    public IndexProcessor(IIndexManager im) {
        indexManager = im;
        index = new IndexStorage();
    }
    public void syncFile(long stamp, Path file) {
        if(log.isLoggable(Level.FINE))
            log.fine("Syncing file " + file);
        IndexedFile f = index.getOrAddFile(file, stamp);
        assert(f.stamp <= stamp); //this is guaranteed
        f.stamp = stamp;
        if(f.state == IndexedFile.PROCESSING) {//already processing
            if(log.isLoggable(Level.FINE))
                log.fine("Already processing " + file);
            return;
        }
        f.state = IndexedFile.PROCESSING;
        f.processingStamp = stamp;
        processFile(file);
    }

    public void syncDirectory(final long stamp, Path file) {
        if(log.isLoggable(Level.FINE))
            log.fine("Syncing directory " + file);
        try {
            Files.walkFileTree(file, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if(Thread.currentThread().isInterrupted())
                        return FileVisitResult.SKIP_SIBLINGS;
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
        if(Thread.currentThread().isInterrupted())
            return;
        index.removeNonexistent(file);
    }

    public void removeFile(long stamp, Path file) {
        index.removeFile(file);
    }

    public void removeDirectory(long stamp, Path file) {
        index.removeDirectory(file);
    }

    private void processFile(Path file) {
        if(log.isLoggable(Level.FINE))
            log.fine("Processing file " + file);
        indexManager.processFile(file);
    }

    public void fileFinished(long stamp, Path file, boolean b) {
        String sfile = file.toString();
        if(log.isLoggable(Level.FINE))
            log.fine("File finished " + sfile + " with result " + b);
        IndexedFile f = index.getFile(file);
        if(f == null) { //file removed
            log.warning("Finished processing removed file " + sfile);
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
            f.state = (b ? IndexedFile.VALID : IndexedFile.INVALID); //TODO: do we need to retry on invalid?
        }
    }

    public void removeWords(long stamp, Path file) {
        index.removeWords(file);
    }

    public void addWord(long stamp, Path file, String word) {
        index.addWord(file, word);
    }


    public List<Map.Entry<String, Integer>> getFiles() {
        return index.getFileNames();
    }

    public Collection<FileEntry> search(String word) {
        return index.search(word);
    }

    private final IndexStorage index;
    private final IIndexManager indexManager;
    private static final Logger log = Logger.getLogger(IndexProcessor.class.getName());
}
