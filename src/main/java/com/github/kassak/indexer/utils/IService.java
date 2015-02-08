package com.github.kassak.indexer.utils;

import java.util.concurrent.TimeUnit;

public interface IService {
    public void startService();
    public void stopService();
    public boolean isRunning();
    public boolean waitFinished(long timeout, TimeUnit unit) throws InterruptedException;
}
