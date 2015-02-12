package com.github.kassak.indexer.tokenizing.factories;

import com.github.kassak.indexer.IIndexManager;
import com.github.kassak.indexer.tokenizing.FilesProcessor;
import com.github.kassak.indexer.tokenizing.IFilesProcessor;
import org.jetbrains.annotations.NotNull;

public class FilesProcessorFactory implements IFilesProcessorFactory {
    public FilesProcessorFactory(int threadsNum, int queueSize) {
        this.threadsNum = threadsNum;
        this.queueSize = queueSize;
    }
    @Override
    public @NotNull IFilesProcessor create(@NotNull IIndexManager im) {
        return new FilesProcessor(im, threadsNum, queueSize);
    }

    private final int threadsNum;
    private final int queueSize;
}
