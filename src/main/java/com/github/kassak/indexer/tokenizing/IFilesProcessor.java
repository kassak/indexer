package com.github.kassak.indexer.tokenizing;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
    Interface for file processor
*/
public interface IFilesProcessor {
    /**
        Start processing supplied file.

        @param f file to process
        @return true if file was accepted for processing        
    */
    public boolean processFile(@NotNull Path f);
}
