package com.github.kassak.indexer.tokenizing;

import java.io.Reader;
import java.util.Scanner;

public class SeparatorTokenizer implements ITokenizer {
    SeparatorTokenizer(Reader r, String delim) {
        scanner = new Scanner(r);
        scanner.useDelimiter(delim);
    }
    @Override
    public boolean hasNext() {
        return scanner.hasNext();
    }

    @Override
    public String next() {
        return scanner.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("don't do it");
    }

    @Override
    public void close() throws Exception {
        scanner.close();
    }

    private Scanner scanner;
}
