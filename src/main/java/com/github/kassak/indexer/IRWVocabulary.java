package com.github.kassak.indexer;

public interface IRWVocabulary<E> extends IVocabulary<E> {
    public static interface IPredicate<D> {
        boolean check(D data);
    }

    public void put(String s, E data);
    public void erase(String s);
    public void eraseAll(IPredicate<E> pred);
}
