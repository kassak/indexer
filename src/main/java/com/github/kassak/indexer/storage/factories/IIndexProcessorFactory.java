package com.github.kassak.indexer.storage.factories;

import com.github.kassak.indexer.IIndexManager;
import com.github.kassak.indexer.storage.IIndexProcessor;
import org.jetbrains.annotations.NotNull;

public interface IIndexProcessorFactory {
    public @NotNull
    IIndexProcessor create(@NotNull IIndexManager im);
}
