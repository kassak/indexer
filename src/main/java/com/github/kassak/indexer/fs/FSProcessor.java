package com.github.kassak.indexer.fs;

import com.github.kassak.indexer.IIndexManager;
import org.jetbrains.annotations.NotNull;

import java.nio.file.*;

public class FSProcessor implements IFSProcessor {
    public FSProcessor(@NotNull IIndexManager im) {
        this.indexManager = im;
    }

    @Override
    public void onFileRemoved(@NotNull Path file) throws InterruptedException {
        indexManager.removeFile(file);
    }

    @Override
    public void onFileChanged(@NotNull Path file) throws InterruptedException {
        indexManager.syncFile(file);
    }

    @Override
    public void onDirectoryRemoved(@NotNull Path file) throws InterruptedException {
        indexManager.removeDirectory(file);
    }

    @Override
    public void onDirectoryChanged(@NotNull Path file) throws InterruptedException {
        indexManager.syncDirectory(file);
    }

    final private IIndexManager indexManager;
}
