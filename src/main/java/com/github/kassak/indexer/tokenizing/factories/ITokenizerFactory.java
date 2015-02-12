package com.github.kassak.indexer.tokenizing.factories;

import com.github.kassak.indexer.tokenizing.ITokenizer;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;

public interface ITokenizerFactory {
    public ITokenizer create(@NotNull Reader r);
}
