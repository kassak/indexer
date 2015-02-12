package com.github.kassak.indexer.storage.factories;

import com.github.kassak.indexer.storage.IIndexProcessor;
import com.github.kassak.indexer.tokenizing.IFilesProcessor;
import org.jetbrains.annotations.NotNull;

public interface IIndexProcessorFactory {
    public @NotNull IIndexProcessor create(@NotNull IFilesProcessor im);
}
