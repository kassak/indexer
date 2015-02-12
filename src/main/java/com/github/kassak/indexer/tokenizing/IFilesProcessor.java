package com.github.kassak.indexer.tokenizing;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public interface IFilesProcessor {
    public boolean processFile(@NotNull Path f);
}
