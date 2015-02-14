package com.github.kassak.indexer.tokenizing.factories;

import com.github.kassak.indexer.tokenizing.ITokenizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

/**
    Interface for tokenizer's factory
*/
public interface ITokenizerFactory {
    /**
     * Create new tokenizer for stream
     *
     * @return tokenizer for supplied stream
     * @param file file to be tokenized or null if no file found
     */
    public @Nullable ITokenizer create(@NotNull Path file) throws IOException;
}
