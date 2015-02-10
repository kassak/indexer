package com.github.kassak.indexer.storage;

import org.jetbrains.annotations.NotNull;

class IndexedWord {
    IndexedWord(@NotNull String word) {
        this.word = word;
    }

    public final String word;
}
