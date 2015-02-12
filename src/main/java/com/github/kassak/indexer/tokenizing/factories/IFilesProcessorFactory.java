package com.github.kassak.indexer.tokenizing.factories;

import com.github.kassak.indexer.IIndexManager;
import com.github.kassak.indexer.tokenizing.IFilesProcessor;
import org.jetbrains.annotations.NotNull;

public interface IFilesProcessorFactory {
    public @NotNull IFilesProcessor create(@NotNull IIndexManager im);
}
