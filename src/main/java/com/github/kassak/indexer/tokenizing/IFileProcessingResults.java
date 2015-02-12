package com.github.kassak.indexer.tokenizing;

import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.nio.file.Path;

public interface IFileProcessingResults {
    public void addWordToIndex(@NotNull Path file, @NotNull String word) throws InterruptedException;
    public void removeFromIndex(@NotNull Path file) throws InterruptedException;
    public void submitFinishedProcessing(@NotNull Path file, long stamp, boolean valid) throws InterruptedException;

    public @NotNull ITokenizer newTokenizer(@NotNull Path file) throws FileNotFoundException;
}
