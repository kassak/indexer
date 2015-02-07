package com.github.kassak.indexer.storage;

public class FileEntry {
    public FileEntry(String path, boolean valid) {
        this.path = path;
        this.valid = valid;
    }

    public String getPath() {
        return path;
    }

    public boolean isValid() {
        return valid;
    }

    private final String path;
    private final boolean valid;
}
