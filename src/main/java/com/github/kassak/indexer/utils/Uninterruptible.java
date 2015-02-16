package com.github.kassak.indexer.utils;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class Uninterruptible {
    /**
        Run interruptible task which should be completed

        @param foo interruptible task
        @param maxTries number of tries before give up
        @return true if task was executed, false if gave up
    */
    public static boolean performUninterruptibly(@NotNull InterruptibleCallable foo, int maxTries) {
        boolean wasInterrupted = false;
        boolean finished = false;
        for(int i = 0; ; ++i) {
            try {
                foo.call();
                finished = true;
                break;
            }
            catch (InterruptedException e) {
                log.fine("Interrupted on uninterruptible op. Retry");
                wasInterrupted = true;
            }
            if(i >= maxTries) {
                log.warning("Too many tries. bye");
                break;
            }

        }
        if(wasInterrupted)
            Thread.currentThread().interrupt();
        return finished;
    }

    private static final Logger log = Logger.getLogger(Uninterruptible.class.getName());
}
