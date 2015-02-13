package com.github.kassak.indexer.tokenizing.factories;

import com.github.kassak.indexer.tokenizing.FilesProcessorService;
import com.github.kassak.indexer.tokenizing.IFileProcessingResults;
import com.github.kassak.indexer.tokenizing.IFilesProcessorService;
import org.jetbrains.annotations.NotNull;

/**
    Standard factory for files processor service
*/
public class FilesProcessorServiceFactory implements IFilesProcessorServiceFactory {
    public FilesProcessorServiceFactory(int threadsNum, int queueSize) {
        this.threadsNum = threadsNum;
        this.queueSize = queueSize;
    }
    @Override
    public @NotNull
    IFilesProcessorService create(@NotNull IFileProcessingResults im) {
        return new FilesProcessorService(im, threadsNum, queueSize);
    }

    private final int threadsNum;
    private final int queueSize;
}
