package com.github.kassak.indexer.utils;

import java.util.concurrent.TimeUnit;

public interface IService {
    public void startService() throws Exception;
    public void stopService() throws Exception;
    public boolean isRunning();
    public boolean waitFinished(long timeout, TimeUnit unit) throws InterruptedException;
}
