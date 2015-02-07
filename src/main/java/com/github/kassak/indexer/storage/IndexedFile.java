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

    public String getPath() {
        return path;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getStamp() {
        return stamp;
    }

    public void setStamp(long stamp) {
        this.stamp = stamp;
    }

    public long getProcessingStamp() {
        return processingStamp;
    }

    public void setProcessingStamp(long processingStamp) {
        this.processingStamp = processingStamp;
    }

    private final String path;
    private int state;
    private long stamp, processingStamp;
}
