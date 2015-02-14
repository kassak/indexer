package com.github.kassak.indexer.tokenizing.factories;

import com.github.kassak.indexer.tokenizing.ITokenizer;
import com.github.kassak.indexer.tokenizing.SeparatorTokenizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
    Factory for tokenizers which splits words on whitespaces
*/
public class WhitespaceTokenizerFactory implements ITokenizerFactory {
    @Override
    public @Nullable ITokenizer create(@NotNull Path file) throws IOException {
        if(!Files.exists(file))
            return null;
        return new SeparatorTokenizer(file, "\\s");
    }
}
