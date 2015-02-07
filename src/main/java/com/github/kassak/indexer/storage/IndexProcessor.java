package com.github.kassak.indexer.storage;

import com.github.kassak.indexer.*;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IndexProcessor {

    public IndexProcessor(IIndexManager im) {
        indexManager = im;
        index = new IndexStorage();
    }
    public void syncFile(long stamp, Path file) {
        if(log.isLoggable(Level.FINE))
            log.fine("syncing file " + file);
        IndexedFile f = index.getOrAddFile(file, stamp);
        assert(f.getStamp() <= stamp); //this is guaranteed
        f.setStamp(stamp);
        if(f.getState() == IndexedFile.PROCESSING) //already processing
            return;
        f.setState(IndexedFile.PROCESSING);
        f.setProcessingStamp(stamp);
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
        index.removeNonexistent(file);
    }

    public void removeFile(long stamp, Path file) {
        index.removeFile(file);
    }

    public void removeDirectory(long stamp, Path file) {
        index.removeDirectory(file);
    }

    private void processFile(Path file) {
        try {
            indexManager.processFile(file);
        } catch (InterruptedException e) {
            log.warning("Interrupted while queueing of processing of " + file);
            Thread.currentThread().interrupt();
        }
    }

    public void fileFinished(long stamp, Path file, boolean b) {
        String sfile = file.toString();
        if(log.isLoggable(Level.FINE))
            log.fine("File finished " + sfile + " with result " + b);
        IndexedFile f = index.getFile(file);
        if(f == null) { //file removed
            if(log.isLoggable(Level.FINE))
                log.fine("Finished processing removed file " + sfile);
            return;
        }
        assert(f.getState() == IndexedFile.PROCESSING && f.getProcessingStamp() <= stamp); //NOTE: how couldn't it be?
        if(f.getStamp() > stamp) { //modified while processing
            if(log.isLoggable(Level.FINE))
                log.fine("File was modified while processing. Doing it again " + sfile);
            f.setProcessingStamp(f.getStamp());
            f.setState(IndexedFile.PROCESSING);
            processFile(file);
        }
        else {
            f.setStamp(stamp);
            f.setProcessingStamp(stamp);
            f.setState(b ? IndexedFile.VALID : IndexedFile.INVALID); //TODO: do we need to retry on invalid?
        }
    }

    public void removeWords(long stamp, Path file) {
        index.removeWords(file);
    }

    public void addWord(long stamp, Path file, String word) {
        index.addWord(file, word);
    }


    public Collection<String> getFiles() {
        return index.getFileNames();
    }

    public Collection<FileEntry> search(String word) {
        return index.search(word);
    }

    private final IndexStorage index;
    //private final SortedMap<String, IndexedFile> files;
    private final IIndexManager indexManager;
    //private final IVocabulary<IndexedWord> index;
    private static final Logger log = Logger.getLogger(IndexProcessor.class.getName());
}
