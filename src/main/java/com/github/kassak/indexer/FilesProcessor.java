package com.github.kassak.indexer;

import com.github.kassak.indexer.utils.BlockingExecutor;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class FilesProcessor {
    public FilesProcessor(IIndexManager im, int threadsNum, int queueSize) {
        executor = new BlockingExecutor(threadsNum, queueSize);
        indexManager = im;
    }

    public void processFile(Path f) throws InterruptedException {
        Runnable r = new FileProcessorUnit(indexManager, f);
        executor.execute(r);
    }

    public void shutdown() {
        executor.shutdown();
    }

    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        executor.awaitTermination(timeout, unit);
    }

    private final BlockingExecutor executor;
    private final IIndexManager indexManager;
}
