package com.github.kassak.indexer.tokenizing.factories;

import com.github.kassak.indexer.tokenizing.IFileProcessingResults;
import com.github.kassak.indexer.tokenizing.IFilesProcessorService;
import org.jetbrains.annotations.NotNull;

public interface IFilesProcessorServiceFactory {
    public @NotNull
    IFilesProcessorService create(@NotNull IFileProcessingResults im);
}
