package com.github.kassak.indexer.tokenizing.factories;

import com.github.kassak.indexer.tokenizing.ITokenizer;
import com.github.kassak.indexer.tokenizing.SeparatorTokenizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
    Factory for tokenizer which splits words on non-alphanumeric characters
*/
public class AlphanumTokenizerFactory implements ITokenizerFactory {
    @Override
    public @Nullable ITokenizer create(@NotNull Path file) throws IOException {
        if(!Files.exists(file))
            return null;
        return new SeparatorTokenizer(file, "[^\\p{IsAlphabetic}\\p{IsDigit}]");
    }
}
