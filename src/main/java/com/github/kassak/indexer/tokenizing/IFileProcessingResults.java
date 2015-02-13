package com.github.kassak.indexer.tokenizing;

import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.nio.file.Path;

/**
    Interface for interaction of file processor with index.
*/
public interface IFileProcessingResults {
    /**
        Submit word to index

        @param file source of word
        @param word word to submit
        @throws InterruptedException
    */
    public void addWordToIndex(@NotNull Path file, @NotNull String word) throws InterruptedException;

    /**
        Remove file from index

        @param file file to be removed
        @throws InterruptedException
    */
    public void removeFromIndex(@NotNull Path file) throws InterruptedException;

    /**
        Submit file processing result

        @param file processed file
        @param stamp time when processing have been started
        @param valid true if parsed successfully
        @throws InterruptedException
    */
    public void submitFinishedProcessing(@NotNull Path file, long stamp, boolean valid) throws InterruptedException;

    /**
        Create tokenizer for file

        @param file file to be tokenized
        @throws FileNotFoundException if no such file found
    */
    public @NotNull ITokenizer newTokenizer(@NotNull Path file) throws FileNotFoundException;
}
