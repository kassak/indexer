package com.github.kassak.indexer;

public class CHMVocabularyFactory implements IVocabularyFactory {
    @Override
    public <E> IRWVocabulary<E> create() {
        return new CHMVocabulary<E>();
    }
}
