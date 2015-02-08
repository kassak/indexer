package com.github.kassak.indexer.utils;

import java.util.logging.Logger;

public class Uninterruptible {
    public static boolean performUninterruptibly(InterruptibleCallable foo, int maxTries) {
        boolean wasInterrupted = false;
        boolean finished = false;
        for(int i = 0; ; ++i) {
            try {
                foo.call();
                finished = true;
                break;
            }
            catch (InterruptedException e) {
                log.warning("Interrupted on uninterruptible op. Retry");
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
