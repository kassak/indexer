package com.github.kassak.indexer;

import java.util.concurrent.*;

public class BlockingExecutor {
    BlockingExecutor(int threadsNum, int queueSize) {
        if(threadsNum > queueSize)
            queueSize = threadsNum;
        executor = new ThreadPoolExecutor(threadsNum, threadsNum, 5
                , TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(queueSize));
        semaphore = new Semaphore(queueSize);
    }

    public void execute(final Runnable command) throws InterruptedException {
        semaphore.acquire();
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
    }

    public void shutdown() {
        executor.shutdown();
    }

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }

    private final ExecutorService executor;
    private final Semaphore semaphore;
}
