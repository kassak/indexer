package com.github.kassak.indexer.tokenizing;

import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.Scanner;

/**
    Tokenizer which splits by specified separator
*/
public class SeparatorTokenizer implements ITokenizer {
    /**
        @param path file to tokenize
        @param delim pattern for words separator
    */
    public SeparatorTokenizer(@NotNull Path path, @NotNull String delim) throws FileNotFoundException {
        scanner = new Scanner(new FileReader(path.toFile()));
        scanner.useDelimiter(delim);
        advance();
    }

    private void advance() {
        do {
            if(scanner.hasNext()) {
                nextToken = scanner.next();
            }
            else {
                nextToken = null;
                break;
            }
        } while(nextToken.isEmpty());
    }
    @Override
    public boolean hasNext() {
        return nextToken != null;
    }

    @Override
    public String next() {
        String res = nextToken;
        advance();
        return res;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("don't do it");
    }

    @Override
    public void close() throws Exception {
        scanner.close();
    }

    private final Scanner scanner;
    private String nextToken;
}
