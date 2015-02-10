package com.github.kassak.indexer.tokenizing;

import org.jetbrains.annotations.NotNull;

import java.io.Reader;

public class WhitespaceTokenizerFactory implements ITokenizerFactory {
    @NotNull
    @Override
    public ITokenizer create(@NotNull Reader r) {
        return new SeparatorTokenizer(r, "\\s");
    }
}
