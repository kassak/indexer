package com.github.kassak.indexer.utils;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
    Class for thread-based services
*/
public class ThreadService implements IService {
    public ThreadService(Runnable r) {
        thread = new Thread(r);
    }
    @Override
    public void startService() throws FailureException {
        if(isRunning())
            throw new IllegalStateException("Service already started");
        thread.start();
    }

    @Override
    public void stopService() {
        thread.interrupt();
    }

    @Override
    public boolean isRunning() {
        return thread.isAlive();
    }

    @Override
    public boolean waitFinished(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        thread.join(unit.toMillis(timeout));
        return !isRunning();
    }

    private final Thread thread;
}
