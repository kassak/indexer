package com.github.kassak.indexer.storage;

import org.jetbrains.annotations.NotNull;

class IndexedFile {
    public IndexedFile(@NotNull String path, long stamp) {
        this.path = path;
        this.state = States.INVALID;
        this.stamp = stamp;
        this.processingStamp = stamp;
    }

    public final String path;
    public volatile int state;
    public long stamp, processingStamp;
}
