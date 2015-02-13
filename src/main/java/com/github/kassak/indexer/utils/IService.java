package com.github.kassak.indexer.utils;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
    Interface for start-stop services
*/
public interface IService {
    /**
        Starts service

        @throws Exception in case of start failure
    */
    public void startService() throws Exception;

    /**
        Stops service

        @throws Exception in case of stop failure
    */
    public void stopService() throws Exception;

    /**
        Check if service is running

        @return true if service is running
    */
    public boolean isRunning();

    /**
        Wait for service to be finished

        @param timeout number of units to wait
        @param unit time unit to wait
        @return true if terminated while waiting
        @throws InterruptedException
    */
    public boolean waitFinished(long timeout, @NotNull TimeUnit unit) throws InterruptedException;
}
