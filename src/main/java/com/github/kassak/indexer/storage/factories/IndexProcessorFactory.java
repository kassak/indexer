package com.github.kassak.indexer.storage.factories;

import com.github.kassak.indexer.IIndexManager;
import com.github.kassak.indexer.storage.IIndexProcessor;
import com.github.kassak.indexer.storage.IndexProcessor;
import org.jetbrains.annotations.NotNull;

public class IndexProcessorFactory implements IIndexProcessorFactory{
    @Override
    public @NotNull IIndexProcessor create(@NotNull IIndexManager im) {
        return new IndexProcessor(im);
    }
}
