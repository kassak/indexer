package com.github.kassak.indexer.tokenizing;

import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.util.Scanner;

/**
    Tokenizer which splits by specified separator
*/
public class SeparatorTokenizer implements ITokenizer {
    /**
        @param r reader to tokenize
        @param delim pattern for words separator
    */
    public SeparatorTokenizer(@NotNull Reader r, @NotNull String delim) {
        scanner = new Scanner(r);
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
