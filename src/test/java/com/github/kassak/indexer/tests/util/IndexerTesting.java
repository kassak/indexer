package com.github.kassak.indexer.tests.util;

import com.github.kassak.indexer.IIndexManagerService;
import com.github.kassak.indexer.fs.IFSWatcherService;

public class IndexerTesting {
    public static void waitIdle(IIndexManagerService im) throws InterruptedException {
        Thread.sleep(100);
        while(!im.isIdle())
            Thread.sleep(100);
    }
    public static void waitIdle(IFSWatcherService fs) throws InterruptedException {
        int count = 0;
        while(count < 2) {
            if(fs.isIdle())
                ++count;
            else
                count = 0;
            Thread.sleep(100);
        }
    }
}
