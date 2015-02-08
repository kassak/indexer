package com.github.kassak.indexer;

import java.nio.file.Path;

class IndexManagerTask {
    static public final int SYNC_FILE = 0;
    static public final int SYNC_DIR = 1;
    static public final int DEL_FILE = 2;
    static public final int DEL_DIR = 3;
    static public final int ADD_WORD = 4;
    static public final int REMOVE_WORDS = 5;
    static public final int FILE_FINISHED_OK = 6;
    static public final int FILE_FINISHED_FAIL = 7;

    public IndexManagerTask(int task, Path path, String word) {
        this.task = task;
        this.path = path;
        this.word = word;
        stamp = System.currentTimeMillis();
    }

    public IndexManagerTask(int task, Path path, long stamp, String word) {
        this.task = task;
        this.path = path;
        this.word = word;
        this.stamp = stamp;
    }

    public final int task;
    public final Path path;
    public final String word;
    public final long stamp;
}
