package com.github.kassak.indexer.tokenizing;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
    Class that does the job on file processing
*/
class FileProcessorUnit implements Runnable {
    /**
        @param im receiver of processing results
        @param f file to process
    */
    public FileProcessorUnit(@NotNull IFileProcessingResults im, @NotNull Path f) {
        indexManager = im;
        file = f;
    }

    @Override
    public void run() {
        if(log.isLoggable(Level.FINER))
            log.finer("Start processing " + file);
        boolean finished = false;
        final long stamp = System.currentTimeMillis();
        try {
            indexManager.removeFromIndex(file);
            try (ITokenizer tok = indexManager.newTokenizer(file)) {
                if(tok == null)
                    return;
                while (!Thread.currentThread().isInterrupted()) {
                    if(!tok.hasNext()) {
                        finished = true;
                        break;
                    }
                    String word = tok.next();
                    submitWord(word);
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Failed to close " + file, e);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            try {
                submitFinished(stamp, finished);
            } catch (InterruptedException e) {
                log.fine("Interrupted while submitting result " + file);
            }
        }
    }

    private void submitFinished(long stamp, boolean finished) throws InterruptedException {
        if(log.isLoggable(Level.FINER))
            log.finer("Processing finished " + file + " with " + finished);
        indexManager.submitFinishedProcessing(file, stamp, finished);
    }

    private void submitWord(@NotNull String word) throws InterruptedException {
        if(log.isLoggable(Level.FINEST))
            log.finest("Submitting word " + file + " : " + word);
        indexManager.addWordToIndex(file, word);
    }

    private final IFileProcessingResults indexManager;
    private final Path file;
    private static final Logger log = Logger.getLogger(FileProcessorUnit.class.getName());
}
