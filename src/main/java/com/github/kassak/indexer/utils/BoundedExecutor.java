package com.github.kassak.indexer.utils;

import java.util.concurrent.*;

public class BoundedExecutor {
    public BoundedExecutor(int threadsNum, int queueSize) {
        if(threadsNum > queueSize)
            queueSize = threadsNum;
        executor = new ThreadPoolExecutor(threadsNum, threadsNum, 5
                , TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(queueSize));
        semaphore = new Semaphore(queueSize);
    }

    public boolean tryExecute(final Runnable command) {
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

    public void shutdown() {
        executor.shutdown();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    public boolean isShutdown() {
        return executor.isShutdown();
    }

    private final ExecutorService executor;
    private final Semaphore semaphore;
}
