package com.github.kassak.indexer.tokenizing;

import com.github.kassak.indexer.IIndexManager;
import com.github.kassak.indexer.utils.BlockingExecutor;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class FilesProcessor implements IFilesProcessor {
    public FilesProcessor(IIndexManager im, int threadsNum, int queueSize) {
        indexManager = im;
        this.threadsNum = threadsNum;
        this.queueSize = queueSize;
    }

    public void processFile(Path f) throws InterruptedException {
        if(!isRunning())
            throw new IllegalStateException("Service already not running");
        Runnable r = new FileProcessorUnit(indexManager, f);
        executor.execute(r);
    }

    @Override
    public void startService() throws Exception {
        if(isRunning())
            throw new IllegalStateException("Service already running");
        executor = new BlockingExecutor(threadsNum, queueSize);
    }

    @Override
    public void stopService() throws Exception {
        if(!isRunning())
            throw new IllegalStateException("Service already stopped");
        executor.shutdown();
    }

    @Override
    public boolean isRunning() {
        return executor != null && !executor.isShutdown();
    }

    @Override
    public boolean waitFinished(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    private BlockingExecutor executor;
    private final IIndexManager indexManager;
    private final int threadsNum, queueSize;
}
