package com.github.kassak.indexer.tokenizing.factories;

import com.github.kassak.indexer.tokenizing.IFileProcessingResults;
import com.github.kassak.indexer.tokenizing.IFilesProcessorService;
import org.jetbrains.annotations.NotNull;

/**
    Interface for files processor factory.
    Needed for testing purposes.
*/
public interface IFilesProcessorServiceFactory {
    /**
        Create new files processor service with specified results sink.

        @param im receiver of processing result
        @return files processor service
    */
    public @NotNull IFilesProcessorService create(@NotNull IFileProcessingResults im);
}
