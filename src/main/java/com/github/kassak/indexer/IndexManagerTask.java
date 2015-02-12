package com.github.kassak.indexer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

class IndexManagerTask implements Comparable<IndexManagerTask> {
    static public final int SYNC_FILE = 0;
    static public final int SYNC_DIR = 1;
    static public final int DEL_FILE = 2;
    static public final int DEL_DIR = 3;
    static public final int ADD_WORD = 4;
    static public final int REMOVE_WORDS = 5;
    static public final int FILE_FINISHED_OK = 6;
    static public final int FILE_FINISHED_FAIL = 7;

    public IndexManagerTask(int task, @NotNull Path path, @Nullable String word) {
        this.task = task;
        this.path = path;
        this.word = word;
        stamp = System.currentTimeMillis();
        seqNum = seq.incrementAndGet();
    }

    public IndexManagerTask(int task, @NotNull Path path, long stamp, @Nullable String word) {
        this.task = task;
        this.path = path;
        this.word = word;
        this.stamp = stamp;
        seqNum = seq.incrementAndGet();
    }

    @Override
    public int compareTo(@NotNull IndexManagerTask o) {
        if(seqNum == o.seqNum)
            return 0;
        int tg1 = taskGroup(task);
        int tg2 = taskGroup(o.task);
        if(tg1 == tg2)
            return seqNum < o.seqNum ? -1 : 1;
        return tg1 < tg2 ? -1 : 1;
    }

    private static int taskGroup(int taskId) {
        if(taskId == FILE_FINISHED_FAIL || taskId == FILE_FINISHED_OK)
            return 2;
        if(taskId == ADD_WORD || taskId == REMOVE_WORDS)
            return 1;
        return 0;
    }

    public final int task;
    public final Path path;
    public final String word;
    public final long stamp;
    private final long seqNum;
    private static final AtomicLong seq = new AtomicLong(0);
}
