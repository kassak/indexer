package com.github.kassak.indexer;

import java.io.Reader;

public class WhitespaceTokenizerFactory implements ITokenizerFactory {
    @Override
    public ITokenizer create(Reader r) {
        return new SeparatorTokenizer(r, "\\s");
    }
}
