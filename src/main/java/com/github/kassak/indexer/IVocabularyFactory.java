package com.github.kassak.indexer;

public interface IVocabularyFactory {
    public <E> IRWVocabulary<E> create();
}
