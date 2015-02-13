package com.github.kassak.indexer.tokenizing.factories;

import com.github.kassak.indexer.tokenizing.ITokenizer;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;

/**
    Interface for tokenizer's factory
*/
public interface ITokenizerFactory {
    /**
        Create new tokenizer for stream

        @param r stream to be tokenized
        @return tokenizer for supplied stream
    */
    public ITokenizer create(@NotNull Reader r);
}
