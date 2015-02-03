package com.github.kassak.indexer;

import java.util.concurrent.ConcurrentHashMap;

/**
 * This is temporary vocabulary class based on ConcurrentHashMap
 * Needs to be replaced by some trie
 */
public class CHMVocabulary<E> implements IRWVocabulary<E> {
    @Override
    public void put(String s, E data) {
        backend.put(s, data);
    }

    @Override
    public void erase(String s) {
        backend.remove(s);
    }

    @Override
    public void eraseAll(IPredicate<E> pred) {
        //TODO:
    }

    @Override
    public E get(String s) {
        return backend.get(s);
    }

    private ConcurrentHashMap<String, E> backend;
}
