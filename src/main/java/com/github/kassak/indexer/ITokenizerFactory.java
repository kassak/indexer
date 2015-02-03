package com.github.kassak.indexer;

import java.io.Reader;

public interface ITokenizerFactory {
    public ITokenizer create(Reader r);
}
