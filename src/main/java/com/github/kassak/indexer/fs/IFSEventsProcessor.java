package com.github.kassak.indexer.fs;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
    Interface for processor of file system events
*/
public interface IFSEventsProcessor {
    /**
        File removal notification

        @param file file that was removed
        @throws InterruptedException
    */
    void onFileRemoved(@NotNull Path file) throws InterruptedException;

    /**
        File change notification

        @param file file that was changed
        @throws InterruptedException
    */
    void onFileChanged(@NotNull Path file) throws InterruptedException;

    /**
        Directory removal notification

        @param file directory that was removed
        @throws InterruptedException
    */
    void onDirectoryRemoved(@NotNull Path file) throws InterruptedException;

    /**
        Directory change notification

        @param file directory that was removed
        @throws InterruptedException
    */
    void onDirectoryChanged(@NotNull Path file) throws InterruptedException;
}
