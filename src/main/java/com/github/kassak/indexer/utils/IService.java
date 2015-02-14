package com.github.kassak.indexer.utils;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
    Interface for start-stop services
*/
public interface IService {
    static class FailureException extends Exception {
        public FailureException() {
            super("Service start failure");
        }
    }
    /**
        Starts service

        @throws IService.FailureException in case of start failure
    */
    public void startService() throws FailureException;

    /**
        Stops service
    */
    public void stopService();

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
