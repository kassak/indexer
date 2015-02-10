package com.github.kassak.indexer.tokenizing;

import com.github.kassak.indexer.IIndexManager;
import com.github.kassak.indexer.utils.BoundedExecutor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class FilesProcessor implements IFilesProcessor {
    public FilesProcessor(@NotNull IIndexManager im, int threadsNum, int queueSize) {
        indexManager = im;
        this.threadsNum = threadsNum;
        this.queueSize = queueSize;
    }

    @Override
    public boolean processFile(@NotNull Path f) {
        if(!isRunning())
            throw new IllegalStateException("Service not running");
        Runnable r = new FileProcessorUnit(indexManager, f);
        return executor.tryExecute(r);
    }

    @Override
    public void startService() throws Exception {
        if(isRunning())
            throw new IllegalStateException("Service already running");
        executor = new BoundedExecutor(threadsNum, queueSize);
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
    public boolean waitFinished(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    private BoundedExecutor executor;
    private final IIndexManager indexManager;
    private final int threadsNum, queueSize;
}
