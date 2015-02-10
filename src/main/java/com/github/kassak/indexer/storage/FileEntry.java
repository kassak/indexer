package com.github.kassak.indexer.storage;

import org.jetbrains.annotations.NotNull;

public class FileEntry {
    public FileEntry(@NotNull String path, boolean valid) {
        this.path = path;
        this.valid = valid;
    }

    public @NotNull String getPath() {
        return path;
    }

    public boolean isValid() {
        return valid;
    }

    private final String path;
    private final boolean valid;
}
