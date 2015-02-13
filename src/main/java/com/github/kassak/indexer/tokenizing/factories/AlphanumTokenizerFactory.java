package com.github.kassak.indexer.tokenizing.factories;

import com.github.kassak.indexer.tokenizing.ITokenizer;
import com.github.kassak.indexer.tokenizing.SeparatorTokenizer;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;

public class AlphanumTokenizerFactory implements ITokenizerFactory {
    @Override
    public @NotNull ITokenizer create(@NotNull Reader r) {
        return new SeparatorTokenizer(r, "[^\\p{IsAlphabetic}\\p{IsDigit}]");
    }
}
