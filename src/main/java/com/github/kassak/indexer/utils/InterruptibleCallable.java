package com.github.kassak.indexer.utils;

public interface InterruptibleCallable {
    public void call() throws InterruptedException;
}
