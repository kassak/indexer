package com.github.kassak.indexer.tokenizing;

import org.jetbrains.annotations.NotNull;

import java.io.Reader;

public interface ITokenizerFactory {
    public ITokenizer create(@NotNull Reader r);
}
