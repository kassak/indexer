package com.github.kassak.indexer.storage;

import org.jetbrains.annotations.NotNull;

class IndexedFile {
    static public final int VALID = 0;
    static public final int PROCESSING = 1;
    static public final int INVALID = 2;

    public IndexedFile(@NotNull String path, long stamp) {
        this.path = path;
        this.state = INVALID;
        this.stamp = stamp;
        this.processingStamp = stamp;
    }

    public final String path;
    public volatile int state;
    public long stamp, processingStamp;
}
