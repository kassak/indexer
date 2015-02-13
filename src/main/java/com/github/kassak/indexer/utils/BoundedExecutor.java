package com.github.kassak.indexer.utils;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;

/**
    Executor service which can decline execution without throwing exception
*/
public class BoundedExecutor {
    /**
        @param threadsNum number of threads in pool
        @param queueSize size of queue
    */
    public BoundedExecutor(int threadsNum, int queueSize) {
        if(threadsNum > queueSize)
            queueSize = threadsNum;
        executor = new ThreadPoolExecutor(threadsNum, threadsNum, 5
                , TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(queueSize));
        semaphore = new Semaphore(queueSize);
    }

    /**
        Tries to queue task to be executed

        @param command task to execute
        @return false if queue is full true if queued
    */
    public boolean tryExecute(@NotNull final Runnable command) {
        if(isShutdown())
            throw new IllegalStateException("Already shut down");
        if(semaphore.tryAcquire()) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        command.run();
                    } finally {
                        semaphore.release();
                    }
                }
            });
            return true;
        } else {
            return false;
        }
    }

    /**
        Shut executor down
    */
    public void shutdown() {
        executor.shutdown();
    }

    /**
        Wait for executor termination

        @param timeout number of units to wait
        @param unit unit of time
        @return true if terminated before timeout
        @throws InterruptedException
    */
    public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }


    /**
       @return true if executor is shut down
    */
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    private final ExecutorService executor;
    private final Semaphore semaphore;
}
