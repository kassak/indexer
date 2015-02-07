package com.github.kassak.indexer;

import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class FilesProcessor {
    public FilesProcessor(IIndexManager im, int threadsNum, int queueSize) {
        executor = new BlockingExecutor(threadsNum, queueSize);
        indexManager = im;
    }

    void processFile(Path f) throws InterruptedException {
        Runnable r = new FileProcessorUnit(indexManager, f);
        executor.execute(r);
    }

    void shutdown() {
        executor.shutdown();
    }

    void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        executor.awaitTermination(timeout, unit);
    }

    private final BlockingExecutor executor;
    private final IIndexManager indexManager;
}
