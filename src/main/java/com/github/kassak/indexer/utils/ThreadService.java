package com.github.kassak.indexer.utils;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
    Abstract base class for thread-based services
*/
public abstract class ThreadService implements IService, Runnable {
    @Override
    public void startService() throws Exception {
        if(thread != null && thread.isAlive())
            throw new IllegalStateException("Service already started");
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void stopService() throws Exception {
        if(thread == null || !thread.isAlive())
            return; //already stopped
        thread.interrupt();
    }

    @Override
    public boolean isRunning() {
        return thread != null && thread.isAlive();
    }

    @Override
    public boolean waitFinished(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
        thread.join(unit.toMillis(timeout));
        return !isRunning();
    }

    private Thread thread;
}
