package com.github.kassak.indexer.storage;

import java.util.Set;

class IndexedWord {
    IndexedWord(String word) {
        this.word = word;
    }

    public String getWord() {
        return word;
    }

    private final String word;
}
