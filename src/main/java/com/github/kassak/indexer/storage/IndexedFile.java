package com.github.kassak.indexer.storage;

import java.nio.file.Path;
import java.util.Set;

class IndexedFile {
    static public final int VALID = 0;
    static public final int PROCESSING = 1;
    static public final int INVALID = 2;

    public IndexedFile(String path, int state, long stamp) {
        this.path = path;
        this.state = state;
        this.stamp = stamp;
        this.processingStamp = stamp;
    }

    public final String path;
    public volatile int state;
    public long stamp, processingStamp;
}
