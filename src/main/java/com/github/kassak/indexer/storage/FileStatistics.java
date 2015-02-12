package com.github.kassak.indexer.storage;

public class FileStatistics {
    public FileStatistics(String name, int state, long wordsNum) {
        this.name = name;
        this.state = state;
        this.wordsNum = wordsNum;
    }

    public final String name;
    public final int state;
    public final long wordsNum;
}
