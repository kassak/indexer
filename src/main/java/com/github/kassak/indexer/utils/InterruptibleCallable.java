package com.github.kassak.indexer.utils;

/**
    Represents function which can be interrupted
*/
public interface InterruptibleCallable {
    /**
        Run function
    */
    public void call() throws InterruptedException;
}
