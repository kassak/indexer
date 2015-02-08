package com.github.kassak.indexer.tokenizing;

import java.io.Reader;

public interface ITokenizerFactory {
    public ITokenizer create(Reader r);
}
