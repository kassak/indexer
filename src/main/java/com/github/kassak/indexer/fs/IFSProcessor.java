package com.github.kassak.indexer.fs;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public interface IFSProcessor {
    void onFileRemoved(@NotNull Path file) throws InterruptedException;
    void onFileChanged(@NotNull Path file) throws InterruptedException;
    void onDirectoryRemoved(@NotNull Path file) throws InterruptedException;
    void onDirectoryChanged(@NotNull Path file) throws InterruptedException;
}
