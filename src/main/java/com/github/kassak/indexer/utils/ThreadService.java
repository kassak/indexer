package com.github.kassak.indexer.utils;

import java.util.concurrent.TimeUnit;

public abstract class ThreadService implements IService, Runnable {
    @Override
    public void startService() {
        if(thread != null && thread.isAlive())
            throw new IllegalStateException("Service already started");
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void stopService() {
        if(thread == null || !thread.isAlive())
            throw new IllegalStateException("Service already stopped");
        thread.interrupt();
    }

    @Override
    public boolean isRunning() {
        return thread != null && thread.isAlive();
    }

    @Override
    public boolean waitFinished(long timeout, TimeUnit unit) throws InterruptedException {
        thread.join(unit.toMillis(timeout));
        return !isRunning();
    }

    private Thread thread;
}
