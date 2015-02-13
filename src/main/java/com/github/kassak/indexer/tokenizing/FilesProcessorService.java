package com.github.kassak.indexer.tokenizing;

import com.github.kassak.indexer.utils.BoundedExecutor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
    Service for queueing files and processing them in multiple threads.
    When queue is full further processing requests are declined. 
*/
public class FilesProcessorService implements IFilesProcessorService {
    /**
        @param im reciever of processing results
        @param threadsNum number of processor threads
        @param queueSize size of files queue
    */
    public FilesProcessorService(@NotNull IFileProcessingResults im, int threadsNum, int queueSize) {
        indexManager = im;
        this.threadsNum = threadsNum;
        this.queueSize = queueSize;
    }

    /**
        Enqueues file for further processing.
        @return true if file succesfully enqueued false if queue is full
    */
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
    private final IFileProcessingResults indexManager;
    private final int threadsNum, queueSize;
}
