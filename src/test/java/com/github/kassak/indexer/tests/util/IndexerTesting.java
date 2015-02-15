package com.github.kassak.indexer.tests.util;

import com.github.kassak.indexer.IIndexManagerService;
import com.github.kassak.indexer.fs.FSWatcherService;

public class IndexerTesting {
    public static void waitIdle(IIndexManagerService im) throws InterruptedException {
        int count = 0;
        while(count < 5) {
            if(im.isIdle())
                ++count;
            else
                count = 0;
            Thread.sleep(100);
        }
    }
    public static void waitIdle(FSWatcherService fs) throws InterruptedException {
        final long WAIT_TIMEOUT = 5000;
        long start = System.currentTimeMillis();
        while(true) {
            long la = Math.max(start, fs.getLastActivity());
            long cur = System.currentTimeMillis();
            if(cur - la > WAIT_TIMEOUT)
                return;
            Thread.sleep(WAIT_TIMEOUT - (cur - la));
        }
    }
}
