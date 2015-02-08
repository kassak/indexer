package com.github.kassak.indexer.tokenizing;

import com.github.kassak.indexer.IIndexManager;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FileProcessorUnit implements Runnable {
    public FileProcessorUnit(IIndexManager im, Path f) {
        indexManager = im;
        file = f;
    }

    @Override
    public void run() {
        if(log.isLoggable(Level.FINE))
            log.fine("Start processing " + file);
        boolean finished = false;
        final long stamp = System.currentTimeMillis();
        try {
            indexManager.removeFromIndex(file);
            if(!Files.exists(file))
                return;
            try (ITokenizer tok = indexManager.newTokenizer(file)) {
                while (!Thread.currentThread().isInterrupted()) {
                    if(!tok.hasNext()) {
                        finished = true;
                        break;
                    }
                    String word = tok.next();
                    submitWord(word);
                }
            } catch (FileNotFoundException e) {
                log.log(Level.WARNING, "File not found " + file, e);
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to close " + file, e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            try {
                submitFinished(stamp, finished);
            } catch (InterruptedException e) {
                log.warning("Interrupted while submitting result " + file);
            }
        }
    }

    private void submitFinished(long stamp, boolean finished) throws InterruptedException {
        if(log.isLoggable(Level.FINE))
            log.fine("Processing finished " + file + " with " + finished);
        indexManager.submitFinishedProcessing(file, stamp, finished);
    }

    private void submitWord(String word) throws InterruptedException {
        if(log.isLoggable(Level.FINEST))
            log.finest("Submitting word " + file + " : " + word);
        indexManager.addWordToIndex(file, word);
    }

    private final IIndexManager indexManager;
    private final Path file;
    private static final Logger log = Logger.getLogger(FileProcessorUnit.class.getName());
}